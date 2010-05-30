/* **********************************************************************
 * ItemListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class ItemListener extends JessListener
                          implements java.awt.event.ItemListener
{
  public ItemListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void itemStateChanged(ItemEvent e) { receiveEvent(e); }

}
