/** **********************************************************************
 * A special type of exception for the expert system
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public class ReteException extends Exception
{

  String m_routine, m_text1, m_text2;

  /**
   * Constructor
   * @param Routine the routine that threw this exception
   * @param Text1 an informational message
   * @param Text2 usually some data    
   */

  public ReteException(String routine, String text1, String text2)
  {
    m_routine = routine;
    m_text1 = text1;
    m_text2 = text2;
  }

  
  /**
    How things get printed
    */

  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("Rete Exception in routine ");
    sb.append(m_routine);
    sb.append(".\n");
    sb.append("  Message: ");
    sb.append(m_text1);
    sb.append(" ");
    sb.append(m_text2);
    sb.append(".");
    return sb.toString();
  }


}
