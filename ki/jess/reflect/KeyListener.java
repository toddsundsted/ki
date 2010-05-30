/* **********************************************************************
 * KeyListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class KeyListener extends JessListener
                         implements java.awt.event.KeyListener
{

  public KeyListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void keyPressed(KeyEvent e) { receiveEvent(e); }
  public void keyReleased(KeyEvent e) { receiveEvent(e); }
  public void keyTyped(KeyEvent e) { receiveEvent(e); }
}
