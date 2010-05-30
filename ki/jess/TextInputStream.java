/** **********************************************************************
 * TextInputStream.java
 * A very simple input stream, something like StringBufferInputStream but
 * you can add text to it!
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.io.*;

public class TextInputStream extends InputStream
{
  private StringBuffer m_buf = new StringBuffer(256);
  private int m_ptr = 0;
  private DataInputStream m_source;
  private boolean m_markActive = false;
  private boolean m_dontWait = false;

  public TextInputStream() {}
  public TextInputStream(boolean dontWait) {m_dontWait = dontWait;}
  public TextInputStream(DataInputStream source) {m_source = source;}

  public synchronized int read() throws IOException
  {
    while (m_ptr >= m_buf.length())      
      {
        if (m_source != null)
          {
            String s = m_source.readLine();
            if (s == null)
              return -1;
            else
              {
                appendText(s);
                appendText("\n");
              }
          }
        else if (m_dontWait)
          {
            return -1;
          }
        else
          try { wait(100); } catch (InterruptedException ie) {}
      }
    
    int c = m_buf.charAt(m_ptr++);
        
    if (m_ptr >= m_buf.length())
      clear();
    
    return c;
  }
  
  public int available()
  {
    return m_buf.length() - m_ptr;
  }

  public synchronized void appendText(String s)
  {
    m_buf.append(s);
    notifyAll();
  }

  public synchronized void clear()
  {
    if (m_markActive)
      return;
    m_buf.setLength(0);
    m_ptr = 0;
  }

  int mark()
  { 
    m_markActive = true;
    return m_ptr;
  }

  boolean seenSince(int mark, char c)
  {    
    m_markActive = false;
    for (int i=mark; i < m_ptr; i++)
      if (m_buf.charAt(i) == c)
        return true;
    return false;
  }

}
