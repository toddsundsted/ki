/** **********************************************************************
 * Convenience class used to parse and print patterns.
 * A Pattern consists mainly of a two-dimensional array of Test1 structures.
 * Each Test1 contains information about a specific characteristic of a slot.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */
package jess;

import java.util.*;


class Pattern 
{

  /**
    The deftemplate corresponding to this pattern
    */

  private ValueVector m_deft;

  /**
    The number of slots in THIS pattern
    */

  int m_nvalues;

  public int size() { return m_nvalues + RU.FIRST_SLOT; }

  /**
    The Slot tests for this pattern
    */

  public Test1[][] m_tests;


  public int m_slotLengths[];


  /**
    Am I in a (not () ) ?
    */
  
  private int m_negated;

  /**
    Class of fact matched by this pattern
    */

  private int m_class;

  /**
    ordered or unordered
    */

  private int m_ordered;


  boolean m_hasVariables, m_compacted;

  /**
    Constructor.
    */

  public Pattern(String name, int ordered, Rete engine, int negcnt)
       throws ReteException 
  {
    m_class = RU.putAtom(name);
    m_ordered = ordered;
    m_nvalues = 0;
    m_deft = findDeftemplate(name, engine);
    m_negated = negcnt;

    if (m_ordered == RU.ORDERED_FACT)
      m_tests = new Test1[RU.MAXFIELDS][];
    else 
      {
        m_nvalues = (m_deft.size() - RU.FIRST_SLOT) / RU.DT_SLOT_SIZE;
        m_tests = new Test1[m_nvalues][];
        m_slotLengths = new int[m_nvalues];
        for (int i=0; i<m_nvalues; i++)
          m_slotLengths[i] = -1;
      }

  }


  /**
    find the deftemplate, if there is one, or create implied dt.
    */

  ValueVector findDeftemplate(String name, Rete engine) throws ReteException 
  {

    m_deft = engine.findDeftemplate(name);
    if (m_deft != null) 
      {
        if (m_ordered != m_deft.get(RU.DESC).descriptorValue()) 
          // looks like there are semantic errors in input.
          throw new ReteException("Pattern::FindDeftemplate",
                                  "Attempt to duplicate implied deftemplate:",
                                  name);
      }
    else  
      {
        // this is OK. Create an implied deftemplate if this is an ordered pattern.
        if (m_ordered == RU.UNORDERED_FACT) 
          throw new ReteException("Pattern::FindDeftemplate",
                                  "Attempt to create implied unordered deftemplate:",
                                  name);
        else
          m_deft = engine.addDeftemplate(new Deftemplate(name,m_ordered));
        
      }
    return m_deft;
  }

  /**
    set the length of a multislot within a pattern
    */

  public void setMultislotLength(String slotname, int length) 
       throws ReteException 
  {
    if (m_ordered == RU.ORDERED_FACT)
      throw new ReteException("Pattern::SetMultislotLength",
                              "Attempt to set slot length on ordered pattern",
                              "");

    // try to find this slotname in the deftemplate
    int i;
    for (i=RU.FIRST_SLOT; i< m_deft.size(); i+=RU.DT_SLOT_SIZE)
      if (m_deft.get(i).stringValue().equals(slotname))
        break;
    if (i >= m_deft.size())
      throw new ReteException("Pattern::SetMultislotLength",
                              "Attempt to set length of invalid slotname",
                              slotname);

    int idx = i - RU.FIRST_SLOT;
    idx /= RU.DT_SLOT_SIZE;
    m_slotLengths[idx] = length;

  }

  /**
    Add a value to this pattern
    */

  public void addTest(String slotname,Value value, int subidx, boolean negated)
       throws ReteException 
  {
    addTest(RU.putAtom(slotname), value, subidx, negated);
  }

  public void addTest(int slotname, Value value,  int subidx, boolean negated)
       throws ReteException 
  {
    if (m_ordered == RU.ORDERED_FACT)
      throw new ReteException("Pattern::AddValue",
                              "Attempt to add slot integer to ordered pattern",
                              "");

    // try to find this slotname in the deftemplate
    int i;
    for (i=RU.FIRST_SLOT; i< m_deft.size(); i+=RU.DT_SLOT_SIZE)
      if (m_deft.get(i).atomValue() == slotname)
        break;
    if (i >= m_deft.size())
      throw new ReteException("Pattern::AddValue",
                              "Attempt to add field with invalid slotname",
                              RU.getAtom(slotname));

    int idx = i - RU.FIRST_SLOT;
    idx /= RU.DT_SLOT_SIZE;

    if (m_tests[idx] == null)
      m_tests[idx] = new Test1[RU.MAXFIELDS];

    int j=0;
    while (m_tests[idx][j] != null)
      ++j;
    
    // this negated refers to tests, not the pattern overall
    if (negated)
      m_tests[idx][j] = new Test1(Test1.NEQ, i, subidx, value);
    else
      m_tests[idx][j] = new Test1(Test1.EQ, i, subidx, value);
      
    
  }


