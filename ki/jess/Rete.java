/** **********************************************************************
 * Rete machine: execute the built Network
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.io.*;
import java.util.*;


public class Rete 
{

  /**
    Context for executing global functions
    */

  private GlobalContext m_globalContext;
  public final GlobalContext globalContext() { return m_globalContext; }

  /**
    Where to draw myself
    */

  private ReteDisplay m_display;
  public synchronized final ReteDisplay display() { return m_display; }

  /**
    Whether to log stuff
    */


  private boolean m_watchRules = false;
  final synchronized public void watchRules(boolean val)
  { m_watchRules = val; }
  final synchronized public boolean watchRules()
  { return m_watchRules; }

  private boolean m_watchFacts = false;
  final synchronized public void watchFacts(boolean val)
  { m_watchFacts = val; }
  final public boolean watchFacts()
  { return m_watchFacts; }

  private boolean m_watchCompilations = false;
  final synchronized public void watchCompilations(boolean val)
  { m_watchCompilations = val; }
  final public boolean watchCompilations()
  { return m_watchCompilations; }

  private boolean m_watchActivations = false;
  final synchronized public void watchActivations(boolean val)
  { m_watchActivations = val; }
  final synchronized public boolean watchActivations()
  { return m_watchActivations; }


  /**
    Successively incremented ID for asserted facts.
    */
  
  private int m_nextFactId = 0;
  synchronized int nextFactId() { return m_nextFactId++;}

  /**
    Successively incremented ID for new rules.
    */

  private int m_nextRuleId = 0;
  synchronized int nextRuleId() { return m_nextRuleId++;}

  /**
    Deftemplates are unique to each interpreter.
    */

  private Hashtable m_deftemplates = new Hashtable(101);

  /**
    Deffacts are unique to each interpreter.
    */

  private Vector m_deffacts = new Vector();

  /**
    Defglobals are unique to each interpreter.
    */

  private Vector m_defglobals = new Vector();

  /**
    Deffunctions are unique to each interpreter.
    */

  private Hashtable m_deffunctions = new Hashtable(101);

  /**
    Userfunctions are unique to each interpreter.
    */

  private Hashtable m_userfunctions = new Hashtable(101);

  /**
    Routers are kept in two hashtables: input ones and output ones.
    Names that are read-write are kept in both tables as separate entries.
    This means we don't need a special 'Router' class.

    Every input router is wrapped in a DataInputStream so we get reliable
    treatment of end-of-line. We need to keep track of the association, so
    we keep the original stream paired with the wrapper in m_inWrappers.
   */

  private Hashtable m_outRouters = new Hashtable(13);
  private Hashtable m_inRouters = new Hashtable(13);
  private Hashtable m_inWrappers = new Hashtable(13);

  public synchronized void addInputRouter(String s, InputStream is)
  { 
    DataInputStream dis;
    if (! (is instanceof DataInputStream))
      dis = new DataInputStream(is);
    else
      dis = (DataInputStream) is;
      
    m_inRouters.put(s, dis);
    m_inWrappers.put(is, dis);
  }
  public synchronized void addOutputRouter(String s, OutputStream os)
  { m_outRouters.put(s, os); }

  public synchronized void removeInputRouter(String s)
  { m_inRouters.remove(s); }
  public synchronized void removeOutputRouter(String s)
  { m_outRouters.remove(s); }

  public synchronized InputStream getInputRouter(String s)
  { return (InputStream) m_inRouters.get(s); }

  public synchronized DataInputStream getInputWrapper(InputStream is)
  { return (DataInputStream) m_inWrappers.get(is); }

  public synchronized OutputStream getOutputRouter(String s)
  { return (OutputStream) m_outRouters.get(s); }

  /**
    The fact-list is unique to each interpreter.
    */
  
  private Vector m_facts = new Vector();

  /**
    The rule base is unique to each interpreter.
    */

  private Hashtable m_rules = new Hashtable();

  /**
    The agenda is unique to each interpreter.
    */

  private Vector m_activations = new Vector();

  /**
    Each interpreter has its own compiler object
    */

  private ReteCompiler m_compiler = new ReteCompiler(this);

  public synchronized final ReteCompiler compiler() { return m_compiler; }

  /**
    Flag for (halt) function
    */

  private boolean m_halt;

  /**
    Stuff to help us parse Jess code
    */

  private TextInputStream m_tis;
  private Jesp  m_jesp;

  /**
    Constructor
    */

  public Rete(ReteDisplay display)  
  {
    m_display = display;
    m_globalContext = new GlobalContext(this);

    // set up default routers
    addInputRouter("t", display.stdin());
    addOutputRouter("t", display.stdout());
    addInputRouter("WSTDIN", getInputRouter("t"));
    addOutputRouter("WSTDOUT", getOutputRouter("t"));
    addOutputRouter("WSTDERR", getOutputRouter("t"));

    m_tis = new TextInputStream(true);
    m_jesp = new Jesp(m_tis, this);
  }

  public synchronized PrintStream errStream()
  {
    // Coerce to PrintStream;
    PrintStream ps;
    OutputStream os = getOutputRouter("WSTDERR");
    if (os instanceof PrintStream)
      ps = (PrintStream) os;
    else
      {
        ps = new PrintStream(os);
        addOutputRouter("WSTDERR", ps);
      }
    return ps;
  }

  public synchronized PrintStream outStream()
  {
    // Coerce to PrintStream;
    PrintStream ps;
    OutputStream os = getOutputRouter("WSTDOUT");
    if (os instanceof PrintStream)
      ps = (PrintStream) os;
    else
      {
        ps = new PrintStream(os);
        addOutputRouter("WSTDOUT", ps);
      }
    return ps;
  }

  private Vector m_clearables = new Vector();
  public synchronized void addClearable(Clearable c)
  { m_clearables.addElement(c); }
  public synchronized void removeClearable(Clearable c)
  { m_clearables.removeElement(c); }

  /*
    Reinitialize engine
    Thanks to Karl Mueller for idea
    */

  public synchronized void clear() throws ReteException
  {
    watchRules(false);
    watchFacts(false);
    watchCompilations(false);
    watchActivations(false);
    m_halt = false;
    m_nextFactId = m_nextRuleId = 0;
    m_deftemplates.clear();
    m_deffacts.removeAllElements();
    m_defglobals.removeAllElements();
    m_deffunctions.clear();

    Enumeration e = m_clearables.elements();
    while (e.hasMoreElements())
      ((Clearable) e.nextElement()).clear();

    m_facts.removeAllElements();
    m_compiler = new ReteCompiler(this);
    if (m_rules.size() != 0)
      m_display.addDefrule((Defrule) m_rules.elements().nextElement());
    m_rules.clear();
    m_activations.removeAllElements();
    System.gc();
  }

  private Vector m_resetables = new Vector();
  public synchronized void addResetable(Resetable c)
  { m_resetables.addElement(c); }
  public synchronized void removeResetable(Clearable c)
  { m_resetables.removeElement(c); }

  /**
    Reset the interpreter. Remove all facts, flush the network,
    remove all activations.
    */

  void removeFacts() throws ReteException
  {
    synchronized (m_compiler)
      {
        // remove all existing facts
        // This Token tag is a special command. All 1-input nodes
        // just pass it along; all two-input nodes clear both memories.
        
        Fact nf = new Fact("*CLEAR*", RU.ORDERED_FACT, this);
        ValueVector vv = nf.factData();
        Token t = new Token(RU.CLEAR, vv);
        processToken(t);
        System.gc();
        m_facts = new Vector();
        m_display.retractFact(vv);
      }
  }

  public void reset() throws ReteException 
  {
    
    synchronized (m_compiler)
      {
        removeFacts();

        // Reset all the defglobals
        
        int size = m_defglobals.size();
        for (int i=0; i<size; i++) 
          {
            Defglobal dg = (Defglobal) m_defglobals.elementAt(i);
            int dgsize = dg.m_bindings.size();
            for (int j=0; j<dgsize; j++) 
              {
                Binding b = (Binding) dg.m_bindings.elementAt(j);
                m_globalContext.addGlobalBinding(b.m_name, b.m_val);
              }
          }
        
        m_nextFactId = 0;
        
        // assert the initial-fact
        Fact init = new Fact("initial-fact", RU.ORDERED_FACT, this);
        assert(init.factData());
        
        // assert all the deffacts
        size = m_deffacts.size();
        for (int i=0; i<size; i++) 
          {
            Deffacts df = (Deffacts) m_deffacts.elementAt(i);
            int dfsize = df.m_facts.size();
            for (int j=0; j<dfsize; j++) 
              {
                ValueVector f = (ValueVector) df.m_facts.elementAt(j);
                assert(f);
              }
          }
        Enumeration e = m_resetables.elements();
        while (e.hasMoreElements())
          ((Resetable) e.nextElement()).reset();
      }
  }

  /**
    Assert a fact, as a String
    */

  public int assertString(String s) throws ReteException 
  {
    StringBufferInputStream sbis;
    try
      {
        m_tis.clear();
        m_jesp.clear();
        m_tis.appendText(s);
        Fact f = m_jesp.parseFact();
        return assert(f.factData());
      }
    catch (Exception t)
      {
        throw new ReteException("Rete::AssertString", t.toString(), s);
      }
  }

  /**
    Assert a fact, as a ValueVector
    */

  public int assert(ValueVector f) throws ReteException 
  {

    synchronized (m_compiler)
      {
        // First, expand any multifields if ordered fact
        if ( f.get(RU.DESC).descriptorValue() == RU.ORDERED_FACT)
          for (int i=0; i<f.size(); i++)
            if (f.get(i).type() == RU.LIST) 
              {
                ValueVector nf = new ValueVector(f.size());
                for (int j=0; j<f.size(); j++) 
                  {
                    Value v = f.get(j);
                    if (v.type() != RU.LIST)
                      nf.add(v);
                    else 
                      {
                    ValueVector vv = v.listValue();
                    for (int k=0; k < vv.size(); k++) 
                      {
                        nf.add(vv.get(k));
                      }
                      }
                  }
                f = nf;
                break;
              }
        
        // find any old copy
        ValueVector of = findFact(f);
        
        if (of != null)
          return -1;
        
        // insert the new fact
        f.set(new Value(m_nextFactId++, RU.FACT_ID), RU.ID);
        
        if (f.get(RU.DESC).descriptorValue() == RU.UNORDERED_FACT) 
          {
            // deal with any default values
            ValueVector deft = null;

            for (int i=RU.FIRST_SLOT; i< f.size(); i++) 
              {
                if (f.get(i).type() == RU.NONE) 
                  {
                    if (deft == null)
                      deft = findDeftemplate(f.get(RU.CLASS).atomValue());
                    int j = (i - RU.FIRST_SLOT)* RU.DT_SLOT_SIZE +
                      RU.FIRST_SLOT + RU.DT_DFLT_DATA;
                    Value dtd = deft.get(j);
                    if (dtd.type() == RU.NONE)
                      f.set(Funcall.NIL(), i);
                    else 
                      f.set(dtd, i);
                  }
              }          
          }
        
        m_facts.addElement(f);
        Token t = new Token(RU.ADD, f);
        processToken(t);
        
        m_display.assertFact(f);
        
        if (m_watchFacts)
          outStream().println(" ==> "  +
                              new Fact(f,this).toString());

        return f.get(RU.ID).factIDValue();
      }
  }

  /** ***********************************************************
   * Karl Mueller NASA/GSFC Code 522.2 
   * (Karl.R.Mueller@gsfc.nasa.gov)
   * 27.January.1998
   *
   * Retract a fact as a string
   * ************************************************************/

  public void retractString(String s) throws ReteException 
  {
    try
      {
        m_tis.clear();
        m_jesp.clear();
        m_tis.appendText(s);
        Fact f = m_jesp.parseFact();
        retract(f.factData());
      }
    catch (Exception t)
      {
        throw new ReteException("Rete::retractString", t.toString(), s);
      }
  }
  

  /**
    Retract a fact, as a ValueVector
    */

  public void retract(ValueVector f) throws ReteException 
  {
    synchronized (m_compiler)
      {
        ValueVector found;
        if ((found = findFact(f)) != null) 
          _retract(found);
      }
  }
  
  /**
    Retract a fact by ID, used by rule RHSs.
    */

  public void retract(int id) throws ReteException 
  {
    synchronized (m_compiler)
      {
        ValueVector found;
        if ((found = findFactByID(id)) != null)
          _retract(found);
      }
  }

  /*
    Do the actual retracting - f MUST be a valid fact
    */

  void _retract(ValueVector f) throws ReteException
  {
    synchronized (m_compiler)
      {
        m_facts.removeElement(f);
        
        Token t = new Token(RU.REMOVE, f);
        processToken(t);
        
        if (m_watchFacts)
          outStream().println(" <== "  +
                              " " + new Fact(f,this).toString());
        m_display.retractFact(f);      

      }    
  }

  /**
    This 'find' is used by the retract that rules use.
    */

  ValueVector findFactByID(int id) throws ReteException 
  {
    int size = m_facts.size();
    for (int i=0; i<size; i++) 
      {
        ValueVector tf = (ValueVector) m_facts.elementAt(i);
        if (tf.get(RU.ID).factIDValue() == id)
          return tf;
      }
    return null;
  }

  /**
    Does a given fact (as a ValueVector) exist? (We're looking for identical
    data, but the ID can differ)
    */

  private ValueVector findFact(ValueVector f) throws ReteException 
  {
    int id = f.get(RU.ID).factIDValue();
    int size = m_facts.size();
    int fsize = f.size();
  outer_loop:
    for (int i=0; i < size; i++) 
      {
        ValueVector tf = (ValueVector) m_facts.elementAt(i);
        if (fsize != tf.size())
          continue;
        if (!f.get(RU.CLASS).equals(tf.get(RU.CLASS)))
          continue;
        for (int j=RU.FIRST_SLOT; j < fsize; j++) 
          {
            if (!f.get(j).equals(tf.get(j)))
              continue outer_loop;
          }
        return tf;
      }
    return null;
  }

  /**
    What the (rules) command calls
    */

  public synchronized void showRules() 
  {
    Enumeration e = m_rules.elements();
    while (e.hasMoreElements())
      {
        Defrule dr = (Defrule) e.nextElement();
        outStream().println(dr.toString());
      }
    outStream().println("For a total of " + m_rules.size() + " rules.");
  }
  
  /** Return the pretty print forms of all facts, as a big string */

  public synchronized String ppFacts(int name) 
  {
    int size = m_facts.size();
    StringBuffer sb = new StringBuffer(100);
    Fact fact = null;
    for (int i=0; i<size; i++) 
      {
        ValueVector f = (ValueVector) m_facts.elementAt(i);
        try 
          {
            if (f.get(RU.CLASS).atomValue() != name) continue;
            fact = new Fact(f, this);
          }
        catch (ReteException re) 
          {
            continue;
          }
        sb.append(fact);
        sb.append("\n");
      }
    return sb.toString();
  }

  public synchronized String ppFacts() throws ReteException
  {
    int size = m_facts.size();
    StringBuffer sb = new StringBuffer(1024);
    Fact fact = null;
    for (int i=0; i<size; i++) 
      {
        ValueVector f = (ValueVector) m_facts.elementAt(i);
        fact = new Fact(f, this);
        sb.append(fact);
        sb.append("\n");
      }
    return sb.toString();
  }


  /**
    What the (facts) command calls
    */

  public void showFacts() throws ReteException 
  {
    synchronized (m_compiler)
      {
        int size = m_facts.size();
        for (int i=0; i<size; i++) 
          {
            ValueVector f = (ValueVector) m_facts.elementAt(i);
            Fact fact = new Fact(f, this);
            outStream().print("f-");
            outStream().print(f.get(RU.ID).factIDValue() + "   ");
            outStream().println(fact.toString());
          }
        outStream().println("For a total of " + size + " facts.");
      }
  }
  
  /** 
    These methods are to be used primarily for debugging purposes.
    Don't get too chummy with the way they work - they will change without
    notice.   
    */

  public synchronized Enumeration listDeffacts()
  { return m_deffacts.elements(); }

  public synchronized Enumeration listDeftemplates()
  { return m_deftemplates.elements(); }

  public synchronized Enumeration listDefrules()
  { return m_rules.elements(); }

  public synchronized Enumeration listFacts()
  { return m_facts.elements(); }

  public synchronized Enumeration listActivations()
  { return m_activations.elements(); }

  public synchronized Enumeration listDefglobals()
  { return m_defglobals.elements(); }

  public synchronized Enumeration listDeffunctions()
  { return m_deffunctions.elements(); }

  public synchronized Enumeration listUserfunctions()
  { return m_userfunctions.elements(); }

  
  /**
    Process a Token which represents a fact being added or removed.
    Eventually we should set this up so that the token gets dispatched
    to only one root of the pattern net - right now it presented to all of
    them!
    
    */

  private boolean processTokenOneNode(Token t, Node n) throws ReteException
  {
    synchronized (m_compiler)
      {
        return n.callNode(t, Node.SINGLE);
      }
  }
       
  private boolean processToken(Token t) throws ReteException 
  {
    synchronized (m_compiler)
      {
        Vector v = m_compiler.roots();
        
        // make sure the network is optimized
        m_compiler.freeze();
        
        int size = v.size();
        for (int i=0; i < size; i++)
          {
            Node1 n = (Node1) ((Successor) v.elementAt(i)).m_node;
            if (processTokenOneNode(t, n))
              return true;
          }
        return false;
      }
  }

  /**
    Present all the facts on the agenda to a single Node.
    */

  void updateNode(Node n) throws ReteException 
  {
    for (int i=0; i < m_facts.size(); i++) 
      {
        Token t = new Token(RU.UPDATE, (ValueVector) m_facts.elementAt(i));
        processTokenOneNode(t, n);
      }
  }

  void updateNodes(Node[] n) throws ReteException 
  {    
    m_compiler.freeze();
    for (int i=0; i < m_facts.size(); i++) 
      {
        Token t = new Token(RU.UPDATE, (ValueVector) m_facts.elementAt(i));
        for (int j=0; j<n.length; j++)
          {
            processTokenOneNode(t, n[j]);
          }
      }
  }

  /**
    Find a defrule object with a certain name
    */

  public final Defrule findDefrule(String name) 
  {
    return (Defrule) m_rules.get(name);
  }

  public final Defrule findDefrule(int code) 
  {
    String name = RU.getAtom(code);
    return findDefrule(name);
  }

  /**
    Find a deftemplate object with a certain name
    */

  public ValueVector findDeftemplate(String name) 
  {
    Deftemplate dt = (Deftemplate) m_deftemplates.get(name);
    if (dt != null)
      return dt.deftemplateData();
    else
      return null;
  }

  Deftemplate findFullDeftemplate(String name) 
  {
    return ((Deftemplate) m_deftemplates.get(name));
  }

  public ValueVector findDeftemplate(int code) 
  {
    String name = RU.getAtom(code);
    return findDeftemplate(name);
  }

  /**
    Creates a new deftemplate in this object. 
    Ensure that every deftemplate has a unique class name
    */

  public synchronized ValueVector addDeftemplate(Deftemplate dt)
       throws ReteException 
  {
    ValueVector dtia = dt.deftemplateData();
    String name = dtia.get(RU.CLASS).stringValue();
    if (m_deftemplates.get(name) == null) 
      {
        m_deftemplates.put(name, dt);
        m_display.addDeftemplate(dt);
      }
    return dtia;
  }

  /**
    Creates a new deffacts in this object
    Can now redefine deffacts.
    */

  public synchronized Deffacts addDeffacts(Deffacts df) throws ReteException 
  {
    try
      {
        for (int i=0; i<m_deffacts.size(); i++) 
          {
            if (df.m_name == ((Deffacts) m_deffacts.elementAt(i)).m_name)
              {
                m_deffacts.setElementAt(df, i);
                return df;
              }
          }
        m_deffacts.addElement(df);
        return df;
      }
    finally
      {
        m_display.addDeffacts(df);
      }
  }

  /**
    Creates a new Defglobal in this object
    */

  public synchronized Defglobal addDefglobal(Defglobal dg) throws ReteException 
  {
    m_defglobals.addElement(dg);

    // Set up the variables now
    int dgsize = dg.m_bindings.size();
    for (int j=0; j<dgsize; j++) 
      {
        Binding b = (Binding) dg.m_bindings.elementAt(j);
        m_globalContext.addGlobalBinding(b.m_name, b.m_val);
      }
    
    return dg;
  }

  /**
    Creates a new deffunction in this object
    Will happily destroy an old one.
    */

  public synchronized Deffunction addDeffunction(Deffunction df)
       throws ReteException 
  {
    String name = RU.getAtom(df.name());
    m_deffunctions.put(name, df);
    return df;
  }

  /**
    Find a deffunction, if there is one.
    */

  public Deffunction findDeffunction(String name) 
  {
    return (Deffunction) m_deffunctions.get(name);
  }

  public Deffunction findDeffunction(int code) 
  {
    String name = RU.getAtom(code);
    return findDeffunction(name);
  }

  /**
    Creates a new userfunction in this object
    Will happily destroy an old one.
    */

  public synchronized Userfunction addUserfunction(Userfunction uf) 
  {
    String name = RU.getAtom(uf.name());
    m_userfunctions.put(name, uf);
    return uf;
  }

  /**
    A package simply calls AddUserfunction lots of times.
    */

  public synchronized Userpackage addUserpackage(Userpackage up) 
  {
    up.add(this);
    return up;
  }

  /**
    Find a userfunction, if there is one.
    */

  public Userfunction findUserfunction(String name) 
  {
    return (Userfunction) m_userfunctions.get(name);
  }

  public Userfunction findUserfunction(int code) 
  {
    String name = RU.getAtom(code);
    return findUserfunction(name);
  }


  /**
    Creates a new defrule in this object
    */

  public final Defrule addDefrule(Defrule dr) throws ReteException 
  {
    synchronized (m_compiler)
      {
        unDefrule(dr.name());
        
        m_compiler.addRule(dr);
        m_rules.put(RU.getAtom(dr.name()), dr);
        m_display.addDefrule(dr);
        return dr;
      }
  }

  public final Value unDefrule(int name) throws ReteException 
  {
    synchronized (m_compiler)
      {
        Defrule odr = findDefrule(name);
        if (odr != null)
          {
            odr.remove(m_compiler.roots());
            m_rules.remove(RU.getAtom(name));
            for (int i=0; i<m_activations.size(); i++)
              {
                Activation a = (Activation) m_activations.elementAt(i);
                if (a.m_rule == odr)
                  {
                    standDown(a);
                    // Removed an element from the vector we're iterating on
                    --i;
                  }
              }
            
            m_display.addDefrule(odr);
            return Funcall.TRUE();
          }
      }

    return Funcall.FALSE();
  }

  /**
    Info about a rule to fire.
    */

  void addActivation(Activation a) throws ReteException 
  {
    m_display.activateRule(a.m_rule);
    m_activations.addElement(a);
    if (m_watchActivations)
      outStream().println("==> Activation: " +
                               RU.getAtom(a.m_rule.m_name) + " : " +
                               factList(a.m_token));
  }

  /**
    An activation has been cancelled; forget it
    */

  void standDown(Activation a) throws ReteException 
  {
    ruleFired(a);
    if (m_watchActivations)
      outStream().println("<== Activation: " +
                                 RU.getAtom(a.m_rule.m_name) + " : " +
                                 factList(a.m_token));
  }

  /**
    An activation has been fired; forget it
    */

  void ruleFired(Activation a) 
  {
    m_display.deactivateRule(a.m_rule);
    m_activations.removeElement(a);
  }

  /**
    Return a string describing a list of facts
    */
  
  static String factList(Token t) throws ReteException 
  {
    StringBuffer sb = new StringBuffer(100);
    boolean first = true;
    for (int i=0; i<t.size(); i++) 
      {
        if (!first)
          sb.append(", ");
        int id = t.fact(i).get(RU.ID).factIDValue();
        if (id != -1)
          {
            sb.append("f-");
            sb.append(id);
          }
        first = false;
      }
    return sb.toString();
  }

  /**
    Run the actual engine.
    */

  public int run() throws ReteException
  {
    int i=0, j;
    do
      {
        j = run(Integer.MAX_VALUE);
        i += j;
      }
    while (j > 0 && !m_halt);
    return i;
  }

  public int run(int max) throws ReteException 
  {
    int n = 0;
    int size = 0;
    m_halt = false;
    synchronized (this)
      {
        size = m_activations.size();
      }
    
    while (size > 0 && !m_halt && n < max) 
      {
        synchronized (this)
          {
            int bs = 0, rs, ms = Integer.MIN_VALUE;
            for (int i=size-1; i> -1; i--) 
              {
                rs =
                  ((Activation) m_activations.elementAt(i)).m_rule.m_salience;
                if (rs > ms) 
                  {
                    ms = rs;
                    bs = i;
                  }
              }
            Activation a = (Activation) m_activations.elementAt(bs);
            a.fire();
            ++n;
            size = m_activations.size();
          }
      }
    return n;
  }

  /**
    Stuff to let Java code call functions inside of us.
    */


  public synchronized Value executeCommand(String cmd) throws ReteException 
  {
    m_tis.clear();
    m_jesp.clear();
    m_tis.appendText(cmd);
    return m_jesp.parse(false);
  }

  /**
    Jane, stop this crazy thing!
    */

  public synchronized void halt() 
  {
    m_halt = true;
  }

}

