/** **********************************************************************
 *  Class used to represent deffacts.
 *  A Deffacts has no ValueVector  representation; it is always a class object.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class Deffacts 
{
  
  int m_name;
  Vector m_facts;
  String m_docstring;

  public final int name() { return m_name; }
  public final String docstring() { return m_docstring; }

  /**
    Constructor.
    */

  Deffacts(String name) 
  {
    m_name = RU.putAtom(name);
    m_facts = new Vector();
  }
  
  /**
    Add a fact to this deffacts
    */
  
  void addFact(Fact fact) throws ReteException 
  {
    addFact(fact.factData());
  }
  
  void addFact(ValueVector fact) 
  {
    m_facts.addElement(fact);
  }
  
  
  /**
    Describe myself
    */
  
  public String toString() 
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Deffacts: ");
    sb.append(RU.getAtom(m_name));
    sb.append(" ");
    if (m_docstring != null)
      sb.append("\"" + m_docstring + "\"; ");
    sb.append(m_facts.size());
    sb.append(" facts");
    sb.append("]");
    return sb.toString();
  }

}

