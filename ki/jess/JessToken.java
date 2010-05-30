/** **********************************************************************
 * A packet of info  about a token in the input stream.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.io.*;

final class JessToken
{
  String m_sval;
  double m_nval;
  int m_lineno;
  int m_ttype;

  static final JessToken create(StreamTokenizer st,
                                TextInputStream tis) 
  {
    try 
      {
        int mark = tis.mark();
        st.nextToken();
         
        JessToken tok = new JessToken();
        switch (st.ttype) 
          {
          case st.TT_WORD: tok.m_ttype   = RU.ATOM;    break;
          case st.TT_NUMBER: 
            {
              if (tis.seenSince(mark, '.'))
                tok.m_ttype = RU.FLOAT;
              else
                tok.m_ttype = RU.INTEGER;
              break;
            }
          case '"': tok.m_ttype          = RU.STRING;  break;
          case st.TT_EOF:
          case 0:
            tok.m_ttype          = RU.NONE;    break;
          default:  tok.m_ttype          = st.ttype;   break;
          }
        
        tok.m_sval = st.sval;
        tok.m_nval = st.nval;
        tok.m_lineno = st.lineno();
        
        // Change the ttype for a few special types
        if (tok.m_ttype == RU.ATOM)
          if (tok.m_sval.charAt(0) == '?') 
            {
              tok.m_ttype = RU.VARIABLE;
              if (tok.m_sval.length() > 1)
                tok.m_sval = tok.m_sval.substring(1);
              else
                tok.m_sval = RU.getAtom(RU.gensym("_blank_"));
              
              
            }
          else if (tok.m_sval.charAt(0) == '$' && tok.m_sval.charAt(1) == '?') 
            {
              tok.m_ttype = RU.MULTIVARIABLE;
              if (tok.m_sval.length() > 2)
                tok.m_sval = tok.m_sval.substring(2);
              else
                tok.m_sval = RU.getAtom(RU.gensym("_blank$_"));
              
            }
          else if (tok.m_sval.equals("=")) 
            {
              tok.m_ttype = '=';
            }
        
        return tok;
      }
    catch (IOException ioe) 
      {
        return null;
      }
  }

  Value tokenToValue() throws ReteException
  {
    // Turn the token into a value.
    switch (m_ttype) 
      {
      case RU.ATOM:
        return new Value(m_sval, RU.ATOM);
      case RU.FLOAT:
        return new Value(m_nval, RU.FLOAT);
      case RU.INTEGER:
        return new Value(m_nval, RU.INTEGER);
      case RU.STRING:
        return new Value(m_sval, RU.STRING);
      default:
        return new Value(RU.putAtom("" + (char) m_ttype), RU.STRING);
      }    
  }

  boolean isBlankVariable()
  {
    if (m_sval != null && m_sval.startsWith("_blank"))
      return true;
    else
      return false;
  }

  public String toString() 
  {
    if (m_ttype == RU.VARIABLE)
      return "?" + m_sval;
    else if (m_ttype == RU.MULTIVARIABLE)
      return "$?" + m_sval;
    else if (m_ttype == RU.STRING)
      return "\"" + m_sval + "\"";
    else if (m_sval != null)
      return m_sval;
    else if (m_ttype == RU.FLOAT)
      return "" + m_nval;
    else if (m_ttype == RU.INTEGER)
      return "" + (int) m_nval;
    else return "" +  (char) m_ttype;
  }

}
  



