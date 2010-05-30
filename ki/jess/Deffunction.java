/** **********************************************************************
 * Class used to represent Deffunctions.
 * A Deffunction has no ValueVector representation; it is always a
 * class object.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */
package jess;

import java.util.*;

/**

@author E.J. Friedman-Hill (C)1996
*/

public class Deffunction extends Context implements Userfunction
{
  
  int m_name;
  String m_docstring;
  private int m_nargs;

  /**
    Constructor.
    */

  public final int name() { return m_name; }
  public final String docstring() { return m_docstring; }

  Deffunction(String name, Rete engine)
  {
    super(engine);
    m_name = RU.putAtom(name);
  }

  /**
    Add a formal argument to this deffunction.
    */

  void addArgument(String name) 
  {
    addBinding(RU.putAtom(name), m_nargs + 1, 0, -1);
    ++m_nargs;
  }

  /**
    Add a simple value to this deffunction.
    */

  void addValue(Value val) 
  {
    m_actions.addElement(val);
  }

  /**
    Execute this deffunction. For each action (ValueVector form of a Funcall),
    do two things:
    1) Call ExpandAction to do variable substitution and
    subexpression expansion
    2) call Funcall.Execute on it.


    Vars is the ValueVector form of the funcall that we're executing in.
    We don't use the Context; we execute in our own context!
    */

  public Value call(ValueVector call, Context c) throws ReteException 
  {
    
    Value result = null;

    if (call.size() < (m_nargs + 1))
      throw new ReteException("Deffunction::Fire",
                              "Too few arguments to Deffunction",
                              RU.getAtom(m_name));

    push();
    clearReturnValue();

    // set up the variable table
    int size = m_bindings.size();
    for (int i=0; i<size; i++) 
      {
        Binding b = (Binding) m_bindings.elementAt(i);
        if (b.m_slotIndex == RU.LOCAL)
          // no default binding for locals
          continue;
        // all others variables come from arguments
        b.m_val = call.get(b.m_factIndex);
      }

    // OK, now run the function. For every action...
    size = m_actions.size();
    for (int i=0; i<size; i++) 
      {

        // do all substitutions on this action
        if (m_actions.elementAt(i) instanceof ValueVector) 
          {
            Funcall original = (Funcall) m_actions.elementAt(i);
            Funcall action = expandAction(original);
            result = Funcall.execute(action, this);
          }
        else 
          {
            // it's a value
            Value v = (Value) m_actions.elementAt(i);
            result = expandValue(v);
          }

        if (returning()) 
          {
            result = getReturnValue();
            clearReturnValue();
            break;
          }
      }
    pop();
    return result;
  }
  

  private void debugPrint(Vector facts) throws ReteException
  {
    m_engine.outStream().print("FIRE " + toString());
    for (int i=0; i<facts.size(); i++) 
      {
        ValueVector f = (ValueVector) facts.elementAt(i);
        m_engine.outStream().print(" f-" + f.get(RU.ID).factIDValue());
        if (i< facts.size() -1)
          m_engine.outStream().print(",");
      }
    m_engine.outStream().println();
  }
  
  /**
    Describe myself
    */

  public String toString() 
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Deffunction: ");
    sb.append(RU.getAtom(m_name));
    sb.append(" ");
    if (m_docstring != null)
      sb.append("\"" + m_docstring + "\"; ");
    sb.append("]");
    return sb.toString(); 
  }

}

