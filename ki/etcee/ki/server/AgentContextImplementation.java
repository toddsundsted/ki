
package etcee.ki.server;

import etcee.ki.agent.Agent;
import etcee.ki.agent.AgentContext;
import etcee.ki.agent.AgentIdentity;
import etcee.ki.agent.Message;
import etcee.ki.agent.MessageResponse;
import etcee.ki.agent.TransferResponse;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * The agent context implementation.
 *
 * This class <EM>is</EM> thread safe.
 *
 * @see AgentContext
 *
 */

public final class AgentContextImplementation
  extends AgentContext
{
  /**
   * The agent wrapper.
   *
   */

  private AgentWrapper agentwrapper = null;

  /**
   * Constructs the agent context implementation.
   *
   */

  AgentContextImplementation(AgentWrapper agentwrapper)
  {
    this.agentwrapper = agentwrapper;
  }

  /**
   * Gets the agent's agent identity.
   *
   * @see AgentContext.getAgentIdentity
   *
   */

  public final AgentIdentity
  getAgentIdentity()
  {
    return agentwrapper.getAgentIdentity();
  }

  /**
   * Gets the agent's agent host name.
   *
   * @see AgentContext.getAgentHostName
   *
   */

  public final String
  getAgentHostName()
  {
    return agentwrapper.getAgentHostImplementation().getAgentHostName();
  }

  /**
   * Sends a message to the specified agent.
   *
   * @see AgentContext.sendMessage
   *
   */

  public final MessageResponse
  sendMessage(String strAgentHostName,
              AgentIdentity agentidentity,
	      Message message)
  {
    if (agentwrapper.getState() != AgentWrapper.STARTING &&
        agentwrapper.getState() != AgentWrapper.STARTED)
    {
      throw new IllegalStateException();
    }

    return agentwrapper.getAgentHostImplementation().initiateSendMessage
      (strAgentHostName, agentidentity, message);
  }

  /**
   * Requests to transfer to the specified host.
   *
   */

  public final TransferResponse
  requestToTransfer(String strAgentHostName)
  {
    if (agentwrapper.getState() != AgentWrapper.STARTING &&
        agentwrapper.getState() != AgentWrapper.STARTED)
    {
      throw new IllegalStateException();
    }

    return agentwrapper.getAgentHostImplementation().initiateTransfer
      (strAgentHostName, getAgentIdentity());
  }                                                    

  /**
   * The published identifier.
   *
   */

  private String strIdentifier = null;

  /**
   * Publishes the agent.
   *
   * @see AgentContext.publish
   *
   */

  public final synchronized void
  publish(String strIdentifier)
  {
    if (agentwrapper.getState() != AgentWrapper.STARTING &&
        agentwrapper.getState() != AgentWrapper.STARTED)
    {
      throw new IllegalStateException();
    }

    agentwrapper.getServerData().hashtableDirectory.put(strIdentifier, getAgentIdentity());

    this.strIdentifier = strIdentifier;
  }

  /**
   * Unpublishes the agent.
   *
   * @see AgentContext.unpublish
   *
   */

  public final synchronized void
  unpublish()
  {
    if (agentwrapper.getState() != AgentWrapper.STARTING &&
        agentwrapper.getState() != AgentWrapper.STARTED)
    {
      throw new IllegalStateException();
    }

    agentwrapper.getServerData().hashtableDirectory.remove(strIdentifier);

    strIdentifier = null;
  }

  /**
   * Gets the published agents.
   *
   * @see AgentContext.getPublishedAgents
   *
   */

  public final Enumeration
  getPublishedAgents()
  {
    if (agentwrapper.getState() != AgentWrapper.STARTING &&
        agentwrapper.getState() != AgentWrapper.STARTED)
    {
      throw new IllegalStateException();
    }

    return agentwrapper.getServerData().hashtableDirectory.keys();
  }

  /**
   * Gets the agent identity of a published agent.
   *
   * @see AgentContext.getPublishedAgentIdentity
   *
   */

  public final AgentIdentity
  getPublishedAgentIdentity(String strIdentifier)
  {
    if (agentwrapper.getState() != AgentWrapper.STARTING &&
        agentwrapper.getState() != AgentWrapper.STARTED)
    {
      throw new IllegalStateException();
    }

    return (AgentIdentity)agentwrapper.getServerData().hashtableDirectory.get(strIdentifier);
  }
}
