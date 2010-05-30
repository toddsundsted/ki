/** **********************************************************************
 * Parent class of all nodes of the pattern network
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public abstract class Node 
{
  
  final static int LEFT     = 0;
  final static int RIGHT    = 1;
  final static int SINGLE   = 2;
  final static int ACTIVATE = 3;


  /**
    What kind of node is this?
    */
  
  int m_command;

  /**
    How many rules use me?
    */

  int m_usecount = 1;

  /**
    Succ is the list of this node's successors
    */

  Vector m_succ;
  public final Vector succ() { return m_succ; }
  Successor[] m_localSucc;
  int m_nsucc;

  Rete m_engine;

  EvalCache m_cache;
  
  /**
    Constructor
    */

  Node(Rete engine)
  {
    m_succ = new Vector();
    m_engine = engine;
    m_cache = new EvalCache();
  }

  Funcall eval(Value v, Token t) throws ReteException
  {
    Funcall vv = (Funcall) v.funcallValue().cloneInto(m_cache.getFuncall());
    
    for (int i=0; i<vv.size(); i++)
      {
        if (vv.get(i).type() ==  RU.INTARRAY)
          {
            int[] binding = vv.get(i).intArrayValue();
            if (binding[2] == -1)
              vv.set(t.fact(binding[0]).get(binding[1]), i);
            else
              {
                ValueVector subv =
                  t.fact(binding[0]).get(binding[1]).listValue();
                vv.set(subv.get(binding[2]), i);
              }
          }
        else if (vv.get(i).type() ==  RU.FUNCALL)
          {
            Value val = m_cache.getValue();
            val.resetValue(eval(vv.get(i), t), RU.FUNCALL);
            vv.set(val, i);
          }
      }
    return vv;
  }

  /**
    Move the successors into an array
    */
  void freeze() 
  {
    m_nsucc = m_succ.size();
    if (m_localSucc == null || m_localSucc.length < m_nsucc)
      m_localSucc = new Successor[m_nsucc];
    for (int i=0; i< m_nsucc; i++)
      {
        m_localSucc[i] = (Successor) m_succ.elementAt(i);
        m_localSucc[i].m_node.freeze();
      }
    
  }

  void removeSuccessor(Successor s)
  {
    m_succ.removeElement(s);
  }

  /**
    Do the business of this node.
    */

  abstract boolean callNode(Token token, int callType) throws ReteException;

  // helper function; all nodes need this
  final void passAlong(Token t) throws ReteException
  {
    Successor [] sa = m_localSucc;
    for (int j=0; j<m_nsucc; j++) 
      {
        Successor s = sa[j];
        s.m_node.callNode(t, s.m_callType);
      }
  }


}






