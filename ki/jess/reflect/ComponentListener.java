/* **********************************************************************
 * ComponentListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class ComponentListener extends JessListener
                               implements java.awt.event.ComponentListener
{
  public ComponentListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void componentHidden(ComponentEvent e) { receiveEvent(e); }
  public void componentMoved(ComponentEvent e) { receiveEvent(e); }
  public void componentResized(ComponentEvent e) { receiveEvent(e); }
  public void componentShown(ComponentEvent e) { receiveEvent(e); }
}