  public void addTest(Value value, boolean negated) throws ReteException 
  {
    if (m_ordered == RU.UNORDERED_FACT)
      throw new ReteException("Pattern::AddValue",
                              "Attempt to add ordered field to unordered pattern",
                              "");

    int i = RU.FIRST_SLOT + m_nvalues* RU.DT_SLOT_SIZE;

    if (m_nvalues < RU.MAXFIELDS) 
      {
      
        if (m_tests[m_nvalues] == null)
          m_tests[m_nvalues] = new Test1[RU.MAXFIELDS];
      
        int j=0;
        while (m_tests[m_nvalues][j] != null)
          ++j;

        // this negated refers to tests, not the pattern overall
        if (negated)
          m_tests[m_nvalues][j] = new Test1(Test1.NEQ, i, value);
        else
          m_tests[m_nvalues][j] = new Test1(Test1.EQ, i, value);

      }
    else
      throw new ReteException("Pattern::AddValue",
                              "MaxFields exceeded",
                              "");
  }

  /**
    point at the next slot.
    */
  
  public void advance() 
  {
    ++m_nvalues;
  }

  /**
    Is this pattern a (not()) CE pattern, possibly nested?
    */

  public int negated()
  {
    return m_negated;
  }

  public int name() 
  {
    return m_class;
  }

  public int ordered() 
  {
    return m_ordered;
  }

  /**
    Shrink all our subarrays to the needed size.
    */

  public void compact() 
  {
    if (m_compacted)
      return;
    else
      m_compacted = true;

    if (m_ordered == RU.ORDERED_FACT) 
      {
        // if this was an unordered fact the 'backbone'
        // would already be the right size; since this is ordered,
        // we allocated it too big.

        Test1[][] nt = new Test1[m_nvalues][];
        for (int i=0; i< m_nvalues; i++)
          nt[i] = m_tests[i];
        m_tests = nt;
      }
    
    int size = m_tests.length;
    for (int i=0; i < size; i++) 
      {
        // now create new subarrays of just the right size.
        if (m_tests[i] == null)
          continue;
        int n = 0;
        while (m_tests[i][n] != null) 
          {
            n++;
          }
        if (n != 0) 
          {
            Test1[] nt = new Test1[n];
            for (int j=0; j<n; j++)
              nt[j] = m_tests[i][j];
            m_tests[i] = nt;
          } 
        else 
          {
            m_tests[i] = null;
          }
      }
  }

  /**
    Describe myself
    */

  public String toString() 
  {
    String s = "[Pattern: " + RU.getAtom(m_class) + " ";
    s += (m_ordered == RU.ORDERED_FACT) ? "(ordered)" : "(unordered)";
    if (m_negated != 0) s += " (negated : " + m_negated + ")";
    s += "]";
    return s;
  }
  



  /* **********************************************************************
   * Following functions added by Rajaram
   * 1. public String ppPattern(Rete engine)
   * 2. private String ppTest1(Test1 atst, Rete engine)
   *
   * Prints out patterns in LHS of rules. Should be called
   * from ppRule. Negation is taken care of in Defrule.ppRule()
   *     
   * ********************************************************************** */

  String ppPattern(Rete engine) throws ReteException
  {
    return doPPPattern(engine);
  }

  String doPPPattern(Rete engine) throws ReteException
  {
    String slotname;
    StringBuffer sb = new StringBuffer(100);
    sb.append("(");
    sb.append(RU.getAtom(m_class));
    sb.append(" ");
    //
    // unordered (<classname> (<slot1> <tests>) ..... (<slotn> <tests>))
    // <tests> := <testn> | <testn>&<testn>
    // <testn> := ~<test>
    // <test>  := ATOM | NUMBER | STRING | ?VAR | :FUNCALL
    //  - unordered is defined by deftemplate
    // ordered (<classname> <tests> <tests> <tests> <tests> ...)

    boolean isTest = (m_class == RU.putAtom("test"));

    if ( m_ordered == RU.UNORDERED_FACT )
      {
        for( int i=0; i<m_tests.length; i++ )
          {
            if( m_tests[i] == null )
              continue;

            for( int j=0; j<m_tests[i].length; j++ )
              {
                if( m_tests[i][j] == null )
                  continue;
                Test1 atst = m_tests[i][j];
                slotname = RU.getAtom(m_deft.get(atst.m_slotIdx).atomValue());
                if ( j == 0 )
                  {
                    sb.append("(");
                    sb.append(slotname);
                    sb.append(" ");
                  }
                if ( j != 0 )
                  sb.append("&");
                sb.append(ppTest1(atst, engine, isTest));
              }
            sb.append(") ");
        }
      } 
    else 
      {
        for( int i=0; i<m_tests.length; i++ )
          {
            if( m_tests[i] == null )
                continue;
            
            for( int j=0; j<m_tests[i].length; j++ )
              {                
                if( m_tests[i][j] == null )
                    continue;

                Test1 atst = m_tests[i][j];
                if ( j != 0 )
                  sb.append("&");
                sb.append(ppTest1(atst,engine, isTest));
            }
            sb.append(" ");
          }
      }
    sb.append(")");
    return sb.toString();
  }
  
  private String ppTest1(Test1 atst, Rete engine, boolean isTest)
       throws ReteException
  {
    String s = "";
    if (atst.m_test == Test1.NEQ)
      s += "~";
    int type = atst.m_slotValue.type();
    switch (type)
      {
      case RU.ATOM:
      case RU.FLOAT:
      case RU.INTEGER:
      case RU.STRING:
      case RU.VARIABLE:
        s += atst.m_slotValue.toString();
        break;
      case RU.FUNCALL:
        Funcall f = atst.m_slotValue.funcallValue();
        if (!isTest)
          s += ":";
        s += f.ppFuncall(engine);
        break;
      }
    return s;
  }
}
  

