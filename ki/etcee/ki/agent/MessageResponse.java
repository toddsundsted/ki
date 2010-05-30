
package etcee.ki.agent;

/**
 * The message response.
 *
 * The <CODE>MessageResponse</CODE> class defines a message response.
 * The <CODE>AgentHost</CODE> <CODE>sendMessage()</CODE> method
 * returns a message response object to its caller.  The message
 * response object allows the caller to check on the status of the
 * send and to obtain the ultimate return value.
 *
 * This class <EM>is</EM> thread safe.
 *
 * @see AgentContext.sendMessage
 * @see AgentHost.sendMessage
 *
 */

public final class MessageResponse
{
  /**
   * The response code.
   *
   * @see UNDEFINED
   * @see SUCCEEDED
   * @see FAILED
   *
   */

  private int nCode = UNDEFINED;

  /**
   * The result of the send is undefined.
   *
   */

  final static int UNDEFINED = -1;

  /**
   * The send succeeded.
   *
   */

  public final static int SUCCEEDED = 1;

  /**
   * The send failed.
   *
   */

  public final static int FAILED = 2;

  /**
   * Gets the response code.
   *
   */

  public final int
  getResponseCode()
  {
    return nCode;
  }

  /**
   * The exception.
   *
   * If an exception is thrown during the send, it is caught
   * and stored here.
   *
   */

  private Exception ex = null;

  /**
   * Gets the exception.
   *
   */

  public final Exception
  getException()
  {
    return ex;
  }

  /**
   * The response.
   *
   */

  private Object objResponse = null;

  /**
   * Gets the response.
   *
   */

  public final Object
  getResponse()
  {
    return objResponse;
  }

  /**
   * Called to indicate the send failed.
   *
   * The agent host calls this method to indicate the send failed.
   *
   */

  public synchronized final void
  setFailed(Exception ex)
  {
    nCode = FAILED;

    this.ex = ex;

    this.objResponse = null;

    notifyAll();
  }

  /**
   * Called to indicate the send succeeded.
   *
   * The agent host calls this method to indicate the send succeeded.
   *
   */

  public synchronized final void
  setSucceeded(Object objResponse)
  {
    nCode = SUCCEEDED;

    this.ex = null;

    this.objResponse = objResponse;

    notifyAll();
  }

  /**
   * Waits for the response to become valid.
   *
   */

  public synchronized final void
  waitForResponse()
  {
    while (nCode == UNDEFINED)
    {
      try
      {
        wait();
      }
      catch (InterruptedException ex)
      {
      }
    }
  }

  /**
   * Waits for the response to become valid and returns whether or not
   * the operation was successful.
   *
   */

  public boolean
  wasSuccessful()
  {
    waitForResponse();

    return nCode == SUCCEEDED;
  }
}
