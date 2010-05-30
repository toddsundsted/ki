/* **********************************************************************
 * MouseMotionListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class MouseMotionListener extends JessListener
                                 implements java.awt.event.MouseMotionListener
{
  public MouseMotionListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void mouseDragged(MouseEvent e) { receiveEvent(e); }
  public void mouseMoved(MouseEvent e) { receiveEvent(e); }

}
