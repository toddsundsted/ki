
package etcee.ki.agenthost;

import etcee.ki.agent.AgentIdentity;
import etcee.ki.agent.Message;

import java.io.IOException;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The agent host.
 *
 * The <CODE>AgentHost</CODE> class defines the basic functionality
 * that all agent hosts must provide.
 *
 */

public interface AgentHost
  extends Remote
{
  /**
   * Sends a message.
   *
   * If the specified agent is no longer hosted by this agent host,
   * the agent host will attempt to locate it.
   *
   * @parameter agenthost is the sending agent host.
   *
   * @throws MessageFailedException if the message dispatch failed --
   *         either because the host rejected the dispatch, or the
   *         dispatch failed for some other reason.
   *
   * @throws AgentNotFoundException if the specified agent did not
   *         exist on the specified host.
   *
   * @throws RemoteException if something unexpected happened during
   *         the remote method invocation.
   *
   */

  public Response
  sendMessage(AgentHost agenthost,
              Voucher voucher,
              AgentIdentity agentidentity,
              Message message)
    throws MessageFailedException,
           AgentNotFoundException,
           RemoteException;

  /**
   * Requests that an agent transfer occur.
   *
   * A remote host calls this method on behalf of an agent and
   * requests the local host transfer the agent.
   *
   * A transfer occurs as follows:
   *
   * <OL>
   * <LI>the local host invokes the <CODE>beginTransfer()</CODE>
   *     method on the remote host
   * <LI>the local host invokes the <CODE>transferResourceFile()</CODE>
   *     method on the remote host
   * <LI>the local host invokes the <CODE>transferDataFile()</CODE>
   *     method on the remote host
   * <LI>the local host invokes the <CODE>endTransfer()</CODE>
   *     method on the remote host
   * </OL>
   *
   * @parameter agenthost the agent host originating the request.
   *
   * @throws IOException if problems occurred while serializing the
   *         data.
   *
   * @throws IllegalAccessException if a class or initializer was not
   *         accessible.
   *
   * @throws InstantiationException if the agent tried to instantiate
   *         an abstract class or an interface, or if the instantiation
   *         failed for some other reason.
   *
   * @throws ClassNotFoundException if a required class could not be
   *         found.
   *
   * @throws TransferFailedException if the local host attempted the
   *         transfer but it failed -- either because the specified
   *         host couldn't be found, the specified host rejected the
   *         transfer, or the transfer failed for some other reason.
   *
   * @throws AgentNotFoundException if the specified agent did not
   *         exist on the specified host.
   *
   * @throws RemoteException if something unexpected happened during
   *         the remote method invocation.
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
           RemoteException;

  /**
   * Begins a transfer.
   *
   * @throws IOException if problems occurred while serializing the
   *         data.
   *
   * @throws TransferFailedException if the transfer failed -- either
   *         because the remote host rejected the transfer, or the
   *         transfer failed for some other reason.
   *
   * @throws AgentNotFoundException if the specified agent did not
   *         exist on the specified host.
   *
   * @throws RemoteException if something unexpected happened during
   *         the remote method invocation.
   *
   * @see endTransfer
   *
   */

  public void
  beginTransfer(Voucher voucher,
                AgentIdentity agentidentity)
    throws IOException,
           TransferFailedException,
           AgentNotFoundException,
           RemoteException;

  /**
   * Ends a transfer.
   *
   * @throws IOException if problems occurred while serializing the
   *         data.
   *
   * @throws TransferFailedException if the transfer failed -- either
   *         because the remote host rejected the transfer, or the
   *         transfer failed for some other reason.
   *
   * @throws AgentNotFoundException if the specified agent did not
   *         exist on the specified host.
   *
   * @throws RemoteException if something unexpected happened during
   *         the remote method invocation.
   *
   * @see beginTransfer
   *
   */

  public void
  endTransfer(Voucher voucher,
              AgentIdentity agentidentity)
    throws IOException,
           TransferFailedException,
           AgentNotFoundException,
           RemoteException;

  /**
   * Transfers the agent's resource file.
   *
   * The local host calls this method to transfer an agent's resource
   * file from the remote host to the local host.
   *
   * @throws IOException if problems occurred while serializing the
   *         data.
   *
   * @throws TransferFailedException if the transfer failed -- either
   *         because the remote host rejected the transfer, or the
   *         transfer failed for some other reason.
   *
   * @throws AgentNotFoundException if the specified agent did not
   *         exist on the specified host.
   *
   * @throws RemoteException if something unexpected happened during
   *         the remote method invocation.
   *
   * @see requestToTransfer
   *
   */

  public byte []
  transferResourceFile(Voucher voucher,
                       AgentIdentity agentidentity)
    throws IOException,
           TransferFailedException,
           AgentNotFoundException,
           RemoteException;

  /**
   * Transfers the agent's data file.
   *
   * The local host calls this method to transfer an agent's data
   * file from the remote host to the local host.
   *
   * @throws IOException if problems occurred while serializing the
   *         data.
   *
   * @throws TransferFailedException if the transfer failed -- either
   *         because the remote host rejected the transfer, or the
   *         transfer failed for some other reason.
   *
   * @throws AgentNotFoundException if the specified agent did not
   *         exist on the specified host.
   *
   * @throws RemoteException if something unexpected happened during
   *         the remote method invocation.
   *
   * @see requestToTransfer
   *
   */

  public byte []
  transferDataFile(Voucher voucher,
                   AgentIdentity agentidentity)
    throws IOException,
           TransferFailedException,
           AgentNotFoundException,
           RemoteException;
}
