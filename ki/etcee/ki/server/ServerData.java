
package etcee.ki.server;

import java.io.Serializable;

import java.util.Hashtable;

/**
 * The persistant server data.
 *
 * The <CODE>ServerData</CODE> class maintains server state across
 * runs of the server.
 *
 */

final class ServerData implements Serializable
{
  /**
   * The agent address book.
   *
   * The agent address book maps agent identities to agent host
   * names.
   *
   */

  final Hashtable hashtableAgentAddressBook = new Hashtable();

  /**
   * The agent directory.
   *
   * The agent directory maps strings to agent identities.
   *
   * The agent directory can be used as the yellow pages - mapping
   * services to the agent that provides those services.
   *
   */

  final Hashtable hashtableDirectory = new Hashtable();
}
