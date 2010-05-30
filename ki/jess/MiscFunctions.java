/** **********************************************************************
 *  Some small sample user-defined miscellaneous functions
 * 
 * To use one of these functions from Jess, simply register the
 * package class in your Java mainline:
 *
 * engine.AddUserpackage(new MiscFunctions());
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;
import java.io.*;
import java.net.*;
import java.util.*;


public class MiscFunctions implements Userpackage 
{

  public void add(Rete engine) 
  {
    engine.addUserfunction(new socket());
    engine.addUserfunction(new _format());
    engine.addUserfunction(new batch());
    engine.addUserfunction(new _system());
    engine.addUserfunction(new ppdefrule());
    engine.addUserfunction(new loadpkg());
    engine.addUserfunction(new loadfn());
    engine.addUserfunction(new time());
    engine.addUserfunction(new build());
    engine.addUserfunction(new eval());
    engine.addUserfunction(new listfunctions());
    // setgen added by Win Carus (9.19.97)
    engine.addUserfunction(new setgen());
  }
}

class batch implements Userfunction 

{

  private int m_name = RU.putAtom("batch");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {

    String filename = vv.get(1).stringValue();
    Value v = Funcall.FALSE();
    InputStream fis = null;
    try
      {
        if (context.engine().display().applet() == null)
          fis = new FileInputStream(filename);
        else
          {
            URL url
              = new URL(context.engine().display().applet().getDocumentBase(),
                        vv.get(1).stringValue());          
            fis = url.openStream(); 
          }         
        Jesp j = new Jesp(fis, context.engine());
        do
          {
            v = j.parse(false);
          }
        while (fis.available() > 0);
      }
    catch (IOException ex)
      {
        throw new ReteException("batch", "I/O Exception on file", "");
      }    
    finally
      {
        if (fis != null) try { fis.close(); } catch (IOException ioe) {}
      }
    return v;
  }
}

class build implements Userfunction 
{

  private int m_name = RU.putAtom("build");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String argument = vv.get(1).stringValue();
    return context.engine().executeCommand(argument);
  }
}

// These will diverge in the future!

class eval implements Userfunction 
{

  private int m_name = RU.putAtom("eval");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String argument = vv.get(1).stringValue();
    return context.engine().executeCommand(argument);
  }
}


class listfunctions implements Userfunction 
{

  private int m_name = RU.putAtom("list-function$");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector rv = new ValueVector(100);
    Enumeration e = Funcall.listIntrinsics();
    while (e.hasMoreElements())
      {
        rv.add(new Value((String) e.nextElement(), RU.ATOM));
      }
    e = context.engine().listDeffunctions();
    while (e.hasMoreElements())
      {
        rv.add(new Value((String) e.nextElement(), RU.ATOM));
      }
    e = context.engine().listUserfunctions();
    while (e.hasMoreElements() )
      {
        String s = (String) e.nextElement();
        rv.add(new Value(s, RU.ATOM));
      }

    // Bubble-sort the names.
    int swaps;
    do 
      {
        swaps = 0;
        for (int i=0; i< rv.size() -1; i++)
          {
            Value v1 = rv.get(i);
            Value v2 = rv.get(i+1);
            if (v1.stringValue().compareTo(v2.stringValue()) > 0)
              {
                ++swaps;
                rv.set(v2, i);
                rv.set(v1, i +1);
              }
          }

      }
    while (swaps > 0);

    return new Value(rv, RU.LIST);
  }
}

class ppdefrule implements Userfunction 
{

  private int m_name = RU.putAtom("ppdefrule");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String argument = vv.get(1).stringValue();
    Defrule dr = context.engine().findDefrule(argument);
    return new Value(dr.ppRule(), RU.STRING);
  }
}

class _system implements Userfunction 
{

  private int m_name = RU.putAtom("system");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        boolean async = false;
        int size = vv.size();
        if (vv.get(size - 1).stringValue().equals("&"))
            {
              async = true;
              --size;
            }

        String[] words = new String[size - 1];
        for (int i=1; i<size; i++)
          words[i-1] = vv.get(i).stringValue();
        Process p = Runtime.getRuntime().exec(words);
        try
          {
            if (!async)
              p.waitFor();
          }
        catch (InterruptedException ie)
          {
            return Funcall.FALSE();
          }
        return Funcall.TRUE();
      }
    catch (IOException ioe)
      {
        throw new ReteException("system", "I/O Exception", "");
      }
    catch (SecurityException se)
      {
        throw new ReteException("system", "Security Exception", "");
      }
  }
}

class loadpkg implements Userfunction 
{

  private int m_name = RU.putAtom("load-package");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String clazz = vv.get(1).stringValue();
    try
      {
        Userpackage up = (Userpackage) Class.forName(clazz).newInstance();
        context.engine().addUserpackage(up);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("load-package", "Class not found", clazz);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("load-package", "Class is not accessible",
                                clazz);
      }
    catch (InstantiationException ie)
      {
        throw new ReteException("load-package", "Class cannot be instantiated",
                                clazz);
      }
    catch (ClassCastException cnfe)
      {
        throw new ReteException("load-package",
                                "Class must inherit from UserPackage", clazz);
      }
    return Funcall.TRUE();
  }
}

class loadfn implements Userfunction 
{

  private int m_name = RU.putAtom("load-function");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String clazz = vv.get(1).stringValue();
    try
      {
        Userfunction uf = (Userfunction) Class.forName(clazz).newInstance();
        context.engine().addUserfunction(uf);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("load-function", "Class not found", clazz);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("load-function", "Class is not accessible",
                                clazz);
      }
    catch (InstantiationException ie)
      {
        throw new ReteException("load-function",
                                "Class cannot be instantiated",
                                clazz);
      }
    catch (ClassCastException cnfe)
      {
        throw new ReteException("load-function",
                                "Class must inherit from UserFunction", clazz);
      }
      
    return Funcall.TRUE();
  }
}

class time implements Userfunction
{
  private int m_name = RU.putAtom( "time" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( System.currentTimeMillis()/1000, RU.FLOAT );
  }
}

class socket implements Userfunction
{
  private int m_name = RU.putAtom( "socket" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    String host = vv.get(1).stringValue();
    int port = vv.get(2).intValue();
    String router = vv.get(3).stringValue();

    try
      {
        Socket sock = new Socket(host, port);
        Rete engine = context.engine();
        engine.addInputRouter(router, sock.getInputStream());
        engine.addOutputRouter(router, sock.getOutputStream());
        return vv.get(3);
      }
    catch (IOException ioe)
      {
        throw new ReteException("socket::call", "I/O Exception", ioe.toString());
      }
  }
}

class setgen implements Userfunction
{
  private int m_name = RU.putAtom( "setgen" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    RU.s_gensymIdx = vv.get( 1 ).intValue( );
    return Funcall.TRUE( );
  }
}

/*
  format is implemented in terms of the Format class from the Core Java CD
  which has the annoying limitation that it can only format one datum at
  a time! 
  */

