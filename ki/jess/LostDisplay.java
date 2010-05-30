/** **********************************************************************
 * A Fancy Jess GUI that looks like Lost in Space
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.awt.*;
import java.io.*;
import java.applet.*;

public class LostDisplay extends Canvas implements ReteDisplay
{

  Color[][] m_colors;
  Rectangle[][] m_locations;
  PrintStream m_os;
  InputStream m_is;
  Applet m_app;

  public final static int COLS = 25;

  /**
    Constructor
    */

  public LostDisplay(PrintStream os, InputStream is, Applet app) 
  {
    m_app = app;
    priv_init(os,is);
  }

  public LostDisplay(PrintStream os, InputStream is) 
  {
    m_app = null;
    priv_init(os,is);
  }
  
  private void priv_init (PrintStream os, InputStream is) 
  {
    m_os = os;
    m_is = is;

    // initialize the colors of the panel lights
    m_colors = new Color[3][COLS];
    for (int i=0; i<3; i++)
      for (int j=0; j<COLS; j++)
        m_colors[i][j] = Color.black;
    
    // initialize the locations of the panel lights
    m_locations = new Rectangle[3][COLS];
    for (int i=0; i<3; i++)
      for (int j=0; j<COLS; j++)
        m_locations[i][j] = 
          new Rectangle(j*15 + 1, i*15 + 1, 13, 13);
  }  
  public void update(Graphics g) 
  {
    paint(g);
  }

  private void sleep(int ms) 
  {
    try 
      {
        Thread.sleep(ms);
      }
    catch (java.lang.InterruptedException ex) 
      {
        // so?
      }
  }

  public void paint(Graphics g) 
  {
    Color oldColor = g.getColor();
    for (int i=0; i<3; i++)
      for (int j=0; j<COLS; j++) 
        {
          Rectangle r = m_locations[i][j];
          g.setColor(m_colors[i][j]);
          g.fillRect(r.x, r.y, r.width, r.height);
        }
    g.setColor(oldColor);
  }

  public void assertFact(ValueVector fact) 
  {
    int id;
    try 
      {
        id = fact.get(RU.ID).factIDValue();
      }
    catch (ReteException re) 
      {
        id = 0;
      }
    id = id % COLS;
    if (m_colors[0][id] == Color.red)
      m_colors[0][id] = Color.green;
    else
      m_colors[0][id] = Color.red;
    repaint();
    sleep(5);
  }

  public void retractFact(ValueVector fact) 
  {
    int id;
    try 
      {
        id = fact.get(RU.ID).factIDValue();
      } catch (ReteException re) 
        {
          id = 0;
        }
      id = id % COLS;
      if (m_colors[0][id] == Color.green)
        m_colors[0][id] = Color.red;
      else
        m_colors[0][id] = Color.green;
      repaint();
      sleep(5);
  }

  public void addDeffacts(Deffacts df) 
  {
    int id = df.name() > 0 ? df.name() : -df.name();
    id = id % COLS;
    if (m_colors[1][id] == Color.yellow)
      m_colors[1][id] = Color.red;
    else
      m_colors[1][id] = Color.yellow;
    repaint();
    sleep(5);
  }
  public void addDeftemplate(Deftemplate dt) 
  {
    int id = dt.name() > 0 ? dt.name() : -dt.name();
    id = id % COLS;
    if (m_colors[1][id] == Color.green)
      m_colors[1][id] = Color.yellow;
    else
      m_colors[1][id] = Color.green;
    repaint();
    sleep(5);
  }

  public void addDefrule(Defrule rule) 
  {
    int id = rule.name() > 0 ? rule.name() : -rule.name();
    id = id % COLS;
    if (m_colors[1][id] == Color.blue)
      m_colors[1][id] = Color.red;
    else
      m_colors[1][id] = Color.blue;
    repaint();
    sleep(5);
  }

  public void activateRule(Defrule rule) 
  {
    int id = rule.name() > 0 ? rule.name() : -rule.name();
    id = id % COLS;
    if (m_colors[2][id] == Color.red)
      m_colors[2][id] = Color.blue;
    else
      m_colors[2][id] = Color.red;
    repaint();
    sleep(5);
  }
  public void deactivateRule(Defrule rule) 
  {
    int id = rule.name() > 0 ? rule.name() : -rule.name();
    id = id % COLS;
    if (m_colors[2][id] == Color.green)
      m_colors[2][id] = Color.red;
    else
      m_colors[2][id] = Color.green;
    repaint();
    sleep(5);
  }
  public void fireRule(Defrule rule) 
  {
    int id = rule.name() > 0 ? rule.name() : -rule.name();
    id = id % COLS;
    if (m_colors[2][id] == Color.yellow)
      m_colors[2][id] = Color.blue;
    else
      m_colors[2][id] = Color.yellow;
    repaint();
    sleep(5);
    System.gc();
  }

  public PrintStream stdout() 
  {
    sleep(5);
    return m_os;
  }
  public java.io.InputStream stdin() 
  {
    sleep(5);
    return m_is;
  }

  public PrintStream stderr() 
  {
    sleep(5);
    return m_os;
  }

  public Applet applet() 
  {
    return m_app;
  }
}


