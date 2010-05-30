
package etcee.ki.server;

import etcee.ki.agent.AgentIdentity;
import etcee.ki.agent.Message;
import etcee.ki.agent.MessageResponse;
import etcee.ki.agent.TransferResponse;

import etcee.ki.agenthost.AgentHost;
import etcee.ki.agenthost.AgentException;
import etcee.ki.agenthost.AgentNotFoundException;
import etcee.ki.agenthost.AgentDefinitionException;
import etcee.ki.agenthost.MessageFailedException;
import etcee.ki.agenthost.TransferFailedException;
import etcee.ki.agenthost.Response;
import etcee.ki.agenthost.Voucher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.UnknownHostException;

import java.rmi.server.UnicastRemoteObject;

import java.net.MalformedURLException;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * The agent host implementation.
 *
 * This class <EM>is</EM> thread safe.
 *
 * @see AgentHost
 *
 */

public class AgentHostImplementation
  extends UnicastRemoteObject
  implements AgentHost
{
  /**
   * The agent host name.
   *
   * The agent host name is in the form of a URL:
   *
   * <CODE>protocol://host:port/name</CODE>
   *
   */

  private String strAgentHostName = null;

  /**
   * Gets the agent host name.
   *
   */

  public String
  getAgentHostName()
  {
    return strAgentHostName;
  }

  /**
   * The server data.
   *
   */

  private ServerData serverdata = null;

  /**
   * The monitor;
   *
   */

  private Monitor monitor = null;

  /**
   * The repository.
   *
   */

  private Repository repository = null;

  /**
   * The repository entry table.
   *
   * The repository entry table maps agent identities to repository
   * entries.
   *
   */

  private Hashtable hashtableRepositoryEntries = null;

  /**
   * The agent wrapper table.
   *
   * The agent wrapper table maps agent identities to agent wrappers.
   *
   */

  private Hashtable hashtableAgentWrappers = null;

  /**
   * The transfer vouchers table.
   *
   * The transfer vouchers table maps agent identities to transfer
   * vouchers.
   *
   */

  private Hashtable hashtableTransferVouchers = null;

  /**
   * The message vouchers table.
   *
   * The message vouchers table maps agent identities to message
   * vouchers.
   *
   */

  private Hashtable hashtableMessageVouchers = null;

  /**
   * Constructs the agent host.
   *
   */

  public
  AgentHostImplementation(final String strAgentHostName,
                          String strArg1,
                          String strArg2,
                          String strArg3)
    throws IOException,
           AgentException,
           IllegalAccessException,
           InstantiationException,
           ClassNotFoundException,
           RemoteException
  {
    serverdata = new ServerData();

    monitor = new Monitor();

    this.strAgentHostName = strAgentHostName;

    repository = new Repository(strArg1, strArg2, strArg3);

    hashtableRepositoryEntries = new Hashtable();
    hashtableAgentWrappers = new Hashtable();
    hashtableTransferVouchers = new Hashtable();
    hashtableMessageVouchers = new Hashtable();

    Enumeration enumeration = repository.relinquishEntries().elements();

    while (enumeration.hasMoreElements())
    {
      final RepositoryEntry repositoryentry = (RepositoryEntry)enumeration.nextElement();

      Thread thread = new Thread()
        {
          public void
          run()
          {
            AgentIdentity agentidentity = null;

            try
            {
              agentidentity = resurrect(repositoryentry);
            }
            catch (Throwable throwable)
            {
              throwable.printStackTrace();
            }

            hashtableRepositoryEntries.put(agentidentity, repositoryentry);

            serverdata.hashtableAgentAddressBook.put(agentidentity, strAgentHostName);
          }
        };

      thread.start();
    }

    Naming.rebind(strAgentHostName, this);
  }

  /**
   * Deconstructs the agent host.
   *
   */

  public void
  deconstruct()
    throws IOException,
           AgentException
  {
    try
    {
      monitor.lockAndWait();
    }
    catch (InterruptedException ex)
    {
    }

    try
    {
      Naming.unbind(strAgentHostName);
    }
    catch (NotBoundException ex)
    {
    }

    Enumeration enumeration = hashtableRepositoryEntries.keys();

    while (enumeration.hasMoreElements())
    {
      AgentIdentity agentidentity =
        (AgentIdentity)enumeration.nextElement();

      RepositoryEntry repositoryentry = mummify(agentidentity);

      hashtableRepositoryEntries.remove(agentidentity);

      repository.assumeEntry(repositoryentry);
    }
  }

  /**
   * Finalizes the agent host.
   *
   */

  public void
  finalize()
  {
    try
    {
      deconstruct();
    }
    catch (Exception ex)
    {
    }
  }

  /**
   * Resurrects an agent.
   *
   * The following operations occur during resurrection:
   *
   * <OL>
   * <LI> create the class loader
   * <LI> instantiate the agent wrapper from within the class loader
   * <LI> read or create the agent
   * <LI> remember the agent wrapper
   * <LI> start the agent
   * </OL> 
   *
   * @returns the agent identity of the agent
   *
   * @see mummify
   *
   */

  private AgentIdentity
  resurrect(RepositoryEntry repositoryentry)
       throws IOException,
              AgentDefinitionException,
              IllegalAccessException,
              InstantiationException,
              ClassNotFoundException
  {
    File fileResource = repositoryentry.getResource();
    File fileData = repositoryentry.getData();

    AgentClassLoader agentclassloader = new AgentClassLoader(fileResource);

    Class c = agentclassloader.loadClass("");

    AgentWrapper agentwrapper = (AgentWrapper)c.newInstance();

    agentwrapper.initialize(this, serverdata);

    if (fileData.exists())
    {
      FileInputStream fileinputstream =
        new FileInputStream(fileData);

      ObjectInputStream objectinputstream =
        new ObjectInputStream(fileinputstream);

      agentwrapper.load(objectinputstream);

      objectinputstream.close();
    }
    else
    {
      agentwrapper.load(null);
    }

    AgentIdentity agentidentity = agentwrapper.getAgentIdentity();

    agentidentity.setAgentHostName(strAgentHostName);

    hashtableAgentWrappers.put(agentidentity, agentwrapper);

    // Initializing and starting the agent must be the last thing we
    // do.  If it's not, we run the risk of the agent identity not
    // being set up correctly.

    if (fileData.exists())
    {
      agentwrapper.agentStart();
    }
    else
    {
      agentwrapper.agentInitialize();
      agentwrapper.agentStart();
    }

    return agentidentity;
  }    

  /**
   * Mummifies an agent.
   *
   * <OL>
   * <LI> forget the agent wrapper
   * <LI> stop the agent
   * <LI> write the agent
   * <LI> destroy the class loader
   * </OL> 
   *
   * @see resurrect
   *
   */

  private RepositoryEntry
  mummify(AgentIdentity agentidentity)
       throws IOException,
              AgentNotFoundException
  {
    AgentWrapper agentwrapper =
      (AgentWrapper)hashtableAgentWrappers.remove(agentidentity);

    RepositoryEntry repositoryentry = findRepositoryEntry(agentidentity);

    File fileData = repositoryentry.getData();

    agentwrapper.agentStop();

    FileOutputStream fileoutputstream =
      new FileOutputStream(fileData);

    ObjectOutputStream objectoutputstream =
      new ObjectOutputStream(fileoutputstream);

    agentwrapper.unload(objectoutputstream);

    objectoutputstream.close();

    AgentClassLoader agentclassloader =
      (AgentClassLoader)agentwrapper.getClass().getClassLoader();

    agentclassloader.deconstruct();

    return repositoryentry;
  }    

  /**
   * Initiates a message send.
   *
   */

  public final MessageResponse
  initiateSendMessage(final String strAgentHostName,
                      final AgentIdentity agentidentity,
                      final Message message)
  {
    final MessageResponse messageresponse = new MessageResponse();

    Thread thread = new Thread()
      {
        public void
        run()
        {
          Voucher voucher = new Voucher();

          hashtableMessageVouchers.put(agentidentity, voucher);

          try
          {
            String str = strAgentHostName;

            while (true)
            {
              AgentHost agenthost = AgentHostImplementation.this;

              if (str != null && !AgentHostImplementation.this.strAgentHostName.equals(str))
              {
                agenthost = lookupAgentHost(str);
              }

              Response response = agenthost.sendMessage(AgentHostImplementation.this,
                                                        voucher,
                                                        agentidentity,
                                                        message);

              if (response.nCode == Response.SUCCEEDED)
              {
                messageresponse.setSucceeded(response.objResponse);

                break;
              }
              else if (response.nCode == Response.FAILED)
              {
                messageresponse.setFailed((Exception)response.objResponse);

                break;
              }
              else if (response.nCode == Response.NOT_FOUND)
              {
                // No handler defined.

                messageresponse.setFailed(new NoSuchMethodException(message.getMessageName()));

                break;
              }
              else if (response.nCode == Response.TRY_AGAIN)
              {
                // Update the address book.

                str = (String)response.objResponse;

                serverdata.hashtableAgentAddressBook.put(agentidentity, str);
              }
              else
              {
                System.out.println("** nCode = " + response.nCode);
                System.out.println("** response = " + response.objResponse);

                break;
              }
            }
          }
          catch (Exception ex)
          {
            messageresponse.setFailed(ex);
          }
          finally
          {
            hashtableMessageVouchers.remove(agentidentity);
          }
        }
      };

    thread.start();

//  thread.setPriority(thread.getPriority() + 1);

    return messageresponse;
  }

  /**
   * Sends a message.
   *
   * This method can be called via RMI.
   *
   * @see AgentHost.sendMessage
   *
   */

  public Response
  sendMessage(AgentHost agenthost,
              Voucher voucher,
              AgentIdentity agentidentity,
              Message message)
    throws MessageFailedException,
           AgentNotFoundException,
           RemoteException
  {
    AgentWrapper agentwrapper = null;

    try
    {
      agentwrapper = findAgentWrapper(agentidentity);
    }
    catch (AgentNotFoundException ex)
    {
      // If we aren't hosting the agent, try to find the agent in our
      // address book.

      String strAgentHostName = (String)serverdata.hashtableAgentAddressBook.get(agentidentity);

      if (this.strAgentHostName.equals(strAgentHostName))
      {
        return new Response(agentidentity, ex, Response.FAILED);
      }

      if (strAgentHostName != null)
      {
        return new Response(agentidentity, strAgentHostName, Response.TRY_AGAIN);
      }

      // If the address book doesn't contain a entry, get the last
      // known host from the agent identity itself.

      strAgentHostName = agentidentity.getAgentHostName();

      if (this.strAgentHostName.equals(strAgentHostName))
      {
        return new Response(agentidentity, ex, Response.FAILED);
      }

      return new Response(agentidentity, strAgentHostName, Response.TRY_AGAIN);
    }      

    return agentwrapper.handleMessage(message);
  }

  /**
   * Initiates an agent transfer.
   *
   */

  public final TransferResponse
  initiateTransfer(final String strAgentHostName,
                   final AgentIdentity agentidentity)
  {
    final TransferResponse transferresponse = new TransferResponse();

    Thread thread = new Thread()
      {
        public void
        run()
        {
          Voucher voucher = new Voucher();

          hashtableTransferVouchers.put(agentidentity, voucher);

          try
          {
            AgentHost agenthost = AgentHostImplementation.this;

            if (!AgentHostImplementation.this.strAgentHostName.equals(strAgentHostName))
            {
              agenthost = lookupAgentHost(strAgentHostName);
            }

            agenthost.requestToTransfer(AgentHostImplementation.this,
                                        voucher,
                                        agentidentity);

            // Update the address book.

            serverdata.hashtableAgentAddressBook.put(agentidentity, strAgentHostName);

            transferresponse.setSucceeded();
          }
          catch (Exception ex)
          {
            transferresponse.setFailed(ex);
          }
          finally
          {
            hashtableTransferVouchers.remove(agentidentity);
          }
        }
      };

    thread.start();

//  thread.setPriority(thread.getPriority() + 1);

    return transferresponse;
  }

  /**
   * Requests that an agent transfer occur.
   *
   * This method should only be called via RMI.
   *
   * @see AgentHost.requestToTransfer
   *
   */

  public void
  requestToTransfer(AgentHost agenthost,
                    Voucher voucher,
                    AgentIdentity agentidentity)
    throws IOException,
           IllegalAccessException,
           InstantiationException,
           ClassNotFoundException,
           TransferFailedException,
           AgentNotFoundException,
           AgentDefinitionException,
           RemoteException
  {
    byte [] rgbResource = null;
    byte [] rgbData = null;

    RepositoryEntry repositoryentry = null;

    agenthost.beginTransfer(voucher, agentidentity);

    rgbResource = agenthost.transferResourceFile(voucher, agentidentity);

    rgbData = agenthost.transferDataFile(voucher, agentidentity);

    agenthost.endTransfer(voucher, agentidentity);

    repositoryentry = repository.createEntry(rgbResource, rgbData);

    agentidentity = resurrect(repositoryentry);

    hashtableRepositoryEntries.put(agentidentity, repositoryentry);

    serverdata.hashtableAgentAddressBook.put(agentidentity, strAgentHostName);
  }

  /**
   * Begins a transfer.
   *
   * This method should only be called via RMI.
   *
   * @see AgentHost.beginTransfer
   *
   */

  public void
  beginTransfer(Voucher voucher,
                AgentIdentity agentidentity)
    throws IOException,
           TransferFailedException,
           AgentNotFoundException,
           RemoteException
  {
    testTransferVoucher(voucher, agentidentity);

    findRepositoryEntry(agentidentity);

    mummify(agentidentity);
  }

  /**
   * Ends a transfer.
   *
   * This method should only be called via RMI.
   *
   * @see AgentHost.endTransfer
   *
   */

  public void
  endTransfer(Voucher voucher,
              AgentIdentity agentidentity)
    throws IOException,
           TransferFailedException,
           AgentNotFoundException,
           RemoteException
  {
    testTransferVoucher(voucher, agentidentity);

    RepositoryEntry repositoryentry = removeRepositoryEntry(agentidentity);

    repository.deleteEntry(repositoryentry);
  }

  /**
   * Transfers the agent's resource file.
   *
   * This method should only be called via RMI.
   *
   * @see AgentHost.transferResourceFile
   *
   */

  public byte []
  transferResourceFile(Voucher voucher,
                       AgentIdentity agentidentity)
    throws IOException,
           TransferFailedException,
           AgentNotFoundException,
           RemoteException
  {
    testTransferVoucher(voucher, agentidentity);

    RepositoryEntry repositoryentry = findRepositoryEntry(agentidentity);

    return repositoryentry.getResourceAsBytes();
  }

  /**
   * Transfers the agent's data file.
   *
   * This method should only be called via RMI.
   *
   * @see AgentHost.transferDataFile
   *
   */

  public byte []
  transferDataFile(Voucher voucher,
                   AgentIdentity agentidentity)
    throws IOException,
           TransferFailedException,
           AgentNotFoundException,
           RemoteException
  {
    testTransferVoucher(voucher, agentidentity);

    RepositoryEntry repositoryentry = findRepositoryEntry(agentidentity);

    return repositoryentry.getDataAsBytes();
  }

  private void
  testTransferVoucher(Voucher voucher,
                      AgentIdentity agentidentity)
       throws TransferFailedException
  {
    if (!voucher.equals(hashtableTransferVouchers.get(agentidentity)))
    {
      throw new TransferFailedException("invalid voucher");
    }
  }

  private RepositoryEntry
  findRepositoryEntry(AgentIdentity agentidentity)
       throws AgentNotFoundException
  {
    RepositoryEntry repositoryentry =
      (RepositoryEntry)hashtableRepositoryEntries.get(agentidentity);

    if (repositoryentry == null)
    {
      throw new AgentNotFoundException(agentidentity, "invalid identity");
    }

    return repositoryentry;
  }

  private RepositoryEntry
  removeRepositoryEntry(AgentIdentity agentidentity)
       throws AgentNotFoundException
  {
    RepositoryEntry repositoryentry =
      (RepositoryEntry)hashtableRepositoryEntries.remove(agentidentity);

    if (repositoryentry == null)
    {
      throw new AgentNotFoundException(agentidentity, "invalid identity");
    }

    return repositoryentry;
  }

  private AgentWrapper
  findAgentWrapper(AgentIdentity agentidentity)
       throws AgentNotFoundException
  {
    AgentWrapper agentwrapper =
      (AgentWrapper)hashtableAgentWrappers.get(agentidentity);

    if (agentwrapper == null)
    {
      throw new AgentNotFoundException(agentidentity, "invalid identity");
    }

    return agentwrapper;
  }

  private AgentWrapper
  removeAgentWrapper(AgentIdentity agentidentity)
       throws AgentNotFoundException
  {
    AgentWrapper agentwrapper =
      (AgentWrapper)hashtableAgentWrappers.remove(agentidentity);

    if (agentwrapper == null)
    {
      throw new AgentNotFoundException(agentidentity, "invalid identity");
    }

    return agentwrapper;
  }

  /**
   * Returns an enumeration of all agents.
   *
   * The results may only be valid for a short period of time.
   *
   */

  public Enumeration
  enumerateAllAgents()
  {
    return hashtableAgentWrappers.keys();
  }

  /**
   * Determines whether or not the agent identity is locked.
   *
   * @see lockAgentIdentity
   * @see unlockAgentIdentity
   *
   */

  private boolean
  isLocked(AgentIdentity agentidentity)
  {
    return hashtableTransferVouchers.containsKey(agentidentity);
  }

  /**
   * Locks the agent identity.
   *
   * Locks the agent identity against further access.  Locking
   * occurs before an agent is mummified.
   *
   * @throws AgentLockedException if the agent identity is already
   *         locked.
   *
   * @see unlockAgentIdentity
   *
   */

  private void
  lockAgentIdentity(Voucher voucher,
                    AgentIdentity agentidentity)
       throws AgentLockedException
  {
    // The transfer voucher might be null if this method is called
    // from the agent hosts's finalize method.  In this case, no
    // transfer is taking place, however we still need to keep the
    // hashtable happy.

    if (voucher == null)
    {
      voucher = new Voucher();
    }

    synchronized (hashtableTransferVouchers)
    {
      if (hashtableTransferVouchers.containsKey(agentidentity))
      {
        throw new AgentLockedException(agentidentity);
      }

      hashtableTransferVouchers.put(agentidentity, voucher);
    }
  }

  /**
   * Unlocks the agent identity.
   *
   * @see lockAgentIdentity
   *
   */

  private void
  unlockAgentIdentity(AgentIdentity agentidentity)
  {
    hashtableTransferVouchers.remove(agentidentity);
  }

  /**
   * Looks up an agent host.
   *
   * @throws MalformedURLException if the agent host name was not a
   *         valid URL.
   *
   * @throws UnknownHostException if the registry was on an unknown
   *         host.
   *
   * @throws RemoteException if the registry could not be contacted.
   *
   * @throws NotBoundException if the agent host name was not bound.
   *
   */

  private final AgentHost
  lookupAgentHost(String strAgentHostName)
    throws MalformedURLException,
           UnknownHostException,
           RemoteException,
           NotBoundException
  {
    return (AgentHost)Naming.lookup(strAgentHostName);
  }
}
