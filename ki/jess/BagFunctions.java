/** **********************************************************************
 * User-defined functions for manipulating 'bags' of properties
 * 
 * To use one of these functions from Jess, simply register the
 * package class in your Java mainline:
 *
 * engine.AddUserpackage(new BagFunctions());
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class BagFunctions implements Userpackage 
{

  public void add(Rete engine) 
  {
    engine.addUserfunction(new bag());
  }
}

class bag implements Userfunction 
{
  private Hashtable m_bags = new Hashtable();

  private int m_name = RU.putAtom("bag");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String command = vv.get(1).stringValue();

    // Create, destroy and find bags.

    if (command.equals("create"))
      {
        String name = vv.get(2).stringValue();
        Hashtable bag = (Hashtable) m_bags.get(name);
        if (bag == null)
          {
            bag = new Hashtable();
            m_bags.put(name, bag);
          }
        return new Value(bag, RU.EXTERNAL_ADDRESS);
      }
    else if (command.equals("delete"))
      {
        String name = vv.get(2).stringValue();
        m_bags.remove(name);
        return Funcall.TRUE();
      }
    else if (command.equals("find"))
      {
        String name = vv.get(2).stringValue();
        Hashtable bag = (Hashtable) m_bags.get(name);
        if (bag != null)
          return new Value(bag, RU.EXTERNAL_ADDRESS);
        else
          return Funcall.NIL();
      }
    else if (command.equals("list"))
      {
        ValueVector rv = new ValueVector();
        Enumeration e = m_bags.keys();
        while (e.hasMoreElements())
          rv.add(new Value( (String) e.nextElement(), RU.STRING));
        return new Value(rv, RU.LIST);
      }

    // Set, check and read properties of bags

    else if (command.equals("set"))
      {
        Hashtable bag = (Hashtable) vv.get(2).externalAddressValue();
        String name = vv.get(3).stringValue();
        bag.put(name, vv.get(4));
        return vv.get(4);
      }
    else if (command.equals("get"))
      {
        Hashtable bag = (Hashtable) vv.get(2).externalAddressValue();
        String name = vv.get(3).stringValue();
        Value v = (Value) bag.get(name);
        if (v != null)
          return v;
        else
          return Funcall.NIL();
      }
    else if (command.equals("props"))
      {
        Hashtable bag = (Hashtable) vv.get(2).externalAddressValue();
        ValueVector rv = new ValueVector();
        Enumeration e = bag.keys();
        while (e.hasMoreElements())
          rv.add(new Value( (String) e.nextElement(), RU.STRING));
        return new Value(rv, RU.LIST);
      }
    
    else
      throw new ReteException("bag", "Unknown command", command);
  }
}
