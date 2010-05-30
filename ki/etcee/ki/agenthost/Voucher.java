
package etcee.ki.agenthost;

import java.io.Serializable;

/**
 * The voucher.
 *
 * The <CODE>Voucher</CODE> class allows the remote agent host
 * to authenticate the steps taken by the local host.
 *
 */

public final class Voucher
  implements Serializable
{
  /**
   * The ID.
   *
   */

  private long nID = 0;

  /**
   * Constructs the voucher.
   *
   */

  public
  Voucher()
  {
    nID = Math.round(Math.random() * Long.MAX_VALUE);
  }

  /**
   * Constructs the voucher.
   *
   */

  public
  Voucher(Voucher voucher)
  {
    nID = voucher.nID;
  }

  /**
   * Tests two objects for equality.
   *
   */

  public boolean
  equals(Object obj)
  {
    try
    {
      Voucher voucher = (Voucher)obj;

      if (voucher == null)
      {
        return false;
      }

      if (nID == voucher.nID)
      {
        return true;
      }
    }
    catch (ClassCastException ccex)
    {
    }

    return false;
  }
}
