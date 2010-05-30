/** **********************************************************************
 * A class for parsing, assembling, and interpreting function calls
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.io.*;
import java.net.*;
import java.applet.*;
import java.util.*;

public class Funcall extends ValueVector
{
  
  static Value s_true = null;
  static Value s_false = null;
  static Value s_nil = null;
  static Value s_else = null;
  static Value s_do = null;
  static Value s_eof = null;
  
  static private Hashtable s_intrinsics = new Hashtable(50);

  static Enumeration listIntrinsics() { return s_intrinsics.keys(); }

  // atoms for control structures
  static private int[] s_ctrl;

  static
  {
    try
      {
        s_true = new Value(RU.putAtom("TRUE"),RU.ATOM);
        s_false = new Value(RU.putAtom("FALSE"),RU.ATOM);
        s_nil = new Value(RU.putAtom("nil"),RU.ATOM);
        s_else = new Value(RU.putAtom("else"),RU.ATOM);
        s_do = new Value(RU.putAtom("do"),RU.ATOM);
        s_eof = new Value(RU.putAtom("EOF"),RU.ATOM);
      }
    catch (ReteException re)
      {
        System.out.println("*** FATAL ***: Can't instantiate constants");
        System.exit(0);
      }

    // Load in all the intrinsic functions

    String [] intlist = {"_return", "_assert","_retract", "_retract_string", 
                         "_printout", "_extract_global",
                         "_open", "_close", "_foreach",
                         "_read","_readline","_gensym_star","_while",
                         "_if","_bind","_modify","_and","_or","_not",
                         "_eq", "_eqstar", "_equals","_not_equals","_gt","_lt",
                         "_gt_or_eq","_lt_or_eq","_neq","_mod","_plus",
                         "_times","_minus","_divide","_sym_cat","_reset",
                         "_run","_facts","_rules","_halt","_exit","_clear",
                         "_watch","_unwatch","_jess_version_string",
                         "_jess_version_number","_load_facts","_save_facts",
                         "_assert_string","_undefrule"};
    
    try
      {
        for (int i=0; i< intlist.length; i++)
          {
            Userfunction uf = (Userfunction)
              Class.forName("jess." + intlist[i]).newInstance();
            s_intrinsics.put(RU.getAtom(uf.name()), uf);
          }
      }
    catch (Throwable t)
      {
        System.out.println("*** FATAL ***: Missing intrinsic function class");
        System.exit(0);
      }


    String [] ctrlNames = {"if", "while", "and", "or", "foreach"};
    s_ctrl = new int[ctrlNames.length];

    for (int i=0; i< ctrlNames.length; i++)
      s_ctrl[i] = RU.putAtom(ctrlNames[i]);
  }
  
  private Userfunction m_funcall;

  public Funcall(String name, Rete engine) throws ReteException
  {
    add(new Value(RU.putAtom(name), RU.ATOM));

    Userfunction uf = engine.findDeffunction(name);
    if (uf == null) 
      {
        uf = engine.findUserfunction(name);
        if (uf == null) 
          uf = (Userfunction) s_intrinsics.get(name);
      }

    m_funcall = uf;
  }
    

  Funcall(int size)
  {
    super(size);
  }
  
  public Object clone()
  {
    Funcall vv = new Funcall(m_ptr);
    vv.m_ptr = m_ptr;
    System.arraycopy(m_v, 0, vv.m_v, 0, m_ptr);
    vv.m_funcall = m_funcall;
    return vv;
  }

  public Funcall cloneInto(Funcall vv)
  {
    vv.setLength(m_ptr);
    System.arraycopy(m_v, 0, vv.m_v, 0, m_ptr);
    vv.m_funcall = m_funcall;
    return vv;
  }

  public final static Value TRUE()
  {
    return s_true;
  }
  
  public final static Value FALSE()
  {
    return s_false;
  }
  
  public final static Value NIL()
  {
    return s_nil;
  }
  
  
  /**
    Add an argument to this funcall
    */

  void addArgument(String value, int type) throws ReteException
  {
    addArgument(RU.putAtom(value), type);
  }

  void addArgument(int value, int type) throws ReteException
  {
    add(new Value(value, type));
  }

  void addArgument(double value, int type) throws ReteException 
  {
    add(new Value(value, type));
  }

  void addArgument(Funcall value) throws ReteException
  {
    add(new Value(value, RU.FUNCALL));
  }

  void addArgument(Fact value) throws ReteException
  {
    ValueVector vv = value.factData();
    add(new Value(vv, vv.get(RU.DESC).descriptorValue()));
  }
  
  void addArgument(ValueVector value, int type) throws ReteException
  {
    add(new Value(value, type));
  }
  
  /**
    Execute a Funcall, recursively expanding its fields in the context of a
    Defrule. 

    @returns A Value structure containing the return value, or null
    if expand_root false.
    */

  static final Value execute(Funcall vv, Context context)
       throws ReteException
  { return execute(vv, context, null, null); }

  static final Value execute(Funcall vv, Context context, EvalCache vs)
       throws ReteException
  { return execute(vv, context, vs, null); }

  static Value execute(Funcall vv, Context context, EvalCache vs, Value retval)
       throws ReteException
  {

    int omark = (vs != null ? vs.markValue() : 0);
    int size = vv.size();
    int functor = vv.get(0).atomValue();    
    
    if (!(functor == s_ctrl[0]) && !(functor == s_ctrl[1]) &&
        !(functor == s_ctrl[2]) && !(functor == s_ctrl[3]))
      {
        // for each argument
        for (int i=1; i<size; i++)
          {
            // Expand first few args of 'foreach' normally
            if (functor == s_ctrl[4] && i > 2)
              break;

            int type = vv.get(i).type();
            if (type == RU.FUNCALL)
              {
                
                // dig out the subexpression
                Funcall subexp = vv.get(i).funcallValue();
                
                // expand subexpression, keeping return value
                Value rv = (vs != null) ? vs.getValue() :
                  new Value(0, RU.INTEGER);
                int mark = (vs != null) ? vs.markValue() : 0;
                Value r = execute(subexp, context, vs, rv);
                vv.set(r,i);
                if (vs != null)
                  vs.restoreValue(mark);
                
              }
            else if (type == RU.UNORDERED_FACT ||
                     type == RU.ORDERED_FACT)
              {
                
                ValueVector fact = vv.get(i).factValue();
                for (int j=RU.FIRST_SLOT; j<fact.size(); j++)
                  {
                    if (fact.get(j).type() == RU.FUNCALL)
                      {
                        Funcall subexp = fact.get(j).funcallValue();
                        Value rv = (vs != null) ? vs.getValue() :
                          new Value(0, RU.INTEGER);
                        int mark = (vs != null) ? vs.markValue() : 0;
                        Value r = execute(subexp, context, vs, rv);
                        fact.set(r,j);
                        if (vs != null)
                          vs.restoreValue(mark);
                      }
                  }
              }
            else if (type == RU.LIST)
              {
                
                ValueVector list = vv.get(i).listValue();
                for (int j=1; j<list.size(); j++)
                  {
                    if (list.get(j).type() == RU.FUNCALL)
                      {
                        Value rv = (vs != null) ? vs.getValue() :
                          new Value(0, RU.INTEGER);
                        int mark = (vs != null) ? vs.markValue() : 0;
                        Funcall subexp = list.get(j).funcallValue();
                        Value r = execute(subexp, context, vs, rv);
                        list.set(r,j);
                        if (vs != null)
                          vs.restoreValue(mark);
                      }
                  }
                vv.set(new Value(Context.flattenList(list), RU.LIST), i);
              }
          }
      }
    // we need to expand ourselves too
    Value v = simpleExecute(vv, context, retval);
    if (vs != null)
      vs.restoreValue(omark);
    return v;
  }
  
  /**
    Execute a funcall in a particular context
    We let the Value class do all our argument type checking, leading
    to remarkably small code!

    Very simple version is the only public one.
    */
  
  public static Value simpleExecute(Funcall vv, Context context)
       throws ReteException
  { return simpleExecute(vv, context, null); }

  static Value simpleExecute(Funcall vv, Context context, Value v)
       throws ReteException
  {
    
    Userfunction uf = vv.m_funcall;
    if (uf == null)
      {
        Rete engine = context.m_engine;
        String name = vv.get(0).stringValue();
        uf = engine.findDeffunction(name);
        if (uf == null) 
          {
            uf = engine.findUserfunction(name);
            if (uf == null) 
              uf = (Userfunction) s_intrinsics.get(name);
          }
        if (uf == null)
          throw new ReteException("Funcall::simpleExecute",
                                  "Unimplemented function",
                                  vv.get(0).stringValue());
      }
    
    if (uf instanceof Fastfunction && v != null)
      return ((Fastfunction) uf).call(vv, context, v);
    else
      return uf.call(vv, context);
    
  }
  
  /* **********************************************************************
   * Code to pretty print funcalls
   * Original by Rajaram Ganeshan:
   * 1. public String ppFuncall()
   * 2. private String ppArgument ( ValueVector vv, int i)
   ********************************************************************** */

  String ppFuncall(Rete engine) throws ReteException
  {
    // functor
    StringBuffer sb = new StringBuffer(100);
    sb.append("(");
    sb.append(get(0).stringValue());
    sb.append(" ");

    // for each argument
    for (int i=1; i< size(); i++)
      {
        sb.append(ppArgument(i, engine));
        sb.append(" ");
      }
    sb.append(")");
    return sb.toString();
  }

  private String ppArgument (int i, Rete engine) throws ReteException
  {
    ValueVector f;
    Fact fact;
    int type = get(i).type();
    switch (type) {
    case RU.ATOM:
    case RU.FLOAT:
    case RU.INTEGER:
    case RU.VARIABLE:
      return get(i).toString();

    case RU.STRING:      
      return "\"" + get(i).stringValue() + "\"";

    case RU.LIST:
      {
        ValueVector list = get(i).listValue();
        String s = "";
          for (int j=1; j<list.size(); j++)
            s = s + ppArgument(j, engine) + " ";
        return s;
      }

    case RU.FUNCALL:
      Funcall fc = get(i).funcallValue();
      return fc.ppFuncall(engine);

    case RU.ORDERED_FACT:
    case RU.UNORDERED_FACT:
      f = get(i).factValue();
      fact = new Fact(f, engine);
      return fact.toString();

    default:
      return get(i).toString();
    }
  }

}    

