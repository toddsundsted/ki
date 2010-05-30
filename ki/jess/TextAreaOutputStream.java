/** **********************************************************************
 * TextAreaOutputStream: a simple output stream, suitable for constructing
 * a PrintStream, which uses a TextArea as its output.
 *
 * Simple, but nice!
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
*/

package jess;

import java.io.*;
import java.awt.*;

public class TextAreaOutputStream extends OutputStream
{

  StringBuffer m_str;
  TextArea m_ta;

  /**
    Call this with an already constructed TextArea
    object, which you can put wherever you'd like.
   */

  public TextAreaOutputStream(TextArea area)
  {
    m_str = new StringBuffer(100);
    m_ta = area;
  }

  /**
    nothing to close, really.
    */

  public void close() throws IOException
  {
  }

  public void flush() throws IOException
  {
    m_ta.appendText(m_str.toString());
    m_str.setLength(0);
  }

  public void write(int b) throws IOException
  {
    m_str.append((char) b);
  }
  
  public void write(byte b[]) throws IOException
  {
    for (int i=0; i< b.length; i++)
      m_str.append((char) b[i]);
  }
  
  public void write(byte b[], int off, int len) throws IOException 
  {
    for (int i=off; i< len; i++)
      m_str.append((char) b[i]);
  }

}
