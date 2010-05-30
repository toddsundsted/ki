/* **********************************************************************
 * ContainerListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class ContainerListener extends JessListener
                               implements java.awt.event.ContainerListener
{
  public ContainerListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void componentAdded(ContainerEvent e) { receiveEvent(e); }
  public void componentRemoved(ContainerEvent e) { receiveEvent(e); }

}
