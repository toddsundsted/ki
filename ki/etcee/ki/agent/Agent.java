
package etcee.ki.agent;

import etcee.ki.server.AgentRoot;

import java.io.Serializable;

/**
 * The agent.
 *
 * The <CODE>Agent</CODE> class defines basic agent functionality.
 *
 * This class <EM>is</EM> thread safe.
 *
 * @see AgentRoot
 *
 */

abstract public class Agent
  extends AgentRoot
  implements Serializable
{
  /**
   * The target.
   *
   * The target defines the agent's behavior.  In many cases the
   * target is the agent itself, but it may refer to an instance of
   * some other class.
   *
   */

  private Object objTarget = null;

  /**
   * Gets the agent's target.
   *
   */

  public final Object
  getTarget()
  {
    return objTarget;
  }

  /**
   * Constructs the agent.
   *
   */

  protected
  Agent()
  {
    objTarget = this;
  }

  /**
   * Constructs the agent.
   *
   */

  protected
  Agent(Object objTarget)
  {
    this.objTarget = objTarget;
  }

  /**
   * Instructs the agent to initialize.
   *
   * The agent host calls <CODE>initialize()</CODE> exactly once --
   * after the agent is created.
   *
   * @see conclude
   *
   */

  abstract public void
  initialize();

  /**
   * Instructs the agent to start.
   *
   * The agent host calls <CODE>start()</CODE> whenever the agent must
   * be started or restarted.
   *
   * @see stop
   *
   */

  abstract public void
  start();

  /**
   * Instructs the agent to stop.
   *
   * The agent host calls <CODE>stop()</CODE> whenever the agent must
   * be stopped -- either to transfer it, to store it, or to terminate
   * it.
   *
   * An agent whose <CODE>stop()</CODE> method is called should
   * stop all computation, store any intermediate results, join all
   * active threads, and promptly return.
   *
   * @see start
   *
   */

  abstract public void
  stop();

  /**
   * Instructs the agent to conclude.
   *
   * The agent host calls <CODE>conclude()</CODE> exactly once --
   * before the agent is destroyed.
   *
   * @see initialize
   *
   */

  abstract public void
  conclude();
}
