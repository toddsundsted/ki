/** **********************************************************************
 *  Class used to represent the Global Execution Context of a Rete engine.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

class GlobalContext extends Context
{
  
  private Vector m_globalBindings;

  /**
    Constructor.
    */

  public GlobalContext(Rete engine)
  {
    super(engine);
    m_globalBindings = new Vector();
  }

  /**
    Find a variable table entry
    */
  
  Binding findGlobalBinding(int name)
  {
    for (int i=0; i< m_globalBindings.size(); i++)
      {
        Binding b = (Binding) m_globalBindings.elementAt(i);
        if (b.m_name == name)
          return b;
      }
    return null;
  }

  /**
    Make note of a global variable during Parsing.
    */

  final public Binding addGlobalBinding(int name, Value value)
  {
    Binding b;
    if ((b = findGlobalBinding(name)) != null) 
      {
        b.m_val = value;
        return b;
      }
    b = new Binding(name, value);
    m_globalBindings.addElement(b);
    return b;
  }

  
  /**
    Describe myself
    */

  public String toString()
  {
    return "[GlobalContext]";
  }

}

