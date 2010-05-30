/** **********************************************************************
 * QuizDisplay.java
 * A basic question-and-answer dialog GUI
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.awt.*;
import java.applet.*;
import java.util.*;
import java.io.*;

// //////////////////////////////////////////////////////////////////////
// This is a 'triple-purpose' class. It is:
//
// 1) A Panel containing input and output text areas that can be plugged
//    into Jess. It uses the TextInputStream and TextAreaOutputStream classes
//    to turn textual data into I/O streams.
//
// 2) An Applet which displays this panel and Jess in a Web browser. It could
//    serve as the basis for any number of 'interview' style Expert System
//    GUIs. (The init(), run, start and stop methods are the applet.
//    If you embed QuizDisplay in a page and give it an INPUT applet param,
//    it will find the named document relative to the applet's document base
//    and interpret the file in batch mode.
//
// 3) An Application which displays the same Panel. main() is the application.
//
// //////////////////////////////////////////////////////////////////////

public class QuizDisplay extends Applet implements Runnable
{
  // Members used for presenting output
  TextArea m_ta;
  PrintStream m_out;
  // Members used for getting input
  TextField m_tf;
  // TextInputStream is in the jess package
  TextInputStream m_in = new TextInputStream();
  // The actual ReteDisplay
  QuizReteDisplay m_qrd;
  // The inference engine
  Rete m_rete = null;
  // A parser
  Jesp m_jesp = null;
  
  // ************************************************************
  // When the user hits RETURN in the text field, attach the text to the
  // stream and clear the field.
  // ************************************************************

  public boolean action(Event e, Object o)
  {
    if (e.target == m_tf)
      {
        synchronized (m_ta)
          {  
            m_out.println(m_tf.getText());
          }

        m_in.appendText(m_tf.getText());
        m_in.appendText("\n");
        m_tf.setText("");    
        return true;
      }
    return false;
  }

  // ************************************************************
  // Following are the applet methods: init(), start(), run(), stop()
  // Instead of a constructor, we give this class an init() method.
  // ************************************************************

  public void init()
  {
    setLayout(new BorderLayout());
    add("Center", m_ta = new TextArea(10, 40));
    add("South", m_tf = new TextField(40));
    m_ta.setEditable(false);
    m_out = new PrintStream(new TextAreaOutputStream(m_ta), true);
    m_qrd = new QuizReteDisplay(this);
    m_rete = new Rete(m_qrd);
    m_jesp = new Jesp(m_qrd.stdin(), m_rete);

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
            m_rete.addUserpackage((Userpackage)
                                Class.forName(packages[i]).newInstance());
          }
        catch (Throwable t) { /* Optional package not present */ }
      }
    
    try
      {
        m_rete.executeCommand("(printout t \"Jess, the Java Expert System " +
                              "Shell\" crlf)");
        m_rete.executeCommand("(printout t \"Copyright (C) 1998 E.J. " +
                              "Friedman-Hill and  the Sandia Corporation\" crlf)");
        
        m_rete.executeCommand("(printout t (jess-version-string) crlf crlf)");
        m_rete.executeCommand("(printout t \"Jess output appears in this " +
                              "window.\" crlf)");
        m_rete.executeCommand("(printout t \"Type your input in the " +
                              "one-line input box below.\" crlf)");

        try
          {
            String appParam = getParameter("INPUT");
            if (appParam != null)
              setCommand("(batch " + appParam + ")");
          }
        catch (Throwable t) { /* whatever */ }

      }
    catch (ReteException re)
      {
        m_qrd.stdout().println("ERROR DURING SETUP: " + re.toString());
      }
    resize(500,300);
  }

  private String m_cmd = null;

  public synchronized void setCommand(String cmd)
  {
    m_cmd = cmd;
  }

  private Thread m_thread = null;
  public synchronized void run()
  {
    try
      {
        if (m_cmd != null)
          {
            m_rete.executeCommand(m_cmd);
            m_cmd = null;
          }
        else
          {
            // Loop and run commands.
            do 
              {
                try
                  {
                    m_jesp.parse(true);
                  }
                catch (ReteException re) 
                  {
                    re.printStackTrace(m_rete.errStream());
                  }
                catch (ArrayIndexOutOfBoundsException ai) 
                  {
                    m_rete.errStream().println("Wrong # of args in funcall?");
                    ai.printStackTrace(m_rete.errStream());
                  }
                catch (NullPointerException npe) 
                  {
                    m_rete.errStream().println("Wrong # of args in funcall?");
                    npe.printStackTrace(m_rete.errStream());
                  }
                catch (Exception e) 
                  {
                    m_rete.errStream().println("Unexpected exception:");
                    e.printStackTrace(m_rete.errStream());
                  }
              }
            while (true);
          }          
      }
    catch (Throwable t) { m_rete.errStream().println(t); }
    m_thread = null;
  }

  public void start()
  {
    if (m_thread == null)
      {
        m_thread = new Thread(this);
        m_thread.start();
      }
  }

  public void stop()
  {
    if (m_thread != null)
      {
        m_thread.stop();
        m_thread = null;
      }
  }

  // ************************************************************
  // Here is a very trivial main() for an application that uses this display
  // ************************************************************

  public static void main(String [] argv)
  {
    QuizFrame f = new QuizFrame("Jess Console");    
    f.m_p.m_qrd.setNotApplet();
    if (argv.length != 0)
      f.m_p.setCommand("(batch " + argv[0] + ")");
    f.m_p.run();
  }
}

// **********************************************************************
// This is the actual ReteDisplay class
// It has intimate knowledge of the QuizDisplay class's innards.
// **********************************************************************

class QuizReteDisplay extends Observable implements ReteDisplay
{
  QuizDisplay m_p;
  boolean m_applet = true;

  public QuizReteDisplay(QuizDisplay qd)
  {
    m_p = qd;
  }

  void setNotApplet() { m_applet = false; }

  // ////////////////////////////////////////////////////////////
  // Here we hand out our specialized streams
  // ////////////////////////////////////////////////////////////
  public java.io.PrintStream stdout()
  { return m_p.m_out;} 
  public java.io.PrintStream stderr()
  { return m_p.m_out;} 
  public java.io.InputStream stdin() { return m_p.m_in; }

  public java.applet.Applet applet() { return m_applet ? m_p : null; }

  // ////////////////////////////////////////////////////////////
  // All this notification stuff is for the benefit of the 'View' command.
  // ////////////////////////////////////////////////////////////

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

}

// **********************************************************************
// This is a trivial Frame subclass that holds a QuizDisplay and
// a Quit button in a window, when QuizDisplay is used as an application.
// **********************************************************************

class QuizFrame extends Frame
{
  QuizDisplay m_p;
  QuizFrame(String title)
  {
    super(title);
    m_p = new QuizDisplay();
    m_p.init();
    add("Center",m_p);
    Button b;
    add("South", b = new Button("Quit"));
    resize(500,300);
    validate();
    show();
  }

  public boolean action(Event e, Object obj)
  {
    if (obj.equals("Quit"))
      {        
        dispose();
        System.exit(0);
        return true;
      }
    return false;
  }
}
