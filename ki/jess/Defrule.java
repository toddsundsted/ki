/** **********************************************************************
 * Class used to represent Defrules.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class Defrule extends Context
{
  
  int m_name;
  Vector m_patts;
  String m_docstring;
  private int m_id;
  int m_salience;
  private boolean m_frozen = false;
  private Funcall[] m_localActions;
  private Vector m_nodes = new Vector();

  public final int name() { return m_name; }
  public final String docstring() { return m_docstring; }

  /**
    Constructor.
    */

  Defrule(String name, Rete engine) 
  {
    super(engine);
    m_name = RU.putAtom(name);
    m_id = engine.nextRuleId();
    m_patts = new Vector();
  }

  /**
    The id is read-only
    */

  public int id() { return m_id; }

  /**
    Add a pattern to this Defrule
    */

  void addPattern(Pattern pattern) throws ReteException 
  {
    m_patts.addElement(pattern);
    pattern.compact();

    // Look for variables in the fact, and create bindings for new ones
    for (int i=0; i< pattern.m_tests.length; i++)
      {
        if (pattern.m_tests[i] == null)
          continue;
        for (int j=0; j< pattern.m_tests[i].length; j++) 
          {
            if (pattern.m_tests[i][j].m_slotValue.type() == RU.VARIABLE ||
                pattern.m_tests[i][j].m_slotValue.type() == RU.MULTIVARIABLE) 
              {
                if (findBinding(pattern.m_tests[i][j].m_slotValue.variableValue())
                    == null)
                  if (pattern.m_tests[i][j].m_test == Test1.EQ) 
                    {
                      addBinding(pattern.m_tests[i][j].m_slotValue.variableValue(),
                                 m_patts.size()-1,
                                 RU.FIRST_SLOT + i,
                                 pattern.m_tests[i][j].m_subIdx);
                    } else
                      throw new ReteException("Defrule::AddPattern",
                                              "First reference to variable negated",
                                              pattern.m_tests[i][j].m_slotValue.toString());
              }
          }
      }
  }

  /**
    Tell this rule to set the actions up for faster execution
    */

  void freeze() 
  {
    if (m_frozen) return;

    m_frozen = true;

    m_localActions = new Funcall[m_actions.size()];
    for (int i=0; i<m_localActions.length; i++)
      m_localActions[i] = (Funcall) m_actions.elementAt(i);

  }
  

  void ready(Token fact_input) throws ReteException 
  {

    ValueVector fact;
    // set up the variable table
    int size = m_bindings.size();
    for (int i=0; i<size; i++) 
      {
        Binding b = (Binding) m_bindings.elementAt(i);
        if (b.m_slotIndex == RU.LOCAL)
          // no default binding for locals
          continue;

        // all others variables need info from a fact
        // if this is a not CE, skip it;
        fact = fact_input.fact(b.m_factIndex);
        try 
          {
            if (b.m_slotIndex == RU.PATTERN) 
              {
                b.m_val = fact.get(RU.ID);
              } 
            else 
              {
                if (b.m_subIndex == -1)
                  {
                    b.m_val = fact.get(b.m_slotIndex);
                  }
                
                else 
                  {
                    ValueVector vv = fact.get(b.m_slotIndex).listValue();
                    b.m_val = vv.get(b.m_subIndex);
                  }
            
              }
          } catch (Throwable t) 
            {
              // bad binding. These can come from unused bindings in not CE's.
            }
      
      }
    return;
  }
  

  /**
    Do the RHS of this rule.For each action (ValueVector form of a Funcall),
    do two things:
    1) Call ExpandAction to do variable substitution and
    subexpression expansion
    2) call Funcall.Execute on it.

    Fact_input is the Vector of ValueVector facts we were fired with.
    */

  void fire(Token fact_input) throws ReteException 
  {
    if (m_engine.watchRules())
      debugPrint(fact_input);
    
    push();
    clearReturnValue();

    // Pull needed values out of facts into bindings table
    ready(fact_input);

    // OK, now run the rule. For every action...
    int size = m_localActions.length;
    for (int i=0; i<size; i++) 
      {

        // do all substitutions on this action
        Funcall original = m_localActions[i];
        Funcall action = expandAction(original);
        Funcall.execute(action, this);

        if (m_return) 
          {
            clearReturnValue();
            pop();
            return;
          }
      }
    pop();
  }
  

  void debugPrint(Token facts) throws ReteException 
  {
    java.io.PrintStream ps = m_engine.outStream();
    ps.print("FIRE " + toString());
    for (int i=0; i<facts.size(); i++) 
      {
        ValueVector f = facts.fact(i);
        if (f.get(RU.ID).factIDValue() != -1)
          ps.print(" f-" + f.get(RU.ID).factIDValue());
        if (i< facts.size() -1)
          ps.print(",");
      }
    ps.println();
  }
  
  /**
    Describe myself
    */

  public String toString() 
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Defrule: ");
    sb.append(RU.getAtom(m_name));
    sb.append(" ");
    if (m_docstring != null)
      sb.append("\"" + m_docstring + "\"; ");
    sb.append(m_patts.size());
    sb.append(" patterns; salience: ");
    sb.append(m_salience);
    sb.append("]");
    return sb.toString();
  }

  public String listNodes()
  {
    StringBuffer sb = new StringBuffer(100);
    for (int i=0; i< m_nodes.size(); i++)
      {
        sb.append(((Successor) m_nodes.elementAt(i)).m_node);
        sb.append("\n");
      }
    return sb.toString();
  }

  void addNode(Successor n)
  {
    m_nodes.addElement(n);
  }

  // ************************************************************
  // Completely remove this rule from the Rete network, including
  // removing any internal nodes that are only used by this rule.
  
  void remove(Vector roots)
  {
    Enumeration e = m_nodes.elements();
    while (e.hasMoreElements())
      {
        Successor s = (Successor) e.nextElement();
        if (--s.m_node.m_usecount <= 0)
          {
            roots.removeElement(s);
            Enumeration e2 = m_nodes.elements();
            while (e2.hasMoreElements())
              {
                Node n = ((Successor) e2.nextElement()).m_node;
                n.removeSuccessor(s);
              }
          }
      }
    // Straighten out the nodes we disturbed.
    e = m_nodes.elements();
    while (e.hasMoreElements())
      {
        Successor s = (Successor) e.nextElement();
        s.m_node.freeze();
      }
    m_nodes.removeAllElements();
  }

  /* **********************************************************************
   * following additions by Rajaram Ganeshan 12/29/97
   * to pretty print rules in JESS:
   * 1. public String ppRule()
   * 2. private String isBoundPattern( int factIndex )
   * 
   ********************************************************************** */

  public String ppRule() throws ReteException
  {
    String varname;
    StringBuffer sb = new StringBuffer(256);
    sb.append("(defrule ");
    sb.append(RU.getAtom(m_name));
    sb.append("\n");
    
    // Defrule docstrings are not required,
    // just encouraged...
    if ( m_docstring != null )
      {
        sb.append("   \"");
        sb.append(m_docstring);
        sb.append("\"\n");
      }  
    
    // check for salience declaration
    if ( m_salience != 0 )
      {
        sb.append("(declare (salience ");
        sb.append(m_salience);
        sb.append("))\n");
      }
    
    // patterns for LHS
    for (int i=0; i<m_patts.size(); i++)
      {
        Pattern p = (Pattern) m_patts.elementAt(i);
        // Two cases are possible:
        // 1. pattern bound to a variable. These look like this:
        // ?name <- (pattern 1 2 3)
        //  2. pattern not bound to a val looks like:
        // (pattern 1 2 3)
        
        // ignore initial-fact if present
        if ( i == 0 && p.name() == RU.putAtom("initial-fact") )
          continue;
        
        // check if pattern is negated
        if (p.negated() != 0)
          {
            // negated patterns cannot be bound to a value
            sb.append("   (not ");
            sb.append(p.ppPattern(m_engine));
            sb.append(")\n");
          } 
        else 
          {
            sb.append("   ");
            // check if this pattern was bound to a val            
            if ( (varname = isBoundPattern( i )) != null )
              {
                sb.append("?");
                sb.append(varname);
                sb.append(" <- ");
              }
            
            sb.append(p.ppPattern(m_engine));
            sb.append("\n");
          }
      }
    // "=>"
    sb.append("  =>\n");
    
    // actions for RHS
    for (int j=0; j < m_actions.size(); j++)
      {
        Funcall f = (Funcall) m_actions.elementAt(j);
        sb.append("   ");
        sb.append(f.ppFuncall(m_engine));
        sb.append("\n");
      }
    sb.append(")");
    return sb.toString();
  }
  
  private String isBoundPattern( int factIndex )
  {
    int size = m_bindings.size();
    for (int i=0; i<size; i++)
      {
        Binding b = (Binding) m_bindings.elementAt(i);
        if (b.m_slotIndex == RU.PATTERN)
          if ( b.m_factIndex == factIndex )
                return RU.getAtom(b.m_name);
      }
    return null;
  }
  
}


