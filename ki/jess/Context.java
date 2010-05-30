/** **********************************************************************
 * An execution context for Funcalls. To be used as a base class for Defrule,
 * Deffunction, etc.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class Context 
{
  Vector m_actions;
  Vector m_bindings;
  Rete m_engine;
  private Stack m_states;

  boolean m_return = false;
  Value m_retval;

  Context(Rete engine) 
  {
    m_actions = new Vector();
    m_bindings = new Vector();
    m_engine = engine;
    m_states = new Stack();
  }
  
  final boolean returning() 
  {
    return m_return;
  }

  final Value setReturnValue(Value val) 
  {
    m_return = true;
    m_retval = val;
    return val;
  }

  final Value getReturnValue() 
  {
    return m_retval;
  }

  final void clearReturnValue() 
  {
    m_return = false;
    m_retval = null;
  }

  public final Rete engine() 
  {
    return m_engine; 
  }

  void push() 
  {
    ContextState cs = new ContextState();
    cs.m_return = m_return;
    cs.m_retval = m_retval;
    cs.m_bindings = new Vector();
    for (int i=0; i<m_bindings.size(); i++) 
      cs.m_bindings.addElement(((Binding) m_bindings.elementAt(i)).clone());
    m_states.push(cs);
  }
  
  void pop() 
  {
    try 
      {
        ContextState cs = (ContextState) m_states.pop();
        m_return = cs.m_return;
        m_retval = cs.m_retval;
        m_bindings = cs.m_bindings;
      }
    catch (java.util.EmptyStackException ese) 
      {
        // tough break.
      }
  }

  /**
    Find a variable table entry
    */
  
  Binding findBinding(int name) 
  {
    for (int i=0; i< m_bindings.size(); i++) 
      {
        Binding b = (Binding) m_bindings.elementAt(i);
        if (b.m_name == name)
          return b;
      }
    return m_engine.globalContext().findGlobalBinding(name);
  }
  
  /**
    Add a Funcall to this context
    */

  final void addAction(ValueVector funcall) 
  {
    m_actions.addElement(funcall);
  }


  /**
    Make note of a variable binding during parsing,
    so it can be used at runtime; the fact and slot indexes are for the use
    of the subclasses. Defrules use factIndex and slotIndex to indicate where in
    a fact to get data from; Deffunctions just use factIndex to indicate which
    element of the argument vector to pull their information from.
    */

  final Binding addBinding(int name, int factIndex, int slotIndex,
                           int subIndex) 
  {
    Binding b = new Binding(name, factIndex, slotIndex, subIndex);
    m_bindings.addElement(b);
    return b;
  }

  /**
    Set a (possibly new!) variable to some type and value
    */

  final Binding setVariable(int name, Value value) 
  {
    Binding b = findBinding(name);
    if (b == null) 
      b = addBinding(name, RU.LOCAL, RU.LOCAL, -1);
    b.m_val = value;

    return b;
  }

  /**
    Recursive function which expands an action ValueVector by doing
    variable substitutions.
    @returns A copy of the action, expanded.
    */

  Funcall expandAction(Funcall original)
       throws ReteException 
  {

    boolean did_bind = false;

    // Make a local copy 
    Funcall action = (Funcall) original.clone();
    String functor = action.get(0).stringValue();            
    // do variable replacements, using current values;
    // note that these can change during a rule execution!
    for (int j=1; j<action.size(); j++) 
      {

        // -*- sigh -*-
        // For very few forms, a variable is treated as an lvalue and
        // therefore must not be substituted here. For now, only
        // bind and foreach are treated this way: Just leave the first arg
        // alone. Note that the actual bind is still executed in Funcall.
      
        Value current = action.get(j);      
        if ((functor.equals("bind") || functor.equals("foreach")) && j == 1) 
          {
            Binding b = findBinding(current.variableValue());
            if (b == null) 
              // This had better be an lvalue...
              b = setVariable(current.variableValue(), null);
            did_bind = true;
            continue;
          }

        if ((functor.equals("if") && j > 1) || functor.equals("while")
            || functor.equals("and") || functor.equals("or"))
          break;

        if (functor.equals("foreach") && j > 2) 
          break;

        action.set(expandValue(current), j);
      }
    return action;
  }
  
  public Value expandValue(Value current) throws ReteException 
  {

    switch (current.type()) 
      {
      case RU.VARIABLE:
      case RU.MULTIVARIABLE:
        {
          Binding b = findBinding(current.variableValue());
          if (b == null) 
            {
              // This had better be an lvalue...
              b = setVariable(current.variableValue(), null);
            }
          else 
            {
              // plug in current values. 
              if (b.m_val != null)
                return b.m_val;
              else
                return Funcall.NIL();
            }
          break;
        } 
      case RU.FUNCALL:
        {
          Funcall subaction = current.funcallValue();
          return new Value(expandAction(subaction), RU.FUNCALL);
        }
      case RU.ORDERED_FACT:
      case RU.UNORDERED_FACT:
        {
          ValueVector fact = current.factValue();
          return new Value(expandFact(fact), current.type());
        }
      case RU.LIST:
        {
          ValueVector list = current.listValue();
          list = expandList(list);
          list = flattenList(list);
          return new Value(list, current.type());
        }
      }
    return current;
  }


  /**
    Do varsubs in facts
    */
  
  public ValueVector expandFact(ValueVector original) throws ReteException
  {
    ValueVector fact = (ValueVector) original.clone();

    for (int i=RU.FIRST_SLOT; i<fact.size(); i++) 
      {
        switch (fact.get(i).type()) 
          {
          case RU.VARIABLE:
          case RU.MULTIVARIABLE:
            {
              Binding b = findBinding(fact.get(i).variableValue());
              fact.set(b.m_val, i);
              break;
            }
          case RU.FUNCALL:
            {
              Funcall fc = expandAction(fact.get(i).funcallValue());
              Value v = Funcall.execute(fc, this, null, null);
              fact.set(v, i);
              break;
            }
          case RU.LIST:
            {
              ValueVector list = fact.get(i).listValue();
              list = expandList(list);
              list = flattenList(list);
              fact.set(new Value(list, RU.LIST), i);
              break;
            }
          }
      }
    return fact;
  }

  /**
    Do varsubs in lists
    */

  ValueVector expandList(ValueVector original) throws ReteException 
  {

    ValueVector list = (ValueVector) original.clone();

    for (int i=0; i<list.size(); i++) 
      {
        switch (list.get(i).type()) 
          {
          case RU.VARIABLE:
          case RU.MULTIVARIABLE:
            {
              Binding b = findBinding(list.get(i).variableValue());
              list.set(b.m_val, i);
              break;
            }
          case RU.FUNCALL:
            {
              Funcall fc = expandAction(list.get(i).funcallValue());
              Value v = Funcall.execute(fc, this, null, null);
              list.set(v, i);
              break;
            }
          case RU.LIST:
            {
              ValueVector vv = expandList(list.get(i).listValue());
              list.set(new Value(vv, RU.LIST), i);
              break;
            }
          }
      }
    
    return list;
  }
  
  // Replace a nested set of lists with a flat list.

  static final ValueVector flattenList(ValueVector vv) throws ReteException 
  {
    ValueVector flat = new ValueVector();
    doFlattenList(flat, vv);
    return flat;
  }

  // Recursively smoosh nested lists into one flat list. 

  private static void doFlattenList(ValueVector acc, ValueVector vv)
       throws ReteException 
  {
    for (int i=0; i< vv.size(); i++) 
      {
        Value current = vv.get(i);
        if (current.type() != RU.LIST) 
          {        
            acc.add(current);
          }
        else
          doFlattenList(acc, current.listValue());
      }
  }
}

/**
 * An execution context 'stack frame'
 */

class ContextState
{
  Vector m_bindings;
  boolean m_return;
  Value m_retval;
}
