
package etcee.ki.server;

import etcee.ki.agent.AgentContext;

/**
 * The agent root.
 *
 * This class <EM>is</EM> thread safe.
 *
 */

public class AgentRoot
{
  /**
   * The agent context.
   *
   * The agent context provides the agent with an interface into the
   * environment in which it lives.
   *
   * @see AgentContext
   *
   */

  private transient AgentContext agentcontext = null;

  /**
   * Sets the agent's agent context.
   *
   */

  final void
  setAgentContext(AgentContext agentcontext)
  {
    this.agentcontext = agentcontext;
  }

  /**
   * Gets the agent's agent context.
   *
   */

  protected final AgentContext
  getAgentContext()
  {
    return agentcontext;
  }
}
