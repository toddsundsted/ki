/** **********************************************************************
 * Specialized two-input nodes for negated patterns
 *
 * NOTE: CLIPS behaves in a surprising way which I'm following here.
 * Given this defrule:
 * <PRE>
 * (defrule test-1
 *  (bar)       
 *  (not (foo))
 *  =>
 *  (printout t "not foo"))
 *  
 * CLIPS behaves this way:
 * 
 * (watch activations)
 * (assert (bar))
 * ==> Activation 0 test-1
 * (assert (foo))
 * <== Activation 0 test-1
 * (retract (foo))
 * ==> Activation 0 test-1
 * 
 * This is not surprising yet. Here's the funky part
 * 
 * (run)
 * "not foo"
 * (assert (foo))
 * (retract (foo))
 * ==> Activation 0 test-1
 *
 * The rule fires,  and all that's required to fire it again is for the
 * "not-ness" to be removed and replaced; the (bar) fact does not need to
 * be replaced. This obviously falls out of the implementation; it makes things
 * easy!
 *
 * </PRE>
*
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public class NodeNot2 extends Node2 
{
  private static Token s_nullToken;
  static
  {
    try
      {
        ValueVector nullFact = new ValueVector();
        nullFact.set(new Value("$NOT-CE$", RU.ATOM), RU.CLASS);
        nullFact.set(new Value(RU.ORDERED_FACT, RU.DESCRIPTOR), RU.DESC);
        nullFact.set(new Value(-1, RU.FACT_ID), RU.ID);
        s_nullToken = new Token(RU.ADD, nullFact);
      }
    catch (ReteException rex) {}
  }

  NodeNot2(Rete engine)
  {
    super(engine);
  }

  /**
    We're going to modify the token we're handed, so
    we need to make a copy. Probably should only make the copy when we're
    going to keep the token; I'll revisit this later.
    */


  boolean callNode(Token token, int callType) throws ReteException
  {
    Token nt = new Token(token);
    return super.callNode(nt, callType);
  }

  /**
    Run all the tests on a given (left) token and every token in the
    right memory. Every time a right token *passes* the tests, increment 
    the left token's negation count; at the end, if the
    left token has a zero count, pass it through.

    The 'nullToken' contains a fact used as a placeholder for the 'not' CE.
    */

  void runTestsVaryRight(Token lt, TokenHolder th) throws ReteException 
  {
    doRunTestsVaryRight(lt, th);
    
    if (lt.m_negcnt == 0)
      {
        Token nt = new Token(lt, s_nullToken.fact(0));
        passAlong(nt);
      }
  }

  private void doRunTestsVaryRight(Token lt, TokenHolder th)
       throws ReteException
  {
    if (th == null)
      return;
    
    doRunTestsVaryRight(lt, th.m_left);
    doRunTestsVaryRight(lt, th.m_right);

    Token rt = th.m_token;

    while (rt != null)
      {
        Token nt = new Token(lt,rt.fact(0));
        if (runTests(lt, rt, nt)) 
          lt.m_negcnt++;
        rt = rt.m_next;
      }

  }
  
  /**
    Run all the tests on a given (right) token and every token in the
    left memory. For the true ones, increment (or decrement) the appropriate
    negation counts. Any left token which transitions to zero gets passed
    along.
    */
  void runTestsVaryLeft(Token rt, TokenHolder th) throws ReteException 
  {
    if (th == null)
      return;

    runTestsVaryLeft(rt, th.m_left);
    runTestsVaryLeft(rt, th.m_right);

    Token lt = th.m_token;

    while (lt != null)
      {
        Token nt = new Token(lt,rt.fact(0));
        if (runTests(lt, rt, nt)) {
          if (rt.m_tag == RU.ADD || rt.m_tag == RU.UPDATE) 
            {
              // retract any activation due to the left token
              Token nt2 = new Token(lt, s_nullToken.fact(0));
              nt2.m_tag = RU.REMOVE;
              passAlong(nt2);
              lt.m_negcnt++;
            } 
          else if (--lt.m_negcnt == 0) 
            {
              // pass along the revitalized left token
              Token nt2 = new Token(lt, s_nullToken.fact(0)); 
              passAlong(nt2);
            }
          else if (lt.m_negcnt < 0)
            throw new ReteException("NodeNot2::RunTestsVaryLeft",
                                    "Corrupted Negcnt (< 0)",
                                    "");
        }
        lt = lt.m_next;
      }
  }

  /**
    Describe myself
    */
  
  public String toString() 
  {
    StringBuffer sb = new StringBuffer(256);
    sb.append("[NodeNot2 ntests=");
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
