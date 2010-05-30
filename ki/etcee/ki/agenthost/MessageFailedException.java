
package etcee.ki.agenthost;

/**
 * The message failed exception.
 *
 * An instance of class <CODE>MessageFailedException</CODE> is thrown
 * whenever the message transfer between two hosts fails for some
 * reason.
 *
 */

public class MessageFailedException
  extends Exception
{
  /**
   * Constructs the exception.
   *
   */

  public
  MessageFailedException()
  {
    super();
  }

  /**
   * Constructs the exception.
   *
   */

  public
  MessageFailedException(String str)
  {
    super(str);
  }
}
