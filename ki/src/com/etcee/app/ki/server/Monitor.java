package com.etcee.app.ki.server;

/**
 * The monitor.
 *
 */

public final class Monitor
{
  /**
   * The lock flag.
   *
   */

  private boolean boolLocked = false;

  /**
   * Sets the lock flag.
   *
   */

  public final void
  setLockFlag(boolean boolLocked)
  {
    this.boolLocked = boolLocked;
  }

  /**
   * Gets the lock flag.
   *
   */

  public final boolean
  getLockFlag()
  {
    return boolLocked;
  }

  /**
   * The operation counter.
   *
   * The operation counter dynamically tracks the number of operations
   * the client is currently handling.
   *
   */

  private int nOperations = 0;

  /**
   * Locks the client and waits.
   *
   * Locks the client and then waits for it to finish handling any
   * unfinished operations.
   *
   */

  public final synchronized void
  lockAndWait()
    throws InterruptedException
  {
    boolLocked = true;

    // Since boolLocked == true, the number of unfinished operations
    // should only decrease.

    while (nOperations > 0)
    {
      wait();
    }
  }

  /**
   * Begins an operation.
   *
   * @returns false if the monitor is locked
   *
   */

  public final synchronized boolean
  beginOperation()
  {
    if (boolLocked)
    {
      return false;
    }

    nOperations++;

    return true;
  }

  /**
   * Ends an operation.
   *
   */

  public final synchronized void
  endOperation()
  {
    nOperations--;

    notify();
  }
}
