/** **********************************************************************
 * A Binary Tree of Tokens kept by sortcode
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

class TokenTree
{
  TokenHolder m_root = new TokenHolder(1000);

  synchronized void add(Token t)
  {
    TokenHolder th = findCodeInTree(t, true);
    if (th.m_token == null)
      {
        th.m_token = t;
        return;
      }
    else
      {
        Token tt = th.m_token;
        if (tt.dataEquals(t))
          {
            return;
          }
        
        while (tt.m_next != null)
          {
            tt = tt.m_next;
            if (tt.dataEquals(t))
              return;
          }
        
        tt.m_next = t;
        return;
      }
  }

  synchronized void remove(Token t)
  {
    TokenHolder th = findCodeInTree(t, false);
    if (th == null || th.m_token == null)
      return;

    else
      {
        Token tt = th.m_token;
        if (tt.dataEquals(t))
          {
            // splice out token
            th.m_token = tt.m_next;
            return;
          }
        while (tt.m_next != null)
          {
            Token last = tt;
            tt = tt.m_next;
            if (tt.dataEquals(t))
              {
                last.m_next = tt.m_next;
                return;
              }
          }
        return;
      }
  }

  private TokenHolder findCodeInTree(Token t, boolean create)
  {
    int code = t.m_sortcode % 101;
    TokenHolder th = m_root;
    int othercode = th.m_code;
    while (othercode != code)
      {
        if (code < othercode)
          {
            if (th.m_left == null)
              {
                if (create)
                  return th.m_left = new TokenHolder(code);
                else
                  return null;
              }

            else
              {
                th = th.m_left;
                othercode = th.m_code;
              }
          }
        else if (code > othercode)
          {
            if (th.m_right == null)
              {
                if (create)
                  return th.m_right = new TokenHolder(code);
                else
                  return null;
              }
            else
              {
                th = th.m_right;
                othercode = th.m_code;
              }
          }
      }
    return th;
  }



}

