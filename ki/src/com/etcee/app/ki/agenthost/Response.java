package com.etcee.app.ki.agenthost;

import com.etcee.app.ki.agent.AgentIdentity;

import java.io.Serializable;

/**
 * The response.
 *
 */

public final class Response
  implements Serializable
{
  /**
   * The agent identity.
   *
   */

  public AgentIdentity agentidentity;

  /**
   * The response.
   *
   */

  public Object objResponse;

  /**
   * The result code.
   *
   */

  public int nCode;

  public final static int SUCCEEDED = 0;
  public final static int FAILED = 1;
  public final static int TRY_AGAIN = 2;

  public final static int LOCKED = 3;
  public final static int NOT_FOUND = 4;

  /**
   * Constructs the response.
   *
   */

  public
  Response(AgentIdentity agentidentity,
           Object objResponse,
           int nCode)
  {
    this.agentidentity = agentidentity;
    this.objResponse = objResponse;
    this.nCode = nCode;
  }
}