// *** Fastfunction  *******************************************
abstract class Fastfunction implements Userfunction
{
  public abstract Value call(ValueVector vv, Context context, Value v)
       throws ReteException;

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    return call(vv, context, new Value(0, RU.INTEGER));
  }
}


// *** return  *******************************************
class _return implements Userfunction
{
  public int name() { return RU.putAtom("return"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    return context.setReturnValue(vv.get(1));
  }
}

// *** assert  *******************************************
class _assert extends Fastfunction
{
  public int name() { return RU.putAtom("assert"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {
    int result = -1;
    for (int i=1; i< vv.size(); i++)
      {
        ValueVector fact = vv.get(i).factValue();
        result = context.engine().assert(fact);
      }
    if (result != -1)
      return v.resetValue(result, RU.FACT_ID);
    else
      return Funcall.s_false;
  }
}
  
// *** retract *******************************************
        
class _retract implements Userfunction
{
  public int name() { return RU.putAtom("retract"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    Value v = vv.get(1);
    if (v.type() == RU.ATOM && v.stringValue().equals("*"))
      {
        context.engine().removeFacts();
      }
    else
      {
        Rete engine = context.engine();
        Userfunction uf = engine.findUserfunction("undefinstance");
        for (int i=1; i< vv.size(); i++) 
          {
            ValueVector f = engine.findFactByID(vv.get(i).factIDValue());
            if (f == null)
              continue;

            // Undefinstance, if needed.
            if (uf != null)
              {
                try
                  {
                    Fact fact = new Fact(f, engine);
                    Value ov = fact.findValue("OBJECT");
                    Funcall fc = new Funcall("undefinstance", engine);
                    fc.add(ov);
                    Funcall.simpleExecute(fc, engine.globalContext());
                  }
                catch (Exception any) { /* OK, not an object! */ }
              }
            // retract actual fact
            engine._retract(f);
          }
      }
    return Funcall.s_true;
  }
}
        
// *** printout *******************************************
      
 
class _printout implements Userfunction
{
  public int name() { return RU.putAtom("printout"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String routerName = vv.get(1).stringValue();
    OutputStream os = context.engine().getOutputRouter(routerName);
    if (os == null)
      throw new ReteException("_printout::call",
                              "printout: bad router",
                              routerName);

    StringBuffer sb = new StringBuffer(100);
    for (int i = 2; i < vv.size(); i++) 
      {
        Value v = vv.get(i);
        switch(v.type()) 
          {
          case RU.ATOM:
            if (v.stringValue().equals("crlf"))
              sb.append("\n");
            else
              sb.append(v.stringValue());
            break;
          case RU.INTEGER:
            sb.append(v.intValue());
            break;
          case RU.FLOAT:
            sb.append(v.floatValue());
            break;
          case RU.FACT_ID:
            sb.append("<Fact-");
            sb.append(v.factIDValue());
            sb.append(">");
            break;
          case RU.STRING:
            sb.append(v.stringValue());
            break;
          case RU.LIST:
            sb.append(v.toString());
            break;
          case RU.EXTERNAL_ADDRESS:
            sb.append(v.toString());
            break;
          default:
            throw new ReteException("_printout::call",
                                    "Bad data type",
                                    "type =" +v.type());
            
          }
      }
    
    try
      {
        for (int i=0; i< sb.length(); i++)
          os.write(sb.charAt(i));
        os.flush();
      }
    catch (IOException ioe)
      {
        throw new ReteException("_printout::call",
                                "I/O Exception",
                                ioe.toString());
      }
    return Funcall.s_nil;
  }
}  

// *** open *******************************************
class _open implements Userfunction
{
  public int name() { return RU.putAtom("open"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    Rete engine = context.engine();

    // Obtain parameters
    String filename = vv.get(1).stringValue();
    String router = vv.get(2).stringValue();
    String access = "r";
    if (vv.size() > 3)
      access = vv.get(3).stringValue();
    
    try
      {
        if (access.equals("r"))
          {
            engine.addInputRouter(router,
                   new BufferedInputStream(new FileInputStream(filename)));
          }
        else if (access.equals("w"))
          {
            engine.addOutputRouter(router,
                   new BufferedOutputStream(new FileOutputStream(filename)));

          } 
        else if (access.equals("a"))
          {
            RandomAccessFile raf = new RandomAccessFile(filename, "rw");
            raf.seek(raf.length());
            FileOutputStream fos = new FileOutputStream(raf.getFD());
            engine.addOutputRouter(router, new BufferedOutputStream(fos));
          }
        else
          throw new ReteException("_open::call",
                                  "Unsupported access mode",
                                  access);
      }
    catch (IOException ioe)
      {
        throw new ReteException("_open::call",
                                "I/O Exception",
                                ioe.toString());
      }
    return new Value(router, RU.ATOM);
  }
}

// *** open *******************************************
class _close implements Userfunction
{
  public int name() { return RU.putAtom("close"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    Rete engine = context.engine();
    if (vv.size() > 1)
      for (int i=1; i<vv.size(); i++)
        {
          OutputStream os;
          InputStream is;
          String router = vv.get(i).stringValue();
          try
            {
              if ((os = engine.getOutputRouter(router)) != null)
                {
                  os.close();
                  engine.removeOutputRouter(router);
                }
            }
          catch (IOException ioe) {}
          try
            {
              if ((is = engine.getInputRouter(router)) != null)
                {
                  is.close();
                  engine.removeInputRouter(router);
                }
            }
          catch (IOException ioe) {}
        }
    else
      throw new ReteException("_close::call",
                              "Must close files by name", "");
    
    return Funcall.TRUE();
  }
}

// *** read *******************************************
class _read implements Userfunction
{
  private TextInputStream m_tis = new TextInputStream();
  private JessTokenStream m_jts = new JessTokenStream(new DataInputStream(m_tis));

  public int name() { return RU.putAtom("read"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {

    // Find input source
    String routerName = "t";

    if (vv.size() > 1)
      routerName = vv.get(1).stringValue();

    DataInputStream is =
      (DataInputStream) context.engine().getInputRouter(routerName);

    if (is == null)
      throw new ReteException("_read::call",
                              "bad router",
                              routerName);
    
    // Get a non-blank line of input from source
    StringBuffer sb = new StringBuffer(80);
    int c = 0;
    try
      {
        // discard leading carriage returns
        while ((c = is.read()) != -1 && Character.isSpace((char)c))
          ;
          
        if (c == -1)
          return Funcall.s_eof;

        sb.append((char) c);

        // actual token
        sb.append(is.readLine());

      }
    catch (IOException ioe)
      {
        throw new ReteException("_read::call",
                                "I/O Exception",
                                ioe.toString());
      }

    sb.append('\n');

    // Feed the line to our captive JessTokenStream, get a Token
    m_tis.appendText(sb.toString());
    JessToken jt = m_jts.getOneToken();
    m_tis.clear();
    return jt.tokenToValue();
  }

}       
// *** readline  *******************************************
 
class _readline implements Userfunction
{
  public int name() { return RU.putAtom("readline"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String routerName = "t";

    if (vv.size() > 1)
      routerName = vv.get(1).stringValue();

    InputStream is = context.engine().getInputRouter(routerName);
    if (is == null)
      throw new ReteException("_readline::call",
                              "bad router",
                              routerName);
    try
      {
        DataInputStream dis;
        if (is instanceof DataInputStream)
          dis = (DataInputStream) is;
        else
          throw new ReteException("readline",
                                  "Input router malformed: ",
                                  routerName);

        String s = dis.readLine();
        if (s == null)
          return Funcall.s_eof;
        else
          return new Value(s, RU.STRING);
      }
    catch (IOException ioe)
      {
        throw new ReteException("readline", ioe.toString(), "");
      }
  }
}

// *** gensym*  *******************************************

class _gensym_star implements Userfunction
{
  public int name() { return RU.putAtom("gensym*"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    return new Value(RU.gensym("gen"), RU.STRING);
  }
}
// *** while *******************************************

class _while implements Userfunction
{
  public int name() { return RU.putAtom("while"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    // This accepts a superset of the correct syntax...
    Value result = vv.get(1);
    if (result.type() == RU.FUNCALL)
      result = Funcall.execute(context.expandAction(result.funcallValue()),
                               context);

    // Skip optional do
    int sawDo = 0;
    if (vv.get(2).equals(Funcall.s_do))
      ++sawDo;

  outer_loop:
    while (! result.equals(Funcall.s_false)) 
      {
        for (int i=2 + sawDo; i< vv.size(); i++) 
          {
            Value current = vv.get(i);
            if (current.type() == RU.FUNCALL) 
              {
                Funcall copy = context.expandAction(current.funcallValue());
                result = Funcall.execute(copy, context);
                if (context.returning()) 
                  {
                    result = context.getReturnValue();
                    break outer_loop;
                  }
              }
            else
              result = current;
          }
        result = vv.get(1);
        if (result.type() == RU.FUNCALL)
          result = Funcall.execute(context.expandAction(result.funcallValue()),
                                   context);
      }
    return result; 
  }
}
// *** if *******************************************

class _if implements Userfunction
{
  public int name() { return RU.putAtom("if"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    // This accepts a superset of the correct syntax...

    // check condition
    Value result = vv.get(1);
    if (result.type() == RU.FUNCALL)
      result = Funcall.execute(result.funcallValue(), context);

    if (!(result.equals(Funcall.s_false))) 
      {
        // do 'then' part
        result = Funcall.s_false;
        for (int i=3; i< vv.size(); i++) 
          {
            Value val = vv.get(i);
            if (val.type() == RU.FUNCALL) 
              {
                Funcall copy = context.expandAction(vv.get(i).funcallValue());
                result = Funcall.execute(copy, context);
                if (context.returning()) 
                  {
                    result = context.getReturnValue();
                    break;
                  }
              }
            else
              if (val.equals(Funcall.s_else))
                break;
            else
              {
                result = val;
                break;
              }
          }
        return result; 
      }
    else
      {
        // first find the 'else'
        result = Funcall.s_false;
        boolean seen_else = false;
        for (int i=3; i< vv.size(); i++)
          {
            Value val = vv.get(i);
            if (seen_else) 
              {
                if (val.type() == RU.FUNCALL)
                  {
                    Funcall copy
                      = context.expandAction(vv.get(i).funcallValue());
                    result = Funcall.execute(copy, context);
                    if (context.returning()) 
                      {
                        result = context.getReturnValue();
                        break;
                      }
                  }
                else
                  result = val;
              } 
            else if (val.equals(Funcall.s_else))
              seen_else = true;
          }
        return result;
      }
  }
}
        
// *** bind *******************************************
        
class _bind implements Userfunction
{
  public int name() { return RU.putAtom("bind"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    context.setVariable(vv.get(1).variableValue(),
                        vv.get(2));
    return vv.get(2);
  }
}
// *** foreach *******************************************
        
class _foreach implements Userfunction
{
  public int name() { return RU.putAtom("foreach"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    int variable = vv.get(1).variableValue();
    ValueVector items = vv.get(2).listValue();
    Value v = Funcall.NIL();

    for (int i=0; i<items.size(); i++)
      {
        context.setVariable(variable, items.get(i));
        for (int j=3; j<vv.size(); j++)
          {
            v = vv.get(j);
            switch (v.type())
              {
              case RU.FUNCALL:
                v = Funcall.execute(context.expandAction(v.funcallValue()),
                                    context);
                if (context.returning()) 
                  {
                    v = context.getReturnValue();
                    context.clearReturnValue();
                    return v;
                  }                
                break;
                
              case RU.VARIABLE:
              case RU.MULTIVARIABLE:
                v = context.findBinding(v.variableValue()).m_val;
                break;
              }
          }
      }
    return v;
  }
}

// *** modify  *******************************************
        
class _modify implements Userfunction
{
  public int name() { return RU.putAtom("modify"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector fact;
    if ((fact
         = context.m_engine.findFactByID(vv.get(1).factIDValue())) == null)
      throw new ReteException("_modify::call",
                              "modify: no such fact",
                              "");
        
    // we have the ValueVector fact, make a Fact out of it
        
    Fact f = new Fact(fact, context.engine());
    Rete engine = context.engine();
        
    // First find out of this is a LHS object, not an ordinary fact
    // If so, call the appropriate mutators...

    if (engine.findUserfunction("definstance") != null)
      {
        try
          {
            Value ov = f.findValue("OBJECT");
            Funcall fc = new Funcall("set", engine);
            fc.add(ov);
            fc.add(new Value("set", RU.STRING));
            fc.setLength(4);
            
            for (int i= 2; i < vv.size(); i++) 
              {
                
                // fetch the slot, value subexp, stored as a List
                ValueVector svp = vv.get(i).listValue();
                
                fc.set(svp.get(0), 2);

                if (svp.size() > 2)
                  {            
                    // wrap the new value in a list before adding
                    ValueVector mf = new ValueVector();
                    for (int j=1; j < svp.size(); j++)
                      mf.add(svp.get(j));
                    
                    fc.set(new Value(mf, RU.LIST), 3);
                  }
                else 
                  fc.set(svp.get(1), 3);
                
                Funcall.simpleExecute(fc, engine.globalContext());
              }
            return Funcall.TRUE();
          }
        catch (ReteException re) { /* OK, not an object */ }
      }
  
    // This is just an ordinary fact.
    // now change the values. For each argument...
    for (int i= 2; i < vv.size(); i++) 
      {
            
        // fetch the slot, value subexp, stored as a List
        ValueVector svp = vv.get(i).listValue();
            
        int slot_desc_idx =
          (f.findSlot(svp.get(0).atomValue()) - RU.FIRST_SLOT)
          * RU.DT_SLOT_SIZE  + RU.FIRST_SLOT;
            
        if ( f.deft().get(slot_desc_idx).type() == RU.MULTISLOT &&
            (svp.size() < 2 || svp.get(1).type() != RU.LIST))
          {            
            // wrap the new value in a list before adding
            ValueVector mf = new ValueVector();
            for (int j=1; j < svp.size(); j++)
              mf.add(svp.get(j));
            f.addValue(svp.get(0).stringValue(), new Value(mf, RU.LIST));
          }
        else 
          {
            // add the new value directly
            f.addValue(svp.get(0).stringValue(), svp.get(1));
          }
      }
        
    // get rid of the old one.
    context.engine().retract(fact.get(RU.ID).factIDValue());
        
    // get the new fact data
    fact = f.factData();
        
    // and assert the new fact
    return new Value(context.engine().assert(fact), RU.FACT_ID);
  }
}

// *** and *******************************************

class _and implements Userfunction
{
  public int name() { return RU.putAtom("and"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size(); i ++) 
      {
        Value v = vv.get(i);
        switch (v.type())
          {
          case RU.FUNCALL:
            v = Funcall.execute(context.expandAction(v.funcallValue()),
                                context);
            /* FALL THROUGH */

          default:
            if (v.equals(Funcall.s_false))
              return Funcall.s_false;
          }
      }
    return Funcall.s_true;
        
  }
}
// *** or *******************************************
class _or implements Userfunction
{
  public int name() { return RU.putAtom("or"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size(); i ++) 
      {
        Value v = vv.get(i);
        switch (v.type())
          {
          case RU.FUNCALL:
            v = Funcall.execute(context.expandAction(v.funcallValue()),
                                context);
            /* FALL THROUGH */

          default:
            if (!v.equals(Funcall.s_false))
              return Funcall.s_true;
          }
      }
    return Funcall.s_false;
  }
}
        
// *** not *******************************************

class _not implements Userfunction
{
  public int name() { return RU.putAtom("not"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    if (vv.get(1).equals(Funcall.s_false))
      return Funcall.s_true;
    else
      return Funcall.s_false;
  }
}
// *** eq *******************************************
 
class _eq implements Userfunction
{
  public int name() { return RU.putAtom("eq"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (!vv.get(i).equals(vv.get(1)))
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}
// *** eq* *******************************************
 
class _eqstar implements Userfunction
{
  public int name() { return RU.putAtom("eq*"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (!vv.get(i).equalsStar(vv.get(1)))
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}

// *** = *******************************************


class _equals implements Userfunction
{
  public int name() { return RU.putAtom("="); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (!(vv.get(i).numericValue() == vv.get(1).numericValue()))
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}
        
// *** <> *******************************************

class _not_equals implements Userfunction
{
  public int name() { return RU.putAtom("<>"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (vv.get(i).numericValue() == vv.get(1).numericValue())
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}
        
// *** > *******************************************

class _gt implements Userfunction
{
  public int name() { return RU.putAtom(">"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size()-1; i ++) 
      {
        double value1 = vv.get(i).numericValue();
        double value2 = vv.get(i+1).numericValue();
            
        if (!(value1 > value2))
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}
        
// *** < *******************************************

class _lt implements Userfunction
{
  public int name() { return RU.putAtom("<"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size()-1; i ++) 
      {
        double value1 = vv.get(i).numericValue();
        double value2 = vv.get(i+1).numericValue();
            
        if (!(value1 < value2))
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}
        
// *** >= *******************************************

class _gt_or_eq implements Userfunction
{
  public int name() { return RU.putAtom(">="); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size()-1; i ++) 
      {
        double value1 = vv.get(i).numericValue();
        double value2 = vv.get(i+1).numericValue();
            
        if (!(value1 >= value2))
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}
// *** <= *******************************************
class _lt_or_eq implements Userfunction
{
  public int name() { return RU.putAtom("<="); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i<vv.size()-1; i ++) 
      {
        double value1 = vv.get(i).numericValue();
        double value2 = vv.get(i+1).numericValue();
          
        if (!(value1 <= value2))
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}
// *** neq *******************************************

class _neq implements Userfunction
{
  public int name() { return RU.putAtom("neq"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=2; i<vv.size(); i ++) 
      {
        if (vv.get(i).equals(vv.get(1)))
          return Funcall.s_false;
      }
    return Funcall.s_true;
  }
}
// *** mod *******************************************

class _mod extends Fastfunction
{
  public int name() { return RU.putAtom("mod"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {
    int d1 = (int) vv.get(1).numericValue();
    int d2 = (int) vv.get(2).numericValue();
        
    return v.resetValue(d1 % d2, RU.INTEGER);
       
  }
}
// *** + *******************************************
class _plus extends Fastfunction
{
  public int name() { return RU.putAtom("+"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {
    double sum = 0;
    int type = RU.INTEGER;
    for (int i=1; i<vv.size(); i++) 
      {
        Value arg = vv.get(i);
        sum += arg.numericValue();
        if (type == RU.INTEGER && arg.type() == RU.FLOAT)
          type = RU.FLOAT;
          
      }

    return v.resetValue(sum, type);      
  } 
}
// *** * *******************************************

class _times extends Fastfunction
{
  public int name() { return RU.putAtom("*"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {
    double product = 1;
    int type = RU.INTEGER;
    for (int i=1; i<vv.size(); i++) 
      {
        Value arg = vv.get(i);
        product *= arg.numericValue();
        if (type == RU.INTEGER && arg.type() == RU.FLOAT)
          type = RU.FLOAT;
      }
                                  
    return v.resetValue(product, type);

  } 
}
// *** - *******************************************
class _minus extends Fastfunction
{
  public int name() { return RU.putAtom("-"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {
    Value arg = vv.get(1);
    int type = arg.type();
    double diff = arg.numericValue();

    for (int i=2; i<vv.size(); i++) 
      {
        arg = vv.get(i);
        diff -= arg.numericValue();
        if (type == RU.INTEGER && arg.type() == RU.FLOAT)
          type = RU.FLOAT;
      }

    return v.resetValue(diff, type);

  } 
}
// *** / *******************************************
class _divide extends Fastfunction
{
  public int name() { return RU.putAtom("/"); }
  public Value call(ValueVector vv, Context context, Value v) 
       throws ReteException
  {
    double quotient = vv.get(1).numericValue();
      
    for (int i=2; i<vv.size(); i++) 
      {
        quotient /= vv.get(i).floatValue();
      }
    return v.resetValue(quotient, RU.FLOAT);


  } 
}

// *** sym-cat *******************************************
class _sym_cat extends Fastfunction
{
  public int name() { return RU.putAtom("sym-cat"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {

    StringBuffer buf = new StringBuffer( "" );
    for ( int i = 1; i < vv.size( ); i++ )
      {
        Value val = vv.get(i);
        if (val.type() == RU.STRING)
          buf.append(val.stringValue());
        else
          buf.append (val.toString());
      }
    
    return v.resetValue( buf.toString( ), RU.ATOM );
  }
}
// *** reset *******************************************

class _reset implements Userfunction
{
  public int name() { return RU.putAtom("reset"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().reset();
    return Funcall.s_true  ;
      
  }
}
// *** run *******************************************

class _run implements Userfunction
{
  public int name() { return RU.putAtom("run"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    if (vv.size() == 1)
      context.engine().run();
    else
      context.engine().run(vv.get(1).intValue());
    return Funcall.s_true;
  }
}
// *** facts *******************************************

class _facts implements Userfunction
{
  public int name() { return RU.putAtom("facts"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().showFacts();
    return Funcall.s_true;
      
  } 
  // *** rules *******************************************

}
class _rules implements Userfunction
{
  public int name() { return RU.putAtom("rules"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().showRules();
    return Funcall.s_true;

      
  } 
}
// *** halt *******************************************
class _halt implements Userfunction
{
  public int name() { return RU.putAtom("halt"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().halt();
    return Funcall.s_true;
  }
}
// *** exit *******************************************
      

class _exit implements Userfunction
{
  public int name() { return RU.putAtom("exit"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    System.exit(0);
    return Funcall.s_true;
  }
}
// *** halt *******************************************
      
class _clear implements Userfunction
{
  public int name() { return RU.putAtom("clear"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    context.engine().clear();
    return Funcall.s_true;
  }
}
// *** watch *******************************************
class _watch implements Userfunction
{
  public int name() { return RU.putAtom("watch"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String what = vv.get(1).stringValue();
    if (what.equals("rules"))
      context.engine().watchRules(true);
    else if (what.equals("facts"))
      context.engine().watchFacts(true);
    else if (what.equals("compilations"))
      context.engine().watchCompilations(true);
    else if (what.equals("activations"))
      context.engine().watchActivations(true);
    else if (what.equals("all")) 
      {
        context.engine().watchFacts(true);
        context.engine().watchRules(true);
        context.engine().watchCompilations(true);
        context.engine().watchActivations(true);
      }
    else
      throw new ReteException("Funcall::Execute", "watch: can't watch" ,
                              what);

    return Funcall.s_true;
  }
}
// *** unwatch *******************************************

class _unwatch implements Userfunction
{
  public int name() { return RU.putAtom("unwatch"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String what = vv.get(1).stringValue();
    if (what.equals("rules"))
      context.engine().watchRules(false);
    else if (what.equals("facts"))
      context.engine().watchFacts(false);
    else if (what.equals("compilations"))
      context.engine().watchCompilations(false);
    else if (what.equals("activations"))
      context.engine().watchActivations(false);
    else if (what.equals("all")) 
      {
        context.engine().watchFacts(false);
        context.engine().watchRules(false);
        context.engine().watchCompilations(false);
        context.engine().watchActivations(false);
      }
    else
      throw new ReteException("Funcall::Execute", "unwatch: can't unwatch" ,
                              what);
    return Funcall.s_true;
  }
}
// *** jess versions  *******************************************

 
class _jess_version_string extends Fastfunction
{
  public int name() { return RU.putAtom("jess-version-string"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {
    return v.resetValue("Jess Version 4.0 3/23/98", RU.STRING);
  } 
}
class _jess_version_number extends Fastfunction
{
  public int name() { return RU.putAtom("jess-version-number"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {
    return v.resetValue(4.0, RU.FLOAT);
  }
}

// *** load-facts ***********************************************

class _load_facts implements Userfunction
{
  public int name() { return RU.putAtom("load-facts"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String s = "ERROR";
    InputStream f;
    if (context.engine().display().applet() == null) 
      {
        try 
          {
            f = new FileInputStream(vv.get(1).stringValue());
          }
        catch (IOException t) 
          {
            throw new ReteException("_load_facts::call",
                                    "I/O Exception",
                                    t.toString());
          }
        
      }
    else 
      {
        try 
          {
            URL url = new URL(context.engine().display().applet().getDocumentBase(),
                              vv.get(1).stringValue());          
            f = url.openStream();
          } 
        catch (Exception t) 
          {
            throw new ReteException("_load_facts::call",
                                    "Network error",
                                    t.toString());
          }
      }
      
    // OK, we have a stream. Now the tricky part!

    Jesp jesp = new Jesp(f, context.engine());

    return jesp.loadFacts();
  }
}

// *** save-facts ***********************************************


class _save_facts implements Userfunction
{
  public int name() { return RU.putAtom("save-facts"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String s = "";
    PrintStream f;
    if (context.engine().display().applet() == null) 
      {
        try 
          {
            f = new PrintStream(new FileOutputStream(vv.get(1).stringValue()));
          } 
        catch (IOException t) 
          {
            throw new ReteException("_save_facts::call", 
                                    "I/O Exception",
                                    t.toString());
          }
        
      }
    else 
      {
        try 
          {
            URL url = new URL(context.engine().display().applet().getDocumentBase(),
                              vv.get(1).stringValue());          
            URLConnection urlc  = url.openConnection();
            urlc.setDoOutput(true);
            f = new PrintStream(urlc.getOutputStream());
          }
        catch (Exception t) 
          {
            throw new ReteException("_load_facts::call",
                                    "Network error",
                                    t.toString());
          }
      }
      
    // OK, we have a stream. Now the tricky part!
    if (vv.size() > 2) 
      {
        for (int i=2; i< vv.size(); i++) 
          {
            s += context.engine().ppFacts(vv.get(i).atomValue());
          
          }
      }
    else 
      s = context.engine().ppFacts();

    f.println(s);
    f.close();
    return Funcall.s_true;
      
  } 
}

class _assert_string extends Fastfunction
{
  public int name() { return RU.putAtom("assert-string"); }
  public Value call(ValueVector vv, Context context, Value v)
       throws ReteException
  {
    String fact = vv.get(1).stringValue();
    return v.resetValue(context.engine().assertString(fact), RU.FACT_ID);      

  }
}

//***********************************************************//
// Karl Mueller NASA/GSFC Code 522.2 
// (Karl.R.Mueller@gsfc.nasa.gov)
// 26.January.1998
//
// *** retract-string ***************************************
// Added function to retract fact as a string
//***********************************************************//

class _retract_string implements Userfunction
{
  public int name() { return RU.putAtom("retract-string"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    for (int i=1; i< vv.size(); i++) 
      {
        context.engine().retractString(vv.get(i).stringValue());
      }
    return Funcall.s_true;
  }
}

class _undefrule implements Userfunction
{
  public int name() { return RU.putAtom("undefrule"); }
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    int rulename = vv.get(1).atomValue();
    return context.engine().unDefrule(rulename);      

  } 

}    

// ************************************************************
// Fetch a variable by name. ReteCompiler uses this to fetch
// defglobals in rule LHSs.
// ************************************************************

class _extract_global implements Userfunction
{
  public int name()
  {
    return RU.putAtom("get-var");
  }

  public Value call(ValueVector vv, Context context)
       throws ReteException
  {
    return context.findBinding(vv.get(1).atomValue()).m_val;
  }
  
}


