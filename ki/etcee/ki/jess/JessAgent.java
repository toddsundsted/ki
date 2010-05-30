
package etcee.ki.jess;

import etcee.ki.agent.Agent;

import java.io.InputStream;

import jess.NullDisplay;
import jess.Userpackage;
import jess.Value;
import jess.ReteException;
import jess.Rete;
import jess.Jesp;

abstract public class JessAgent
  extends Agent
{
  /**
   * The Rete engine.
   *
   */

  private transient Rete rete = null;

  /**
   * Gets the Rete engine.
   *
   */

  protected Rete
  getRete()
  {
    if (rete == null) initializeJessAgent(null);

    return rete;
  }

  /**
   * The jess agent thread class.
   *
   */

  class JessAgentThread
    extends Thread
  {
    synchronized public void
    run()
    {
      while (true)
      {
        try
        {
          rete.run();
        }
        catch (ReteException ex)
        {
          ex.printStackTrace();
        }
        catch (Throwable throwable)
        {
        }

        try
        {
          wait();
        }
        catch (InterruptedException ex)
        {
          break;
        }
      }
    }
  };

  /**
   * The jess agent thread.
   *
   */

  private transient Thread thread = null;

  /**
   * Evaluate a command.
   *
   */

  protected Value
  evaluateCommand(String strCommand)
    throws ReteException
  {
    if (rete == null) initializeJessAgent(null);

    Value value = rete.executeCommand(strCommand);

    synchronized (thread)
    {
      thread.notifyAll();
    }

    return value;
  }

  /**
   * Evaluate a script.
   *
   */

  protected Value
  evaluateScript(String strScript)
    throws ReteException
  {
    if (rete == null) initializeJessAgent(null);

    Value value = new Jesp(getClass().getResourceAsStream(strScript), rete).parse(false);

    synchronized (thread)
    {
      thread.notifyAll();
    }

    return value;
  }

  /**
   * Run.
   *
   */

  protected void
  run()
  {
    synchronized (thread)
    {
      thread.notifyAll();
    }
  }

  /**
   * Initialize the jess agent.
   *
   */

  public void
  initializeJessAgent(String strScript)
  {
    // Create the Rete engine.

    rete = new Rete(new NullDisplay());

    // Load the default packages.

    String [] rgPackages =
    {
      "jess.StringFunctions",
      "jess.PredFunctions",
      "jess.MultiFunctions", 
      "jess.MiscFunctions",
      "jess.MathFunctions",
      "jess.BagFunctions",
    };

    try
    {
      for (int i = 0; i < rgPackages.length; i++)
      {
        rete.addUserpackage((Userpackage)Class.forName(rgPackages[i]).newInstance());
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    // Load the jess agent functions package.

    try
    {
      Object object = Class.forName("etcee.ki.jess.JessAgentFunctions").newInstance();

      JessAgentFunctions jessagentfunctions = (JessAgentFunctions)object;

      jessagentfunctions.setAgentAndAgentContext(this, getAgentContext());

      Userpackage userpackage = (Userpackage)object;

      rete.addUserpackage(userpackage);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    // Load the initialization script.

    if (strScript != null)
    {
      InputStream inputstream  = getClass().getResourceAsStream(strScript);

      try
      {
        new Jesp(inputstream, rete).parse(false);
      }
      catch (ReteException ex)
      {
        ex.printStackTrace();
      }
    }

    thread = new JessAgentThread();

    thread.start();
  }
}
