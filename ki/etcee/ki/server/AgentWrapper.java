
package etcee.ki.server;

import etcee.ki.agent.Agent;
import etcee.ki.agent.AgentIdentity;
import etcee.ki.agent.AgentContext;
import etcee.ki.agent.Message;

import etcee.ki.agenthost.Response;
import etcee.ki.agenthost.AgentDefinitionException;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.util.Hashtable;

/**
 * The agent wrapper.
 *
 * The <CODE>AgentWrapper</CODE> class is the agent host facing
 * interface to an agent.  It provides thread safe access to an agent
 * and ensures that message sends and agent transfers are
 * orchestrated properly.
 *
 * The <CODE>AgentWrapper</CODE> class and a special subclass support
 * the bootstrap loading of an agent by an agent class loader.  Here's
 * how it works:
 *
 * <OL>
 * <LI>create a new agent class loader
 * <LI>using the agent class loader, create a new agent wrapper
 * <LI>initialize the agent wrapper
 * <LI>load the agent into the agent wrapper
 * <LI>initialize and/or start the agent
 * </OL>
 *
 * This class <EM>is</EM> thread safe.
 *
 * @see initialize
 * @see load
 *
 */

abstract public class AgentWrapper
{
  /**
   * The agent.
   *
   */

  private Agent agent = null;

  /**
   * Gets the agent.
   *
   */

  protected final Agent
  getAgent()
  {
    return agent;
  }

  /**
   * The agent identity.
   *
   */

  private AgentIdentity agentidentity = null;

  /**
   * Gets the agent identity.
   *
   */

  protected final AgentIdentity
  getAgentIdentity()
  {
    return agentidentity;
  }

  /**
   * The agent host implementation.
   *
   */

  private AgentHostImplementation agenthostimplementation = null;

  /**
   * Gets the agent host implementation.
   *
   */

  final AgentHostImplementation
  getAgentHostImplementation()
  {
    return agenthostimplementation;
  }

  /**
   * The server data.
   *
   */

  private ServerData serverdata = null;

  /**
   * Gets the server data.
   *
   */

  final ServerData
  getServerData()
  {
    return serverdata;
  }

  /**
   * The target.
   *
   */

  private Object objTarget = null;

  /**
   * The method table.
   *
   * The method table maps message names to methods.
   *
   */

  private Hashtable hashtableMethods = null;

  /**
   * The state.
   *
   */

  private int nState = INERT;

  final static int INERT = -1;
  final static int INITIALIZING = 1;
  final static int INITIALIZED = 2;
  final static int STARTING = 3;
  final static int STARTED = 4;
  final static int STOPPING = 5;
  final static int STOPPED = 6;
  final static int CONCLUDING = 7;
  final static int CONCLUDED = 8;

  /**
   * Gets the state.
   *
   */

  final int
  getState()
  {
    return nState;
  }

  /**
   * Initializes the agent wrapper.
   *
   * The agent host calls this method after it creates the agent
   * wrapper.
   *
   * @see AgentHostImplementation.resurrect
   *
   */

  final void
  initialize(AgentHostImplementation agenthostimplementation,
             ServerData serverdata)
  {
    this.agenthostimplementation = agenthostimplementation;
    this.serverdata = serverdata;
  }

  /**
   * Sets up the agent.
   *
   * The special subclass calls this method after it has loaded the
   * agent (via the <CODE>load()</CODE> method).
   *
   * The agent may or may not have been initialized (via the
   * <CODE>initializeAgent()</CODE> method) in a previous life.
   *
   * @parameter boolNew indicates whether or not the agent has been
   *            initialized.
   *
   * @see load
   *
   */

  protected final void
  setup(Agent agent, AgentIdentity agentidentity, boolean boolNew)
    throws AgentDefinitionException
  {
    this.agent = agent;

    this.agentidentity = agentidentity;

    // Create the agent context.

    agent.setAgentContext(new AgentContextImplementation(this));

    objTarget = agent.getTarget();

    // At this point in time, the agent identity may still refer to
    // some other agent host (if the agent has been transferred).  The
    // local agent host will fix this.  See
    // AgentHostImplementation.resurrect.

    nState = boolNew ? INERT : STOPPED;

    // Scan the target for message handlers.

    Method [] rgmethod = objTarget.getClass().getMethods();

    hashtableMethods = new Hashtable(rgmethod.length);

    for (int i = 0; i < rgmethod.length; i++)
    {
      String strName = rgmethod[i].getName();

      if (strName.startsWith("handle"))
      {
        String strMessageName = strName.substring(6);

        if (hashtableMethods.put(strMessageName, rgmethod[i]) != null)
        {
          throw new AgentDefinitionException(agentidentity, strMessageName);
        }
      }
    }
  }

