/* **********************************************************************
 * MouseListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class MouseListener extends JessListener
                           implements java.awt.event.MouseListener
{
  public MouseListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void mouseClicked(MouseEvent e) { receiveEvent(e); }
  public void mouseEntered(MouseEvent e) { receiveEvent(e); }
  public void mouseExited(MouseEvent e) { receiveEvent(e); }
  public void mousePressed(MouseEvent e) { receiveEvent(e); }
  public void mouseReleased(MouseEvent e) { receiveEvent(e); }
}
