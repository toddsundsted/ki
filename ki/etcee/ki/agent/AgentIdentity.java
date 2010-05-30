
package etcee.ki.agent;

import java.security.SecureRandom;

import java.io.Serializable;

/**
 * The agent identity.
 *
 * The <CODE>AgentIdentity</CODE> class identifies agents.
 *
 * This class <EM>is</EM> thread safe.
 *
 */

public final class AgentIdentity
  implements Serializable
{
  /**
   * The agent host name.
   *
   * The agent host name field holds the name of the last known agent
   * host that hosted the agent.  It is in the form of a URL:
   *
   * <CODE>protocol://host:port/name</CODE>
   *
   */

  private String strAgentHostName = null;

  /**
   * The random number generator.
   *
   */

  private final static SecureRandom securerandom = new SecureRandom();

  /**
   * The length (in bytes) of rgbID.
   *
   */

  private final static int nL = 128;

  /**
   * The ID array.
   *
   */

  private byte [] rgbID = new byte [nL];

  /**
   * Constructs the agent identity.
   *
   * The agent host name is set separately.
   *
   */

  public
  AgentIdentity()
  {
    // Agent identities in which the topmost four bytes of rgbID are
    // all zero are reserved for "well known" agent identities.  Such
    // arrays must not be generated randomly.

    int n = 0;

    while (n == 0)
    {
      securerandom.nextBytes(rgbID);

      n = rgbID[nL - 1] | rgbID[nL - 2] | rgbID[nL - 3] | rgbID[nL - 4];
    }
  }

  /**
   * Constructs the agent identity.
   *
   */

  public
  AgentIdentity(AgentIdentity agentidentity)
  {
    strAgentHostName = agentidentity.strAgentHostName;

    System.arraycopy(agentidentity.rgbID, 0, rgbID, 0, rgbID.length);
  }

  /**
   * Sets the agent host name.
   *
   */

  public final void
  setAgentHostName(String strAgentHostName)
  {
    this.strAgentHostName = strAgentHostName;
  }

  /**
   * Gets the agent host name.
   *
   */

  public final String
  getAgentHostName()
  {
    return strAgentHostName;
  }

  /**
   * Tests two objects for equality.
   *
   */

  public final boolean
  equals(Object obj)
  {
    try
    {
      AgentIdentity agentidentity = (AgentIdentity)obj;

      if (agentidentity == null)
      {
        return false;
      }

      if (this == agentidentity)
      {
        return true;
      }

      for (int i = 0; i < rgbID.length; i++)
      {
        if (rgbID[i] != agentidentity.rgbID[i])
        {
          return false;
        }
      }
    }
    catch (ClassCastException ex)
    {
      return false;
    }

    return true;
  }

  /**
   * Returns the hashcode.
   *
   */

  public final int
  hashCode()
  {
    // The hashcode is built from the lowest four bytes in rgbID.

    return (rgbID[0] & 0xFF) +
          ((rgbID[1] & 0xFF) << 8) +
          ((rgbID[2] & 0xFF) << 16) +
          ((rgbID[3] & 0xFF) << 24);
  }
}
