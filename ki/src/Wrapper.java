import com.etcee.app.ki.agent.Agent;
import com.etcee.app.ki.agent.AgentIdentity;

import com.etcee.app.ki.agenthost.AgentDefinitionException;

import com.etcee.app.ki.server.AgentWrapper;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * The implementation of the agent wrapper abstract class.
 *
 */

final public class Wrapper
  extends AgentWrapper
{
  protected void
  load(ObjectInputStream objectinputstream)
    throws IOException,
           AgentDefinitionException,
           IllegalAccessException,
           InstantiationException,
           ClassNotFoundException,
           ClassCastException
  {
    Agent agent = null;
    AgentIdentity agentidentity = null;

    boolean boolNew = false;

    // Load an existing agent.

    if (objectinputstream != null)
    {
      agentidentity = (AgentIdentity)objectinputstream.readObject();
      agent = (Agent)objectinputstream.readObject();

      boolNew = false;
    }

    // Create a new agent.

    else
    {
      agentidentity = new AgentIdentity();
      agent = (Agent)Class.forName("Main").newInstance();

      boolNew = true;
    }

    // At this point in time, the agent identity may still refer to
    // some other agent host (if the agent has been transferred).  The
    // local agent host will fix this.  See
    // AgentHostImplementation.resurrect.

    setup(agent, agentidentity, boolNew);
  }

  protected void
  unload(ObjectOutputStream objectoutputstream)
       throws IOException
  {
    objectoutputstream.writeObject(getAgentIdentity());
    objectoutputstream.writeObject(getAgent());
  }
}
