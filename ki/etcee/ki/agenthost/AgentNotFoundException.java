
package etcee.ki.agenthost;

import etcee.ki.agent.AgentIdentity;

/**
 * The agent not found exception.
 *
 */

public class AgentNotFoundException
  extends AgentException
{
  /**
   * Constructs the exception.
   *
   */

  public
  AgentNotFoundException(AgentIdentity agentidentity)
  {
    super(agentidentity);
  }

  /**
   * Constructs the exception.
   *
   */

  public
  AgentNotFoundException(AgentIdentity agentidentity, String str)
  {
    super(agentidentity, str);
  }
}