  /**
   * The monitor.
   *
   */

  private Monitor monitor = new Monitor();

  /**
   * Locks the agent wrapper.
   *
   * Locks the agent wrapper -- this prevents it from handling any new
   * messages.
   *
   * @see lockAndWait
   *
   */

  final void
  lock()
  {
    monitor.setLockFlag(true);
  }

  /**
   * Locks the agent and waits.
   *
   * Locks the agent and waits for it to finish handling any
   * unfinished messages.
   *
   * @see lock
   *
   */

  final void
  lockAndWait()
    throws InterruptedException
  {
    monitor.lockAndWait();
  }

  /**
   * Handles a message.
   *
   * The agent host calls this method when it wants the agent to
   * handle a message.
   *
   * @see AgentHostImplementation.sendMessage
   *
   */

  final Response
  handleMessage(Message message)
  {
    String strMessageName = message.getMessageName();

    Object [] rgobj = message.getParameters();

    Method method = (Method)hashtableMethods.get(strMessageName);

    // If a handler wasn't defined, try the catch-all handler.  Pass
    // it the message name and the parameters.

    if (method == null)
    {
      method = (Method)hashtableMethods.get("");

      Object [] rgobjTemp = new Object [2];

      rgobjTemp[0] = strMessageName;

      rgobjTemp[1] = rgobj;

      rgobj = rgobjTemp;
    }

    // If a handler wasn't defined, give up.

    if (method == null)
    {
      return new Response(agentidentity, null, Response.NOT_FOUND);
    }

    // Try and invoke the message handler.

    if (monitor.beginOperation() == false)
    {
      return new Response(agentidentity, null, Response.LOCKED);
    }

    Object obj = null;

    try
    {
      obj = method.invoke(objTarget, rgobj);
    }
    catch (IllegalAccessException ex)
    {
      return new Response(agentidentity, ex, Response.FAILED);
    }
    catch (IllegalArgumentException ex)
    {
      return new Response(agentidentity, ex, Response.FAILED);
    }
    catch (InvocationTargetException ex)
    {
      return new Response(agentidentity, ex.getTargetException(), Response.FAILED);
    }
    finally
    {
      monitor.endOperation();
    }

    return new Response(agentidentity, obj, Response.SUCCEEDED);
  }

  /**
   * Initializes the agent.
   *
   */

  final void
  agentInitialize()
  {
    nState = AgentWrapper.INITIALIZING;

    agent.initialize();

    nState = AgentWrapper.INITIALIZED;
  }

  /**
   * Starts the agent.
   *
   */

  final void
  agentStart()
  {
    nState = AgentWrapper.STARTING;

    agent.start();

    nState = AgentWrapper.STARTED;
  }

  /**
   * Stops the agent.
   *
   */

  final void
  agentStop()
  {
    nState = AgentWrapper.STOPPING;

    agent.stop();

    nState = AgentWrapper.STOPPED;
  }

  /**
   * Concludes the agent.
   *
   */

  final void
  agentConclude()
  {
    nState = AgentWrapper.CONCLUDING;

    agent.conclude();

    nState = AgentWrapper.CONCLUDED;
  }

  /**
   * Loads the agent wrapper.
   *
   * Loading consists of creating an agent either from the
   * serialized state information contained in the specified stream or
   * from scratch.
   *
   * @parameter objectinputstream the input stream or null.
   *
   * @throws IOException if problems occurred while reading from the
   *         stream.
   *
   * @throws AgentDefinitionException if the agent defined multiple
   *         messages handlers of the same name.
   *
   * @throws IllegalAccessException if a class or initializer was not
   *         accessible.
   *
   * @throws InstantiationException if the agent tried to instantiate
   *         an abstract class or an interface, or if the
   *         instantiation failed for some other reason.
   *
   * @throws ClassNotFoundException if the class of a serialized
   *         object could not be found.
   *
   * @throws ClassCastException if the serialized object could not be
   *         cast to an agent.
   * */

  abstract protected void
  load(ObjectInputStream objectinputstream)
    throws IOException,
           AgentDefinitionException,
           IllegalAccessException,
           InstantiationException,
           ClassNotFoundException,
           ClassCastException;

  /**
   * Unloads the agent wrapper.
   *
   * @throws IOException if problems occurred while writing to the
   *         stream.
   *
   */

  abstract protected void
  unload(ObjectOutputStream objectoutputstream)
    throws IOException;
}
