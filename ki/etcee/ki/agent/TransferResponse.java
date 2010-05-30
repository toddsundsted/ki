
package etcee.ki.agent;

/**
 * The transfer response.
 *
 * The <CODE>TransferResponse</CODE> class defines a transfer response.
 * The <CODE>AgentHost</CODE> <CODE>requestToTransfer()</CODE> method
 * returns a transfer response object to its caller.  The transfer
 * response object allows the caller to check on the status of the
 * transfer.
 *
 * This class <EM>is</EM> thread safe.
 *
 * @see AgentContext.requestToTransfer
 * @see AgentHost.requestToTransfer
 *
 */

public final class TransferResponse
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
   * The result of the transfer is undefined.
   *
   */

  final static int UNDEFINED = -1;

  /**
   * The transfer succeeded.
   *
   */

  public final static int SUCCEEDED = 1;

  /**
   * The transfer failed.
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
   * If an exception is thrown during the transfer, it is caught
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
   * Called to indicate the transfer failed.
   *
   * The agent host calls this method to indicate the transfer failed.
   *
   */

  public synchronized final void
  setFailed(Exception ex)
  {
    nCode = FAILED;

    this.ex = ex;

    notifyAll();
  }

  /**
   * Called to indicate the transfer succeeded.
   *
   * The agent host calls this method to indicate the transfer succeeded.
   *
   */

  public synchronized final void
  setSucceeded()
  {
    nCode = SUCCEEDED;

    this.ex = null;

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
