/* **********************************************************************
 * FocusListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class FocusListener extends JessListener
                           implements java.awt.event.FocusListener
{
  public FocusListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void focusGained(FocusEvent e) { receiveEvent(e); }
  public void focusLost(FocusEvent e) { receiveEvent(e); }
}
