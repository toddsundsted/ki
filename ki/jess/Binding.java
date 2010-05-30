/** **********************************************************************
 * Another tiny container class
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

class Binding implements Cloneable 
{
  int m_name;
  int m_factIndex;
  int m_slotIndex;
  int m_subIndex;

  Value m_val;

  public Binding(int name, int factIndex, int slotIndex, int subIndex) 
  {
    m_name = name;
    m_factIndex = factIndex;
    m_slotIndex = slotIndex;
    m_subIndex = subIndex;
    m_val = null;
  }
  public Binding(int name, Value val) 
  {
    m_name = name;
    m_factIndex = RU.LOCAL;
    m_slotIndex = RU.LOCAL;
    m_subIndex = -1;
    m_val = val;
  }

  public Object clone() 
  {
    Binding b = new Binding(m_name, m_factIndex, m_slotIndex, m_subIndex);
    b.m_val = m_val;
    return b;
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Binding: ");
    sb.append(RU.getAtom(m_name));
    sb.append(";factIndex=" + m_factIndex);
    sb.append(";slotIndex=" + m_slotIndex);
    sb.append(";subIndex=" + m_subIndex);
    sb.append(";val=" + m_val);
    sb.append("]");
    return sb.toString();
  }

}
