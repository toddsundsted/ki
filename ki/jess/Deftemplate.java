/** **********************************************************************
 * Convenience class used to parse and print deftemplates.
 * A deftemplate is represented as a ValueVector
 *
 * An ordered deftemplate has only the first few fields, no slots
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class Deftemplate
{  
  private ValueVector m_deft;
  int m_name;
  int m_ordered;
  String m_docstring;

  public final int name() { return m_name; }
  public final String docstring() { return m_docstring; }
  public final void docstring(String d) { m_docstring = d; }

  /**
    Constructor.
    */

  public Deftemplate(String name, int ordered) throws ReteException 
  {
    m_deft = new ValueVector();
    m_name = RU.putAtom(name);
    m_ordered = ordered;
    m_deft.setLength(3);
    m_deft.set(new Value(m_name, RU.ATOM), RU.CLASS);
    m_deft.set(new Value(m_ordered, RU.DESCRIPTOR), RU.DESC);
    m_deft.set(new Value(0, RU.FACT_ID), RU.ID);
  }
  
  /**
    Constructor
    starts from the ValueVector form of a Deftemplate
    */

  public Deftemplate(ValueVector dt) throws ReteException 
  {
    m_deft = (ValueVector) dt.clone();
    m_name = dt.get(RU.CLASS).atomValue();
    m_ordered = dt.get(RU.DESC).descriptorValue();
  }


  /**
    Create a new slot in this deftemplate
    */

  public void addSlot(String name, Value value) throws ReteException 
  {
    if (m_ordered != RU.UNORDERED_FACT)
      throw new ReteException("AddSlot",
                              "Ordered deftemplates cannot have slots:",
                              name);
    m_deft.add(new Value(name, RU.SLOT));
    m_deft.add(value);

  }

  /**
    Create a new multislot in this deftemplate
    */

  public void addMultiSlot(String name, Value value) throws ReteException 
  {
    if (m_ordered != RU.UNORDERED_FACT)
      throw new ReteException("AddSlot",
                              "Ordered deftemplates cannot have slots:",
                              name);
    m_deft.add(new Value(name, RU.MULTISLOT));
    m_deft.add(value);
  }
  
  /**
    Generates the ValueVector form of this Deftemplate
    */

  ValueVector deftemplateData() 
  {
    return m_deft;
  }

  /**
    Report whether a given slot is multi or not in a ValueVector dt
    */

  static int slotType(ValueVector vvec, String slotname)
       throws ReteException 
  {

    if (vvec.get(RU.DESC).descriptorValue() != RU.UNORDERED_FACT)
      return RU.NONE;

    int slotint = RU.putAtom(slotname);

    for (int i = RU.FIRST_SLOT; i < vvec.size(); i += RU.DT_SLOT_SIZE)
      if (vvec.get(i + RU.DT_SLOT_NAME).atomValue() == slotint)
        return vvec.get(i + RU.DT_SLOT_NAME).type();
    
    return RU.NONE;
  }


  
  /**
    Describe myself
    */

  public String toString() 
  {
    try 
      {
        StringBuffer sb = new StringBuffer(100);
        sb.append("[Deftemplate: ");
        sb.append(RU.getAtom(m_name));
        sb.append(" ");
        sb.append((m_ordered == RU.ORDERED_FACT) ? "(ordered)" : "(unordered)");
        if (m_docstring != null)
          sb.append(" \"" + m_docstring + "\" ");
        if (m_ordered == RU.UNORDERED_FACT) 
          {
            sb.append("slots:");
            for (int i=RU.FIRST_SLOT; i<m_deft.size(); i+=RU.DT_SLOT_SIZE)
              {
                sb.append(" ");
                sb.append(m_deft.get(i + RU.DT_SLOT_NAME).stringValue());
                sb.append(" default: ");
                sb.append(m_deft.get(i + RU.DT_DFLT_DATA).toString());
                sb.append(";");
              }
          }
        sb.append("]");
        return sb.toString(); 
      } 
    catch (ReteException re) 
      {
        return re.toString();
      }      
  }
}


