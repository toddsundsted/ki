package com.etcee.app.ki.agent;

import java.util.Enumeration;

/**
 * The agent context.
 *
 * The <CODE>AgentContext</CODE> class provides an agent with an
 * interface into the environment in which it lives.
 *
 * @see Agent.getAgentContext
 *
 */

abstract public class AgentContext
{
  /**
   * Gets the agent's agent identity.
   *
   */

  abstract public AgentIdentity
  getAgentIdentity();

  /**
   * Gets the agent's agent host name.
   *
   */

  abstract public String
  getAgentHostName();

  /**
   * Sends a message to the specified agent on the specified host.
   *
   * @parameter strAgentHostName may be null.
   *
   */

  abstract public MessageResponse
  sendMessage(String strAgentHostName,
              AgentIdentity agentidentity,
	      Message message);

  /**
   * Requests to transfer to the specified host.
   *
   */

  abstract public TransferResponse
  requestToTransfer(String strAgentHostName);

  /**
   * Publishes an agent.
   *
   * By publishing itself, an agent makes itself visible to other
   * agents in the same agent host.
   *
   * @parameter strIdentifier identifies the agent to other agents.
   *
   * @see unpublish
   *
   */

  abstract public void
  publish(String strIdentifier);

  /**
   * Unpublishes an agent.
   *
   * @see publish
   *
   */

  abstract public void
  unpublish();

  /**
   * Gets the published agents.
   *
   * This list may only be valid for the instant in time at which the
   * list was made.
   *
   * @see publish
   * @see unpublish
   *
   */

  abstract public Enumeration
  getPublishedAgents();

  /**
   * Gets the agent identity of a published agent.
   *
   * @see getPublishedAgents
   *
   */

  abstract public AgentIdentity
  getPublishedAgentIdentity(String strIdentifier);
}
