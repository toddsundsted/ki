/** **********************************************************************
 *  Tiny class to hold a tag and a set of facts
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.Vector;

class Token
{

  int m_tag;
  int m_negcnt;

  /**
    sortcode is used by the engine to hash tokens
    and prevent long liner memory searches
    */

  int m_sortcode;

  private ValueVector m_fact;
  private int m_size;

  // Tokens refer to 'parents' (of which they are a superset)
  // 'next' is used to store tokens in a tree.
  private Token m_parent;
  Token m_next;

  final ValueVector fact(int i)
  {
    Token where = this;
    int j = m_size - i;
    while (--j > 0)
      where = where.m_parent;

    return where.m_fact;
  }

  final int size() { return m_size; }
  
  /**
    Constructor
    tag should be RU.ADD or RU.REMOVE
    */

  Token(int tag, ValueVector firstFact) throws ReteException
  {
    // m_parent = null;
    ++m_size;
    m_fact  = firstFact;
    m_tag = tag;
    // m_negcnt = 0;
    m_sortcode = firstFact.get(RU.ID).factIDValue();
  }

  Token() {/* For recycling... */ }

  Token resetToken(int tag, ValueVector firstFact) throws ReteException
  {
    m_parent = null;
    m_size = 1;
    m_fact  = firstFact;
    m_tag = tag;
    m_negcnt = 0;
    m_sortcode = firstFact.get(RU.ID).factIDValue();
    return this;
  }

  /**
    Create a new Token containing the same data as an old one
    */

  Token(Token t, ValueVector newFact) throws ReteException
  {
    m_fact = newFact;
    m_parent = t;
    m_tag = t.m_tag;
    // m_negcnt = 0;
    m_size = t.m_size + 1;
    m_sortcode = (t.m_sortcode << 3) + newFact.get(RU.ID).factIDValue();
  }

  /**
    Create a new Token identical to an old one
    */

  Token(Token t) throws ReteException
  {
    m_fact = t.m_fact;
    m_parent = t.m_parent;
    m_tag = t.m_tag;
    m_negcnt = 0;
    m_size = t.m_size;
    m_sortcode = t.m_sortcode;
    m_next = null;
  }

  /**
    Compare the data in this token to another token.
    The tokens are assumed to be of the same size (same number of facts).
    */

  final public boolean dataEquals(Token t)
  {
    if (m_sortcode != t.m_sortcode)
      return false;

    if (!m_fact.equals(t.m_fact))
      return false;

    else if (m_parent == null || m_parent == t.m_parent)
      return true;
    
    else
      return m_parent.dataEquals(t.m_parent);
    
  }

  public String toString() 
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Token: size=");
    sb.append(m_size);
    sb.append(";sortcode=");
    sb.append(m_sortcode);
    sb.append(";tag=");
    sb.append(m_tag == RU.ADD ? "ADD" : (m_tag == RU.UPDATE ? "UPDATE" : "REMOVE"));
    sb.append(";negcnt=");
    sb.append(m_negcnt);
    sb.append(";facts=");
    for (int i=0; i<m_size; i++)
      {
        sb.append(fact(i));
        sb.append(";");
      }
    sb.append("]");
    return sb.toString();
  }

}
