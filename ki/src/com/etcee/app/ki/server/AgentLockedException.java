package com.etcee.app.ki.server;

import com.etcee.app.ki.agent.AgentIdentity;

import com.etcee.app.ki.agenthost.AgentException;

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
