/** **********************************************************************
 * Fact.java
 * Convenience class used to parse and print facts.
 * A Fact is represented as a Value Vector where the first entry is the
 * head, the second a descriptor with type RU.ORDERED_ -
 * or RU.UNORDERED_ - FACT. The third entry has type RU.FACT_ID and is
 * the integer fact id.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class Fact
{

  /**
    The actual fact array
    */

  private ValueVector m_fact;

  /**
    The deftemplate corresponding to this fact
    */

  private ValueVector m_deft;
  private int m_name;
  private int m_ordered;

  /**
    Constructor.
    */

  public Fact(String name, int ordered, Rete engine) throws ReteException 
  {
    m_name = RU.putAtom(name);
    m_ordered = ordered;
    m_deft = findDeftemplate(name, m_ordered, engine);
    m_fact = createNewFact(m_deft);
  }

  /**
    Constructor
    starts from the ValueVector form of a Fact
    */

  public Fact(ValueVector f, Rete engine) throws ReteException 
  {
    m_name = f.get(RU.CLASS).atomValue();
    m_ordered = f.get(RU.DESC).descriptorValue();
    m_deft = findDeftemplate(RU.getAtom(m_name), m_ordered, engine);
    m_fact = (ValueVector) f.clone();
  }

  private ValueVector createNewFact(ValueVector deft) 
  {
    ValueVector fact = new ValueVector();
    int size = (m_deft.size() -RU.FIRST_SLOT)/RU.DT_SLOT_SIZE + RU.FIRST_SLOT;
    fact.setLength(size);

    fact.set(deft.get(RU.CLASS), RU.CLASS);
    fact.set(deft.get(RU.DESC), RU.DESC);
    fact.set(deft.get(RU.ID), RU.ID);
    int j = RU.FIRST_SLOT + RU.DT_DFLT_DATA;
    for (int i=RU.FIRST_SLOT; i< size; i++) 
      {
        fact.set(deft.get(j), i);
        j += RU.DT_SLOT_SIZE;
      }
    return fact;
  }


  /**
    find the deftemplate, if there is one, or create implied dt.
    */
  
  private ValueVector findDeftemplate(String name, int ordered, Rete engine)
       throws ReteException 
  {
    
    ValueVector deft;
    
    deft = engine.findDeftemplate(name);
    if (deft != null) 
      {
        if (deft.get(RU.DESC).descriptorValue() != ordered) 
          {
            // looks like there are semantic errors in input.
            throw new ReteException("Fact::findDeftemplate",
                                    "Attempt to duplicate implied deftemplate:",
                                    name);
          }
      }
    else  
      {
        // this is OK. Create an implied deftemplate if this is an ordered fact.
        if (ordered == RU.UNORDERED_FACT) 
          {
            throw new ReteException("Fact::findDeftemplate",
                                    "Can't create implied unordered deftempl:",
                                    name);
          }
        deft = engine.addDeftemplate(new Deftemplate(name,ordered));
      }
    return deft;
  }
  
  
  /**
    Add a value to this fact
    */
  
  final int findSlot(String slotname) throws ReteException 
  {
    return findSlot(RU.putAtom(slotname));
  }

  final int findSlot(int slotname) throws ReteException 
  {

    if (m_ordered == RU.ORDERED_FACT)
      throw new ReteException("Fact::FindSlot",
                              "Attempt to find named slot in ordered fact",
                              "");

    // try to find this slotname in the deftemplate
    int i;
    for (i=RU.FIRST_SLOT; i < m_deft.size(); i+=RU.DT_SLOT_SIZE)
      if (m_deft.get(i + RU.DT_SLOT_NAME).atomValue() == slotname)
        break;
    if (i >= m_deft.size())
      throw new ReteException("Fact::AddValue",
                              "Attempt to add field with invalid slotname",
                              RU.getAtom(slotname));
    return (i - RU.FIRST_SLOT) / RU.DT_SLOT_SIZE + RU.FIRST_SLOT;
  }

  final public Value findValue(String slotname) throws ReteException
  {
    int i = findSlot(slotname);
    return m_fact.get(i);
  }

  final public void addValue(String slotname, String value, int type)
       throws ReteException 
  {
    int i = findSlot(slotname);
    m_fact.set(new Value(value, type),i);
  }
  
  final public void addValue(String slotname, int value, int type)
       throws ReteException 
  {
    int i = findSlot(slotname);
    m_fact.set(new Value(value, type),i);
  }

  final public void addValue(String slotname, double value, int type)
       throws ReteException 
  {
    int i = findSlot(slotname);
    m_fact.set(new Value(value, type),i);
  }

  final public void addValue(String slotname, Funcall value, int type)
       throws ReteException 
  {
    int i = findSlot(slotname);
    m_fact.set(new Value(value, type),i);
  }

  final public void addValue(String slotname, Value value) throws ReteException 
  {
    int i = findSlot(slotname);
    m_fact.set(new Value(value),i);
  }


  final public void addValue(String value, int type) throws ReteException 
  {
    addValue(RU.putAtom(value), type);
  }

  

  final public void addValue(int value, int type) throws ReteException 
  {
    if (m_ordered == RU.UNORDERED_FACT)
      throw new ReteException("Fact::AddValue",
                              "Can't add ordered field to unordered fact",
                              "");

    m_fact.add(new Value(value, type));
  }

  final public void addValue(double value, int type) throws ReteException 
  {
    if (m_ordered == RU.UNORDERED_FACT)
      throw new ReteException("Fact::AddValue",
                              "Can't add ordered field to unordered fact",
                              "");

    m_fact.add(new Value(value, type));
  }

  final public void addValue(Funcall value, int type) throws ReteException 
  {
    if (m_ordered == RU.UNORDERED_FACT)
      throw new ReteException("Fact::AddValue",
                              "Can't add ordered field to unordered fact",
                              "");

    m_fact.add(new Value(value, type));
  }

  final public void addValue(Value value) throws ReteException 
  {
    m_fact.add(new Value(value));
  }

  /**
    Returns the actual fact
    */

  final public ValueVector factData() 
  {
    return m_fact;
  }

  /**
    Returns the actual fact
    */

  public final ValueVector deft() 
  {
    return m_deft;
  }

  public String toString() 
  {
    StringBuffer sb = new StringBuffer(100);
    try 
      {
        sb.append("(");
        sb.append(RU.getAtom(m_name));
        if (m_ordered == RU.ORDERED_FACT) 
          {
            // print each slot
            for (int i=RU.FIRST_SLOT; i< m_fact.size(); i++) 
              {
                sb.append(" ");
                sb.append(m_fact.get(i));
              }
        
          } 
        else 
          { // UNORDERED_FACT
        
            int nslots = (m_deft.size() - RU.FIRST_SLOT) / RU.DT_SLOT_SIZE;
            for (int i=0; i< nslots; i++) 
              {
                sb.append(" (");
                sb.append(m_deft.get(RU.FIRST_SLOT + 
                                     (i*RU.DT_SLOT_SIZE)+RU.DT_SLOT_NAME)
                          .stringValue());
                sb.append(" ");
                sb.append(m_fact.get(RU.FIRST_SLOT + i));
                sb.append(")");
              }
          }
        sb.append(")");
        return sb.toString();
      }
    catch (ReteException re) 
      {
        return re.toString();
      }
  }

}









