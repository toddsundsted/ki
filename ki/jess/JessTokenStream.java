/** **********************************************************************
 * A smart lexer for the Jess subset of CLIPS
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.io.*;
import java.util.*;

class JessTokenStream 
{
  private Stack m_stack;
  private StreamTokenizer m_stream;
  private TextInputStream m_source;
  private int m_lineno = 1;
  private StringBuffer m_string = new StringBuffer();
  private Value m_loner = null;

  /**
    Construct a JessTokenStream.
    Tell the tokenizer how to separate Jess tokens
    */

  JessTokenStream(DataInputStream dis)
  {    
    m_source = new TextInputStream(dis);
    m_stream = prepareStream(m_source);
    m_stack = new Stack();
  }

  private StreamTokenizer prepareStream(InputStream is) 
  {
    StreamTokenizer stream = new StreamTokenizer(is);
    
    stream.commentChar(';');
    stream.wordChars('$','$');
    stream.wordChars('*','*');
    stream.wordChars('=','=');
    stream.wordChars('+','+');
    stream.wordChars('/','/');
    stream.wordChars('<','<');
    stream.wordChars('>','>');
    stream.wordChars('_','_');
    stream.wordChars('?','?');
    stream.wordChars('\'','\'');
    // Pound sign added at the request of Karl.R.Mueller@gsfc.nasa.gov
    stream.wordChars('#','#');  
    stream.wordChars('!','!');  
    stream.wordChars('@','@');  
    stream.parseNumbers();
    return stream;
  }

  /**
    Return the current line number, corresponding to the most recently
    popped or pushed token.
    */
  
  int lineno() 
  {
    return m_lineno;
  }

  /**
    Are there any more tokens in this sexp?
    */
  
  boolean moreTokens() 
  {
    return m_stack.empty();
  }

  /**
    Are there any more tokens in this sexp?
    */
  
  boolean eof() throws ReteException
  {
    if (m_stack.empty())
      if (!prepareSexp()) 
        {
          return true;
        }
    return false;
  }

  /**
    Retrive any lone token
    */

  Value loner()
  {
    return m_loner;
  }


  /**
    Load a full sexp into the stack.
    */

  boolean prepareSexp() throws ReteException
  {
    int level = 1;
    m_string.setLength(0);

    JessToken tok = JessToken.create(m_stream, m_source);
    if (tok.m_ttype != '(') 
      {
        if (tok.m_ttype != RU.NONE)
          m_loner = tok.tokenToValue();
        return false;
      }
    else
      m_loner = null;
    
    Stack temp_stack = new Stack();
    temp_stack.push(tok);
    while (level > 0) 
      {
        tok = JessToken.create(m_stream, m_source);
        temp_stack.push(tok);
        if (tok.m_ttype == RU.NONE)
          return false; 
        else if (tok.m_ttype == ')')
          --level;
        else if (tok.m_ttype == '(')
          ++level;
      }
    
    while (!temp_stack.empty()) 
      {
        m_stack.push(temp_stack.pop());
      }
    m_source.clear();
    return true;
  }

  /**
    Return the next token in the stream, or NULL if empty.
    */

  JessToken nextToken() throws ReteException
  {
    if (m_stack.empty())
      if (!prepareSexp()) 
        {
          return null;
        }
    JessToken tok = (JessToken) m_stack.pop();
    m_string.append(tok.toString() + " ");
    m_lineno = tok.m_lineno;
    return tok;
  }

  /**
    Infinite pushback
    */

  void pushBack(JessToken tok) 
  {
    m_lineno = tok.m_lineno;
    m_stack.push(tok);
    m_string.setLength(m_string.length() - (tok.toString().length() + 1));
  }

  /**
    Return the 'car' of a sexp as a String, or null.
    */

  String head() throws ReteException
  {
    if (m_stack.empty())
      if (!prepareSexp())
        return null;

    JessToken top = (JessToken) m_stack.pop();
    JessToken tok = (JessToken) m_stack.peek();

    m_stack.push(top);

    if (tok.m_ttype != RU.ATOM)      
      {
        if (tok.m_ttype == '-')
          return "-";
        else if (tok.m_ttype == '=')
          return "=";
        // This is allowed so we can use shorthand 'JAVACALL' syntax
        else if (tok.m_ttype == RU.VARIABLE)
          return tok.m_sval;
        else
          return tok.toString();
      }
    else
      {
        if (tok.m_sval != null)
          return tok.m_sval;
        else
          return tok.toString();
      }
  }

  void clear() 
  {
    m_stack = new Stack();
    m_string.setLength(0);
    m_loner = null;
  }
  // Print the sexp on the stack
  public String toString() 
  {
    return m_string.toString();
  }
  
  // for internal use by _read
  JessToken getOneToken()
  {
    JessToken jt = JessToken.create(m_stream, m_source);
    return jt;
  }

}












