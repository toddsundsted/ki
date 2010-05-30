/** **********************************************************************
 * Node containing an arbitrary list of tests; used for TEST CE's and also
 * the base class for join nodes.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class NodeTest extends Node
{
  
  /**
    The tests this node performs
    */

  Vector m_tests;
  Object[] m_localTests;

  /**
    Constructor
    */

  NodeTest(Rete engine) 
  {
    super(engine);
    m_tests = new Vector();
  }

  void complete() 
  {
    // freeze the tests vector
    m_localTests = new Object[m_tests.size()];
    for (int i=0; i< m_tests.size(); i++)
      m_localTests[i] = m_tests.elementAt(i); 
  }

  // Bare NodeTests can only have Test1's in them.
  void addTest(int test, int slot_idx, Value v) 
  {
    m_tests.addElement(new Test1(test, slot_idx, -1, v));
  }

  void addTest(int test, int slot_idx, int slot_sidx, Value v) 
  {
    m_tests.addElement(new Test1(test, slot_idx, slot_sidx, v));
  }

  // The classic 'you should never inherit from concrete classes' problem!

  void addTest(int test, int token_idx, int left_idx, int right_idx) 
       throws ReteException
  {
    throw new ReteException("NodeTest:addtest",
                            "Can't add Test2s to this class", "");

  }

  void addTest(int test, int token_idx, int left_idx, int leftSub_idx,
               int right_idx, int rightSub_idx)
       throws ReteException
  {
    throw new ReteException("NodeTest:addtest",
                            "Can't add Test2s to this class", "");
  }

  /**
    For our purposes, two Node2's are equal if every test in
    one has an equivalent test in the other, and if the test vectors are
    the same size. The subclass NodeNot2 should never be shared, so we'll
    report unequal always. This routine is used during network compilation,
    not at runtime.
    */

  boolean equals(NodeTest n) 
  {
    if (this == n)
      return true;
      
    if (this.getClass() != n.getClass() ||
        this instanceof NodeNot2 ||
        n instanceof NodeNot2 ||
        n.m_tests.size() != m_tests.size())
      return false;

  outer_loop:
    for (int i=0; i<m_tests.size(); i++) 
      {
        // Test1 nodes hold function call tests. They're too
        // complicated to try to share.
        if (m_tests.elementAt(i) instanceof Test1)
          return false;
        Test2 t1 = (Test2) m_tests.elementAt(i);
        for (int j=0; j<m_tests.size(); j++) {
          if (t1.equals(n.m_tests.elementAt(j)))
            continue outer_loop;
        }
        return false;
      }
    return true;
  }

  boolean callNode(Token token, int callType) throws ReteException
  {
    if (token.m_tag == RU.CLEAR)
      {
        passAlong(token);
        return true;
      }
    

    boolean result = runTests(token);
    if (result)
      {
        Successor [] sa = m_localSucc;
        int nsucc = sa.length;
        for (int j=0; j<nsucc; j++) 
          {
            Successor s = sa[j];
            s.m_node.callNode(token, s.m_callType);
          }
      }
    return result;
  }

  boolean runTests(Token token) throws ReteException
  {
    int ntests = m_localTests.length;

    for (int i=0; i<ntests; i++) 
      {        
        // Test1
        // this is a function call! if it evals to Funcall.FALSE(),
        // the test failed; FALSE(), it failed.
        Test1 t;
        try
          {
            t = (Test1) m_localTests[i];
          }
        catch (ClassCastException bce)
          {
            // This will only happen if a subclass misbehaves.
            throw new ReteException("NodeTest:callNode",
                                    "Bad test type!",
                                    m_localTests[i].getClass().getName());
          }
        Value value = t.m_slotValue;
        switch (t.m_test) 
          {
          case Test1.EQ:
            {
              int markF=0, markV=0;
              try
                {
                  markF = m_cache.markFuncall();
                  markV = m_cache.markValue();
                  if (Funcall.execute(eval(value, token),
                                      m_engine.globalContext(),
                                      m_cache).equals(Funcall.FALSE()))
                    return false;
                }
              finally
                {
                  m_cache.restoreFuncall(markF);
                  m_cache.restoreValue(markV);
                }
            }
          break;
          case Test2.NEQ:
            {
              int markF=0, markV=0;
              try
                {
                  markF = m_cache.markFuncall();
                  markV = m_cache.markValue();
                  if (!Funcall.execute(eval(value, token),
                                       m_engine.globalContext(),
                                       m_cache).equals(Funcall.FALSE()))
                    return false;
                }
              finally
                {
                  m_cache.restoreFuncall(markF);
                  m_cache.restoreValue(markV);
                }
            }
          break;
          default:
            throw new ReteException("NodeTest::runTests",
                                    "Test type not supported",
                                    String.valueOf(t.m_test));
          }
      }

      return true;
      
  }

  /**
    Describe myself
    */
  
  public String toString() 
  {
    StringBuffer sb = new StringBuffer(256);
    sb.append("[NodeTest ntests=");
    sb.append(m_tests.size());
    sb.append(" ");
    for (int i=0; i<m_tests.size(); i++)
      {
        sb.append(m_tests.elementAt(i).toString());
        sb.append(" ");
      }
    sb.append(";usecount = ");
    sb.append(m_usecount);
    sb.append("]");
    return sb.toString();
  }

}





