/* **********************************************************************
 * JessListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class JessListener
{
  private Funcall m_fc;
  private Rete m_engine;

  JessListener(String uf, Rete engine) throws ReteException
  {
    m_engine = engine;
    m_fc = new Funcall(uf, engine);
    m_fc.setLength(2);
  }      

  final void receiveEvent(AWTEvent e)
  {
    try
      {
        m_fc.set(new Value(e, RU.EXTERNAL_ADDRESS), 1);
        Funcall.simpleExecute(m_fc, m_engine.globalContext());
      }
    catch (ReteException re)
      {
        m_engine.errStream().println(re);
      }
  }
}
