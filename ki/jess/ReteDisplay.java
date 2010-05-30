/** **********************************************************************
 * ReteDisplay.java
 * An interface for a Jess GUI
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public interface ReteDisplay
{

  public void assertFact(ValueVector fact);
  public void retractFact(ValueVector fact);

  public void addDeffacts(Deffacts df);
  public void addDeftemplate(Deftemplate dt);

  public void addDefrule(Defrule rule);
  public void activateRule(Defrule rule);
  public void deactivateRule(Defrule rule);
  public void fireRule(Defrule rule);

  public java.io.PrintStream stdout();
  public java.io.InputStream stdin();
  public java.io.PrintStream stderr();

  public java.applet.Applet applet();

}
