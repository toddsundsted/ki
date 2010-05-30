/** **********************************************************************
 * Two-input nodes of the Rete network
 * Test that a slot from the left input and one
 * from the right input have the same
 * value and type.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class Node2 extends NodeTest
{
  
  /**
    The left and right token memories
    They are Vectors of Tokens
    */

  TokenTree m_left, m_right;
  
  /**
    Constructor
    */

  Node2(Rete engine) 
  {
    super(engine);
    m_left = new TokenTree();
    m_right = new TokenTree();
  }

  void addTest(int test, int token_idx, int left_idx, int right_idx) 
  {
    m_tests.addElement(new Test2(test, token_idx, left_idx, -1,
                                 right_idx, -1));
  }

  void addTest(int test, int token_idx, int left_idx, int leftSub_idx,
               int right_idx, int rightSub_idx) {
    Test2 t;
    m_tests.addElement(t = new Test2(test, token_idx, left_idx, 
                                     leftSub_idx, right_idx, rightSub_idx));

  }


  /**
    Do the business of this node.
    The 2-input nodes, on receiving a token, have to do several things,
    and their actions change based on whether it's an ADD or REMOVE,
    and whether it's the right or left input!
    <PRE>

    *********** For ADDs, left input:
    1) Look for this token in the left memory. If it's there, do nothing;
    If it's not, add it to the left memory.

    2) Perform all this node's tests on this token and each of the right-
    memory tokens. For any right token for which they succeed:

    3) a) append the right token to a copy of this token. b) do a
    CallNode on each of the successors using this new token.

    *********** For ADDs, right input:

    1) Look for this token in the right memory. If it's there, do nothing;
    If it's not, add it to the right memory.

    2) Perform all this node's tests on this token and each of the left-
    memory tokens. For any left token for which they succeed:

    3) a) append this  token to a copy of the left token. b) do a
    CallNode on each of the successors using this new token.

    *********** For REMOVEs, left input:
    
    1) Look for this token in the left memory. If it's there, remove it;
    else do nothing.

    2) Perform all this node's tests on this token and each of the right-
    memory tokens. For any right token for which they succeed:
    
    3) a) append the right token to a copy of this token. b) do a
    CallNode on each of the successors using this new token.

    *********** For REMOVEs, right input:
    
    1) Look for this token in the right memory. If it's there, remove it;
    else do nothing.

    2) Perform all this node's tests on this token and each of the left-
    memory tokens. For any left token for which they succeed:
    
    3) a) append this token to a copy of the left token. b) do a
    CallNode on each of the successors using this new token.

    </PRE>
    */
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    // This is a special case. If we get a 'clear', we flush our memories,
    // then notify all our successors and return.
    
    if (token.m_tag == RU.CLEAR)
      {
        m_left = new TokenTree();
        m_right = new TokenTree();
        passAlong(token);
        return false;
      }

    // the four cases listed above are now implemented in order.

    switch (callType) 
      {

      case Node.LEFT:
        switch (token.m_tag) 
          {

          case RU.ADD:
          case RU.UPDATE:
            m_left.add(token);
            runTestsVaryRight(token, m_right.m_root);
            break;

          case RU.REMOVE:
            m_left.remove(token);
            runTestsVaryRight(token, m_right.m_root);
            break;

          default:
            throw new ReteException("Node2::callNode",
                                    "Bad tag in token",
                                    String.valueOf(token.m_tag));
          } // switch token.tag
        break; // case Node.LEFT;


      case Node.RIGHT:
        switch (token.m_tag) 
          {

          case RU.UPDATE:
          case RU.ADD:
            m_right.add(token);
            runTestsVaryLeft(token, m_left.m_root);
            break;

          case RU.REMOVE:
            m_right.remove(token);
            runTestsVaryLeft(token, m_left.m_root);
            break;

          default:
            throw new ReteException("Node2::callNode",
                                    "Bad tag in token",
                                    String.valueOf(token.m_tag));
          } // switch token.tag
        break; // case Node.RIGHT

      default:
        throw new ReteException("Node2::callNode",
                                "Bad callType",
                                String.valueOf(callType));

      } // switch callType

    // the runTestsVary* routines pass messages on to the successors.

    return true;
  }

  /**
    Node2.callNode can call this to produce debug info.
    */

  void debugPrint(Token token, int callType) throws ReteException 
  {
    System.out.println("TEST " + toString() + ";calltype=" + callType
                       + ";tag=" + token.m_tag + ";class=" +
                       token.fact(0).get(RU.CLASS).stringValue());
  }

  /**
    Run all the tests on a given (left) token and every token in the
    right memory. For the true ones, assemble a composite token and
    pass it along to the successors.
    */

  void runTestsVaryRight(Token lt, TokenHolder th) throws ReteException 
  {
    if (th == null)
      return;
    TokenHolder thl, thr;
    if ((thl = th.m_left) != null) runTestsVaryRight(lt, thl);
    if ((thr = th.m_right) != null) runTestsVaryRight(lt, thr);

    Token rt = th.m_token;

    while (rt != null)
      {
        Token nt = new Token(lt,rt.fact(0));
        if (runTests(lt, rt, nt)) 
          {
            passAlong(nt);
          }
        rt = rt.m_next;
      }
  }
    
  /**
    Run all the tests on a given (right) token and every token in the
    left memory. For the true ones, assemble a composite token and
    pass it along to the successors.
    */

  void runTestsVaryLeft(Token rt, TokenHolder th) throws ReteException 
  {
    if (th == null)
      return;

    if (th.m_left != null) runTestsVaryLeft(rt, th.m_left);
    if (th.m_right != null) runTestsVaryLeft(rt, th.m_right);

    Token lt = th.m_token;

    while (lt != null)
      {
        Token nt = new Token(lt,rt.fact(0));
        if (runTests(lt, rt, nt)) 
          {
            // the new token has the *left* token's tag at birth...
            nt.m_tag = rt.m_tag;
            passAlong(nt);
          }

        lt = lt.m_next;
      }
  }


  /**
    Run all this node's tests on two tokens.  This routine assumes
    that the right token has only one fact in it, which is true at this
    time!
    */

  boolean runTests(Token lt, Token rt, Token token) throws ReteException 
  {
    int ntests = m_localTests.length;

    ValueVector rf = rt.fact(0);
    EvalCache cache = m_cache;

    for (int i=0; i < ntests; i++) 
      {
        Object gentest = m_localTests[i];

        if (gentest instanceof Test2) 
          {
            Test2 t = (Test2) gentest;
            ValueVector lf = lt.fact(t.m_tokenIdx);

            if (lf == null)
              continue;

            int lsubidx = t.m_leftSubIdx;
            int rsubidx = t.m_rightSubIdx;

            Value v1, v2;
            ValueVector slot;
            
            if (lsubidx != -1)  // i.e., first variable is in a multislot
              v1 = lf.get(t.m_leftIdx).listValue().get(lsubidx);
            else 
              v1 = lf.get(t.m_leftIdx);
            
            if (rsubidx != -1)  // i.e., first variable is in a multislot
              v2 = rf.get(t.m_rightIdx).listValue().get(rsubidx);
            else 
              v2 = rf.get(t.m_rightIdx);
            
            boolean retval = v1.equals(v2);
            int test = t.m_test;
            if (test == Test2.EQ)
              {
                if (!retval) return false;
              }
            else if (test == Test2.NEQ)
              {
                if (retval) return false;
              }
            else 
              throw new ReteException("Node2::runTests",
                                      "Test2 type not supported",
                                      String.valueOf(t.m_test));
          } 
        else 
          { // Test1
            // this is a function call! if it evals to Funcall.FALSE(),
            // the test failed; FALSE(), it failed.
            Test1 t = (Test1) gentest;
            Value value = t.m_slotValue;
            switch (t.m_test) 
              {
              case Test1.EQ:
                {
                  int markF=0, markV=0;
                  try
                    {
                      markF = cache.markFuncall();
                      markV = cache.markValue();
                      if (Funcall.execute(eval(value, token),
                                          m_engine.globalContext(),
                                          m_cache).equals(Funcall.FALSE()))
                        return false;
                    }
                  finally
                    {
                      cache.restoreFuncall(markF);
                      cache.restoreValue(markV);
                    }
                }
                break;
              case Test2.NEQ:
                {
                  int markF=0, markV=0;
                  try
                    {
                      markF = cache.markFuncall();
                      markV = cache.markValue();
                      if (!Funcall.execute(eval(value, token),
                                           m_engine.globalContext(),
                                           m_cache).equals(Funcall.FALSE()))
                        return false;
                    }
                  finally
                    {
                      cache.restoreFuncall(markF);
                      cache.restoreValue(markV);
                    }
                }
                break;
              default:
                throw new ReteException("Node2::runTests",
                                        "Test1 type not supported",
                                        String.valueOf(t.m_test));
              }
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
    sb.append("[Node2 ntests=");
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


  public String displayMemory()
  {
    StringBuffer sb = new StringBuffer(256);
    sb.append("\n\nLeft Memory:\n\n");
    int count = showMemory(m_left.m_root, sb);
    sb.append ("\n");
    sb.append(count);
    sb.append(" entries");
    sb.append("\n\nRight Memory:\n\n");
    count = showMemory(m_right.m_root, sb);
    sb.append ("\n");
    sb.append(count);
    sb.append(" entries");
    return sb.toString();
  }

  private int showMemory(TokenHolder th, StringBuffer sb)
  {
    if (th == null)
      return 0;
    int count = 0;

    count += showMemory(th.m_left, sb);

    if (th.m_token != null)
        {
          Token t = th.m_token;
          while (t != null)
            {
              ++count;
              sb.append(t);
              sb.append('\n');
              t = t.m_next;
            }
        }

    count += showMemory(th.m_right, sb);
    return count;
  }
}





