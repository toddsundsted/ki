/* **********************************************************************
 * ActionListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class ActionListener extends JessListener
                            implements java.awt.event.ActionListener
{
  public ActionListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void actionPerformed(ActionEvent e) { receiveEvent(e); }
}
