
package etcee.ki.server;

import etcee.ki.agent.AgentIdentity;

import etcee.ki.agenthost.AgentException;

class AgentLockedException extends AgentException
{
  public AgentLockedException(AgentIdentity agentidentity)
  {
    super(agentidentity);
  }

  public AgentLockedException(AgentIdentity agentidentity, String str)
  {
    super(agentidentity, str);
  }
}