class _format implements Userfunction
{
  private int m_name = RU.putAtom( "format" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    // individual formats in here
    StringBuffer fmtbuf = new StringBuffer(20);

    // The eventual return value
    StringBuffer outbuf = new StringBuffer(100);

    // The router where things go
    Value router = vv.get(1);

    // The format command string    
    String fmt = vv.get(2).stringValue();
    // an index into the above
    int ptr = 0;

    // index over arguments
    int validx = 3;

    char c = 0;
    while (ptr < fmt.length())
      {
        // Any leading non-format stuff
        while (ptr < fmt.length() && (c = fmt.charAt(ptr++)) != '%')
          outbuf.append(c);
        
        if (ptr >= fmt.length())
          break;

        // copy format to fmtbuf
        fmtbuf.setLength(0);
        fmtbuf.append(c);
        while (ptr < fmt.length() && (c = fmt.charAt(ptr++)) != '%'
                && !Character.isLetter(c))
          fmtbuf.append(c);
        if (c == 'n')
          {
            outbuf.append('\n');
            break;
          }
        else if (c == '%')
          {
            outbuf.append('%');
            break;
          }
        else
          fmtbuf.append(c);
        
        Format f = new Format(fmtbuf.toString());
        Value v;
        switch (f.fmt)
          {
          case 'd': case 'i': case 'o': case 'x': case 'X':
            v = vv.get(validx++);
            outbuf.append(f.form(v.intValue())); break;
          case 'f': case 'e': case 'E': case 'g': case 'G':
            v = vv.get(validx++);
            outbuf.append(f.form(v.floatValue())); break;
          case 'c':
            v = vv.get(validx++);
            switch (v.type())
              {
              case RU.ATOM: case RU.STRING:
                outbuf.append(f.form(v.stringValue().charAt(0))); break;
              default:
                outbuf.append(f.form((char) v.intValue())); break;
              }
            break;
          case 's':
            v = vv.get(validx++);                
            outbuf.append(f.form(v.stringValue())); break;
            
          default:
            throw new ReteException("_format::call",
                                    "Unknown format",
                                    fmtbuf.toString());
          }
      }

    String s = outbuf.toString();
    if (!router.equals(Funcall.NIL()))
      {
        String routerName = router.stringValue();
        OutputStream os = context.engine().getOutputRouter(routerName);
        if (os == null)
          throw new ReteException("_format::call", "Bad router", routerName);
        try
          {
            for (int i=0; i<s.length(); i++)
              os.write(s.charAt(i));
            os.flush();
          }
        catch (IOException ioe)
          {
            throw new ReteException("_format::call", "I/O Exception", ioe.toString());
          }
      }

    return new Value(s, RU.STRING);
  }
}
