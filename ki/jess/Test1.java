/** **********************************************************************
 *  A tiny class to hold an individual test for a 1-input node to perform
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

class Test1
{

  final static int EQ  = 0;
  final static int NEQ = 1;

  /**
    What test to do (Test1.EQ, Test1.NEQ, etc)
   */

  int m_test;

  /**
    Which slot within a fact (0,1,2...)
   */

  int m_slotIdx;

  /**
    Which subslot within a multislot (0,1,2...)
   */

  int m_subIdx;

  /**
    The datum to test against
    */

  Value m_slotValue;

  /**
    Constructor
    */
  Test1(int test, int slot_idx, Value slot_value)
  {
    this(test, slot_idx, -1, slot_value);
  }

  Test1(int test, int slot_idx, int sub_idx, Value slot_value) 
  {
    m_test = test;
    m_slotIdx = slot_idx;
    m_subIdx = sub_idx;
    m_slotValue = slot_value;
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Test1: test=");
    sb.append(m_test == NEQ ? "NEQ" : "EQ");
    sb.append(";slot_idx=");
    sb.append(m_slotIdx);
    sb.append(";sub_idx=");
    sb.append(m_subIdx);
    sb.append(";slot_value=");
    sb.append(m_slotValue);
    sb.append("]");

    return sb.toString();
  } 


}



