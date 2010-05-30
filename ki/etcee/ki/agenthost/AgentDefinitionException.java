
package etcee.ki.agenthost;

import etcee.ki.agent.AgentIdentity;

/**
 * The agent definition exception.
 *
 */

public class AgentDefinitionException extends AgentException
{
  /**
   * Constructs the exception.
   *
   */

  public
  AgentDefinitionException(AgentIdentity agentidentity)
  {
    super(agentidentity);
  }

  /**
   * Constructs the exception.
   *
   */

  public
  AgentDefinitionException(AgentIdentity agentidentity, String str)
  {
    super(agentidentity, str);
  }
}
