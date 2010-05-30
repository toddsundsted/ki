/** **********************************************************************
 * A Successor is used to tell Nodes how to interact
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */


package jess;

public class Successor
{

  public Node m_node;
  public int m_callType;

  /**
    Create a successor. callType should be either Node.LEFT, Node.RIGHT,
    Node.SINGLE, or Node.ACTIVATE.
    */

  public Successor(Node node, int callType)
  {
    m_node = node;
    m_callType = callType;
  }

}
