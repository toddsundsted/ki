/** **********************************************************************
 *  Class used to represent Defglobals.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;


public class Defglobal 
{
  
  Vector m_bindings;

  /**
    Constructor.
    */

  Defglobal()
  {
    m_bindings = new Vector();
  }
  
  /**
    Add a variable to this Defglobal
    */


  void addGlobal(String name, Value val) 
  {
    m_bindings.addElement(new Binding(RU.putAtom(name),val));
  }

  
  /**
    Describe myself
    */

  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Defglobal: ");
    sb.append(m_bindings.size());
    sb.append(" variables");
    sb.append("]");
    return sb.toString(); 
  }

}

