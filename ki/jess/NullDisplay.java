/** **********************************************************************
 * NullDisplay.java
 * A Null JESS GUI
 * It extends observable so that it can send messages to the
 * network viewer tool. Ultra-performance-concerned folk might want to
 * remove this stuff...
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public class NullDisplay extends Observable implements ReteDisplay
{

  public void assertFact(ValueVector fact)
  {
    setChanged();
    notifyObservers("FACT");
  }
  public void retractFact(ValueVector fact)
  {
    setChanged();
    notifyObservers("FACT");
  }

  public void addDeffacts(Deffacts df) {}
  public void addDeftemplate(Deftemplate dt) {}

  public void addDefrule(Defrule rule)
  {
    setChanged();
    notifyObservers("RULE");
  }
  public void activateRule(Defrule rule) {}
  public void deactivateRule(Defrule rule) {}
  public void fireRule(Defrule rule)
  {
    setChanged();
    notifyObservers("RULE");
    System.gc();
  }

  public java.io.PrintStream stdout() {return System.out;}
  public java.io.InputStream stdin() {return System.in;}
  public java.io.PrintStream stderr() {return System.err;}
  public java.applet.Applet applet() { return null; }
}



