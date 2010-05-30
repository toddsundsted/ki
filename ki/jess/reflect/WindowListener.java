/* **********************************************************************
 * WindowListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class WindowListener extends JessListener
                            implements java.awt.event.WindowListener
{
  private Funcall m_fc;
  private Rete m_engine;

  public WindowListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void windowActivated(WindowEvent e) { receiveEvent(e); }
  public void windowDeactivated(WindowEvent e) { receiveEvent(e); }
  public void windowDeiconified(WindowEvent e) { receiveEvent(e); }
  public void windowIconified(WindowEvent e) { receiveEvent(e); }
  public void windowOpened(WindowEvent e) { receiveEvent(e); }
  public void windowClosed(WindowEvent e) { receiveEvent(e); }
  public void windowClosing(WindowEvent e) { receiveEvent(e); }
}
