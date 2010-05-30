/** **********************************************************************
 *  A tiny class to hold an individual test for a 2-input node to perform
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

class Test2
{

  final static int EQ  = 0;
  final static int NEQ = 1;

  /**
    What test to do (Test2.EQ, Test2.NEQ, etc)
   */

  int m_test;

  /**
    Which fact within a token (0,1,2...)
   */

  int m_tokenIdx;

  /**
    Which field (absolute index of slot start) from the left memory
   */

  int m_leftIdx;

  /**
    Which subfield from the left memory
   */

  int m_leftSubIdx;


  /**
    Which field (absolute index of slot start) from the right memory
   */

  int m_rightIdx;

  /**
    Which subfield from the right memory
   */

  int m_rightSubIdx;

  /**
    Constructors
    */

  Test2(int test, int tokenIdx, int leftIdx, 
               int leftSubIdx, int rightIdx, int rightSubIdx)
  {
    m_test = test;
    m_tokenIdx = tokenIdx;
    m_rightIdx = rightIdx;
    m_rightSubIdx = rightSubIdx;
    m_leftIdx = leftIdx;
    m_leftSubIdx = leftSubIdx;
  }

  Test2(int test, int tokenIdx, int leftIdx, int rightIdx)
  {
    this(test, tokenIdx, leftIdx, -1, rightIdx, -1);
  }

  boolean equals(Test2 t)
  {
    return  (m_test == t.m_test &&
             m_tokenIdx == t.m_tokenIdx &&
             m_rightIdx == t.m_rightIdx &&
             m_leftIdx == t.m_leftIdx &&
             m_rightSubIdx == t.m_rightSubIdx &&
             m_leftSubIdx == t.m_leftSubIdx);
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Test2: test=");
    sb.append(m_test == NEQ ? "NEQ" : "EQ");
    sb.append(";tokenIdx=");
    sb.append(m_tokenIdx);
    sb.append(";leftIdx=");
    sb.append(m_leftIdx);
    sb.append(";leftSubIdx=");
    sb.append(m_leftSubIdx);
    sb.append(";rightIdx=");
    sb.append(m_rightIdx);
    sb.append(";rightSubIdx=");
    sb.append(m_rightSubIdx);
    sb.append("]");

    return sb.toString();
  }


}


