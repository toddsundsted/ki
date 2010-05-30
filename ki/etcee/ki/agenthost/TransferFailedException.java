
package etcee.ki.agenthost;

/**
 * The transfer failed exception.
 *
 * An instance of class <CODE>TransferFailedException</CODE> is thrown
 * whenever the agent transfer between two hosts fails for some
 * reason.
 *
 */

public class TransferFailedException
  extends Exception
{
  /**
   * Constructs the exception.
   *
   */

  public
  TransferFailedException()
  {
    super();
  }

  /**
   * Constructs the exception.
   *
   */

  public
  TransferFailedException(String str)
  {
    super(str);
  }
}
