/** **********************************************************************
 * Main.java
 * An applet and a command line driver for testing the jess package
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
********************************************************************** */

package jess;

import java.awt.*;
import java.io.*;
import java.applet.*;
import java.net.*;

public class Main extends Applet implements Runnable
{
  
  final private static int ROWS = 10;
  final private static int COLS = 60;    

  LostDisplay m_ld;
  String m_filename;
  Rete m_rete;
  Jesp m_jesp;
  PrintStream m_out;
  TextArea m_ta = null;
  Button m_bananas, m_grapes;
  String m_goal = "bananas";
  boolean m_firstTime = true;
  boolean m_running = false;

  public void init()
  {
    setLayout(new BorderLayout());
    
    m_ta = new TextArea(ROWS, COLS);
    m_ta.setEditable(false);
    add("South", m_ta);
    m_out = new PrintStream(new TextAreaOutputStream(m_ta), true);

    Panel p = new Panel();
    p.setLayout(new BorderLayout());
    add("Center", p);
    Panel pp = new Panel();
    pp.setLayout(new FlowLayout());
    p.add("East",pp);
    pp.add(m_bananas=new Button("Find Bananas"));
    pp.add(m_grapes=new Button("Find Grapes"));

    
    m_ld = new LostDisplay(m_out, System.in, this);
    p.add("Center", m_ld);

    m_rete = new Rete(m_ld);
    m_filename = getParameter("INPUT");
    if (m_filename == null)
      m_filename = "examples/mab.clp";

    repaint();
  }

  public void start()
  {
    new Thread(this).start();
  }

  public void run()
  {
    if (m_running)
      return;
    m_running = true;
    InputStream fis;
    try
      {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      }
    catch (SecurityException t) {}

    if (m_firstTime) {
      // read in the given problem

      try
        {
          fis = new URL(getDocumentBase(), m_filename).openStream();
        }
      catch (Throwable ex)
        {
          showStatus("File not found or cannot open file");
          m_running = false;
          return;
        }
      
      if (fis != null)
        {
          m_jesp = new Jesp(fis, m_rete);
          try
            {
              m_jesp.parse(false);
              m_firstTime = false;
              
            }
          catch (ReteException re)
            {
              m_rete.errStream().println(re);
            }
          
          try
            {
              fis.close();
            }
          catch (IOException ex) {/* so? */}
        }
    }
    
    try
      {
        m_rete.executeCommand("(reset)");
        m_rete.executeCommand("(assert (goal-is-to (action eat) (argument-1 " +
                              m_goal + " )))");
        m_rete.executeCommand("(run)");
      }
    catch (Throwable ex)
      {
        System.out.println(ex.getMessage());
        ex.printStackTrace();
        showStatus("Error while running");
        m_running = false;
        return;
      }
    m_running = false;
  }
  
  public void stop()
  {
  }

  public void update(Graphics g)
  {
    paint(g);
  }
  
  public void paint(Graphics g)
  {
    m_ld.paint(g);
  }
  
  public boolean action(Event e, Object o) 
  {
    if (e.target == m_bananas)
      {
        m_goal = "bananas";
        start();
        return true;
      }
    else if (e.target == m_grapes)
      {
        m_goal = "grapes";
        start();
        return true;
      }
    return false;
  }
  
  public static void main(String[] argv) 
  {
    System.runFinalizersOnExit(true);

    NullDisplay nd = new NullDisplay();
    Rete rete = new Rete(nd);

    // Print banner
    System.out.println("\nJess, the Java Expert System Shell");
    System.out.println("Copyright (C) 1998 E.J. Friedman Hill"
                       + " and the Sandia Corporation");
    try
      {
        rete.executeCommand("(printout t (jess-version-string) crlf crlf)");
      } 
    catch(ReteException re) {}


    // Load in optional packages, but don't fail if any are missing.
    String [] packages = { "jess.StringFunctions",
                           "jess.PredFunctions",
                           "jess.MultiFunctions", 
                           "jess.MiscFunctions",
                           "jess.MathFunctions",
                           "jess.BagFunctions",
                           "jess.reflect.ReflectFunctions",
                           "jess.view.ViewFunctions" };

    for (int i=0; i< packages.length; i++)
      {
        try
          {
            rete.addUserpackage((Userpackage)
                                Class.forName(packages[i]).newInstance());
          }
        catch (Throwable t) { /* Optional package not present */ }
      }

    InputStream fis = System.in;
    String name = argv.length == 0 ? null : argv[0];

    try
      {
        if (name != null)
          fis = new FileInputStream(name);
      }
    catch (IOException ioe) 
      {
        rete.errStream().println("File not found or cannot open file:" +
                                 ioe.getMessage());
        System.exit(0);
      }

    if (fis != null) 
      {
        Jesp j = new Jesp(fis, rete);
        do 
          {
            try
              {
                // Argument is 'true' for prompting, false otherwise
                j.parse(fis == System.in);
              }
            catch (ReteException re) 
              {
                re.printStackTrace(rete.errStream());
              }
            catch (ArrayIndexOutOfBoundsException ai) 
              {
                rete.errStream().println("Wrong number of args in funcall?");
                ai.printStackTrace(rete.errStream());
              }
            catch (NullPointerException npe) 
              {
                rete.errStream().println("Wrong number of args in funcall?");
                npe.printStackTrace(rete.errStream());
              }
            catch (Exception e) 
              {
                rete.errStream().println("Unexpected exception:");
                e.printStackTrace(rete.errStream());
              }
          }
        // Loop if we're using the command line
        while (fis == System.in);
      }
  }
}
