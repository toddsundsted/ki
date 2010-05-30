/* **********************************************************************
 * TextListener
 * An AWT Event Adapter for Jess
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 * $Id$
 ********************************************************************** */

package jess.reflect;
import java.awt.*;
import java.awt.event.*;
import jess.*;

public class TextListener extends JessListener
                          implements java.awt.event.TextListener
{
  public TextListener(String uf, Rete engine) throws ReteException
  {
    super(uf, engine);
  }      

  public void textValueChanged(TextEvent e) { receiveEvent(e); }
}
