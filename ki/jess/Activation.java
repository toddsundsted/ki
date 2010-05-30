/** **********************************************************************
 * An activation of a rule. Contains enough info to bind
 * a rule's variables.
 * 
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

// This class is public, but please don't too friendly with it!
public class Activation
{

  /**
    Token is the token that got us fired.
   */
  
  Token m_token;

  /**
    Rule is the rule we will fire.
   */

  Defrule m_rule;

  /**
    Nt is the NodeTerm that created us; we need to notify it when
    we die.
   */

  private NodeTerm m_nt;

  /**
    Constructor
    */

  Activation(Token token, Defrule rule, NodeTerm nt) 
  {
    m_token = token;
    m_rule = rule;
    m_nt = nt;
  }

  /**
    Fire my rule
    */

  boolean fire() throws ReteException 
  {
    m_rule.fire(m_token);
    m_nt.ruleFired(this);
    return true;
  }


  public String toString()
  {
    try
      {
        StringBuffer sb = new StringBuffer(100);
        sb.append("[Activation: ");
        sb.append(Rete.factList(m_token));
        sb.append("]");
        return sb.toString();
      }
    catch (ReteException re) { return re.toString(); }
  }

}

