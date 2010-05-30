/** **********************************************************************
 * Terminal nodes of the pattern network
 * Package up the info needed to fire a rule and create an Activation
 * object containing this info.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class NodeTerm extends Node
{
  
  /**
    The rule we activate
    */

  private Defrule m_rule;

  public Defrule rule() { return m_rule; }

  /**
    Activations we've created. We need to keep an eye on them so that
    we can retract them if need be.
    */

  private Vector m_activations = new Vector();

  /**
    Constructor
    */

  public NodeTerm(Defrule rule, Rete engine)
  {
    super(engine);
    m_rule = rule;
  }
  
  /**
    An activation has been cancelled; forget it
    */

  public void standDown(Activation a) throws ReteException 
  {
    m_activations.removeElement(a);
    m_engine.standDown(a);
  }

  /**
    An activation has been fired; forget it
    */

  public void ruleFired(Activation a) 
  {
    m_activations.removeElement(a);
    m_engine.ruleFired(a);
  }

  /**
    Add an activation to this rule
    */
     
  private void doAddCall(Token token) throws ReteException
  {
    if (token.m_tag == RU.UPDATE)
      {
        // Check to make sure we don't already know about this token
        for (int i=0; i< m_activations.size(); i++)
          {
            Activation a = (Activation) m_activations.elementAt(i);
            if (a.m_token.dataEquals(token))
              return;
          }
      }

    Activation a = new Activation(token, m_rule, this);
    m_activations.addElement(a);
    m_engine.addActivation(a);
  }


  /**
    All we need to do is create or destroy the appropriate Activation
    object, which contains enough info to fire a rule.
    */
  
  public boolean callNode(Token token, int callType) throws ReteException 
  {
    // debugPrint(token, callType);
    switch (token.m_tag) 
      {
      
      case RU.CLEAR:
        {
          for (int i=0; i< m_activations.size(); i++)
            standDown((Activation) m_activations.elementAt(i));
          break;
        }

      case RU.UPDATE:
      case RU.ADD:
        {
          doAddCall(token);
          break;
        }

      case RU.REMOVE:
        {
          int size = m_activations.size();
          for (int i=0; i < size; i++) 
            {
              Activation a = (Activation) m_activations.elementAt(i);
              if (token.dataEquals(a.m_token)) 
                {
                  standDown(a);
                  return true;
                }
            }
          break;
        }
      }
    return true;
  }

  /**
    callNode can call this to show debug info
    */

  private void debugPrint(Token token, int callType) 
  {
    System.out.println("TEST " + toString() +
                       ";tag=" + token.m_tag + "callType="+ callType);
  }

  /**
    Describe myself
    */
  
  public String toString() 
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[NodeTerm rule=");
    sb.append(RU.getAtom(m_rule.m_name));
    sb.append(";Activations:\n");
    for (int i=0; i< m_activations.size(); i++)
      {
      sb.append(m_activations.elementAt(i));
      sb.append("\n");
      }
    sb.append(";usecount = ");
    sb.append(m_usecount);
    sb.append("]");
    return sb.toString();
  }


}

