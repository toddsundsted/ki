
package etcee.ki.agenthost;

import etcee.ki.agent.AgentIdentity;

/**
 * The agent exception.
 *
 * The <CODE>AgentException</CODE> class is the supertype of all
 * agent exceptions.
 *
 */

public class AgentException
  extends Exception
{
  /**
   * The identity of the agent in question.
   *
   */

  private AgentIdentity agentidentity = null;

  /**
   * Constructs the agent exception.
   *
   */

  public
  AgentException(AgentIdentity agentidentity)
  {
    super();

    this.agentidentity = agentidentity;
  }

  /**
   * Constructs the agent exception.
   *
   */

  public
  AgentException(AgentIdentity agentidentity, String str)
  {
    super(str);

    this.agentidentity = agentidentity;
  }
}
