/** **********************************************************************
 *  Java Reflection for Jess
 *
 * This stuff is suprisingly powerful! Right now we don't handle
 * multi-dimensional arrays, but I think we don't miss anything else!
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess.reflect;

import java.lang.reflect.*;
import java.beans.*;
import java.util.*;
import jess.*;

public class ReflectFunctions implements Userpackage 
{
  public void add(Rete engine) 
  {
    engine.addUserfunction(new _new());
    engine.addUserfunction(new _engine(engine));
    engine.addUserfunction(new _call());
    engine.addUserfunction(new _field("set-member"));
    engine.addUserfunction(new _field("get-member"));
    engine.addUserfunction(new _set());
    engine.addUserfunction(new _get());

    defclass dc = new defclass();
    engine.addUserfunction(dc);
    engine.addClearable(dc);

    definstance di = new definstance(engine, dc);
    engine.addUserfunction(di);
    engine.addClearable(di);
    engine.addResetable(di);

    engine.addUserfunction(new undefinstance(di));

  }

  // ******************************
  // Return a Java argument derived from the Value which matches the
  // Class object as closely as possible. Throws an exception if no match.
  // ******************************

  static Object valueToObject(Class clazz, Value value)
       throws IllegalArgumentException, ReteException
  {
    switch (value.type())
      {

      case RU.EXTERNAL_ADDRESS:
        {
          if (clazz.isInstance(value.externalAddressValue()))
            return value.externalAddressValue();
          else
            throw new IllegalArgumentException();
        }

      case RU.ATOM:
      case RU.STRING:
        {
          String s = value.stringValue();
          if (clazz == String.class)
            return s;

          else if (clazz == Character.TYPE)
            {
              if (s.length() == 1)
                return new Character (s.charAt(0));
              else
                throw new IllegalArgumentException();
            }

          else if (clazz == Boolean.TYPE)
            {
              if (s.equals("TRUE"))
                return Boolean.TRUE;
              if (s.equals("FALSE"))
                return Boolean.FALSE;
              else
                throw new IllegalArgumentException();
            }

          else if (!clazz.isPrimitive() && s.equals("NIL"))
            return null;

          else
            throw new IllegalArgumentException();
        }
      
      case RU.INTEGER:
        {
          int i = value.intValue();

          if (clazz == Long.TYPE || clazz == Long.class)
            return new Long(i);

          else if (clazz == Integer.TYPE || clazz == Integer.class)
            return new Integer(i);

          else if (clazz == Short.TYPE || clazz == Short.class)
            return new Short((short) i);

          else if (clazz == Character.TYPE || clazz == Character.class)
            return new Character( (char) i);

          else if (clazz == Byte.TYPE || clazz == Byte.class)
            return new Byte( (byte) i);
          
          else
            throw new IllegalArgumentException();

        }
        
      case RU.FLOAT:
        {
          double d = value.floatValue();

          if (clazz == Double.TYPE || clazz == Double.class)
            return new Double(d);

          else if (clazz == Float.TYPE || clazz == Float.class)
            return new Float((float) d);

          else
            throw new IllegalArgumentException();
          
        }

      // Turn lists into arrays.
      case RU.LIST:
        {
          if (clazz.isArray())
            {
              Class elemType = clazz.getComponentType();
              ValueVector vv = value.listValue();
              Object array = Array.newInstance(elemType, vv.size());
              for (int i=0; i<vv.size(); i++)
                Array.set(array, i, valueToObject(elemType, vv.get(i)));
              return array;
            }
          else
            throw new IllegalArgumentException();
        }
      default:
        throw new IllegalArgumentException();
      }
    
  }

  // ******************************
  // Create a Jess Value object out of a Java Object. Primitive types get
  // special treatment.
  // ******************************

  static Value objectToValue(Class c, Object obj) throws ReteException
  {
    if (obj == null)
      return Funcall.NIL();
    
    if (c == Void.class)
      return Funcall.NIL();

    if (c == String.class)
      return new Value( obj.toString(), RU.STRING);

    if (c.isArray())
      {
        int length = Array.getLength(obj);
        ValueVector vv = new ValueVector(length);

        for (int i=0; i<length; i++)
          vv.add(objectToValue(c.getComponentType(), Array.get(obj, i)));

        return new Value(vv, RU.LIST);
      }

    if (c == Boolean.TYPE)
      return ((Boolean) obj).booleanValue() ? Funcall.TRUE() : Funcall.FALSE();

    if (c == Byte.TYPE || c == Short.TYPE ||
        c == Integer.TYPE || c == Long.TYPE)
      return new Value (( (Number) obj).intValue(), RU.INTEGER);

    if (c == Double.TYPE || c == Float.TYPE)
      return new Value (( (Number) obj).doubleValue(), RU.FLOAT);

    if (c == Character.TYPE)
      return new Value (obj.toString(), RU.ATOM);

    return new Value(obj, RU.EXTERNAL_ADDRESS);
  }

}

class _engine implements Userfunction 
{
  private int m_name = RU.putAtom("engine");
  public int name() { return m_name; }
  
  private Value m_engine;
  _engine(Rete engine)
  {
    try
      {
        m_engine = new Value(engine, RU.EXTERNAL_ADDRESS);
      }
    catch (ReteException re) { /* Can't happen */ }

  }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    return m_engine;
  }
}

// **********************************************************************
// Call a Java method from Jess. First argument is EITHER an external-address
// object, or the name of a class. The latter works only for Static methods, of
// course. Later arguments are the contructor arguments. We pick methods based
// on a first-fit algorithm, not necessarily a best-fit. If you want to be super
// selective, you can disambiguate by wrapping basic types in object wrappers.
// If it absolutely won't work, well, you can always write a Java Userfunction
// as a wrapper!
// **********************************************************************

class _call implements Userfunction 
{

  int m_name = RU.putAtom("call");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String method = vv.get(2).stringValue();

    try
      {
        Class c = null;
        Object target = null;

        Value v = vv.get(1);
    if (v.type() == RU.STRING || v.type() == RU.ATOM)
          {
            try
              {
                c = Class.forName(v.stringValue());
              }
            catch (ClassNotFoundException cnfe)
              {
                // Maybe we're supposed to call the method
                // on the string object itself...
              }
          }
        if (c == null)
          {
            target = v.externalAddressValue();
            c = target.getClass();
          }        

        /*
         * Build argument list
         */

        int nargs = vv.size() - 3;
        Object args[] = new Object[nargs];

        Method [] methods = c.getMethods();
        Object rv = null;
        int i;
        for (i=0; i< methods.length; i++)
          {
            try
              {
                Method m = methods[i];
                Class[] argTypes = m.getParameterTypes();
                if (!m.getName().equals(method) || nargs != argTypes.length)
                  continue;

                // Otherwise give it a try!
                
                for (int j=0; j<nargs; j++)
                  args[j]
                    = ReflectFunctions.valueToObject(argTypes[j], vv.get(j+3));

                rv = m.invoke(target, args);
                return ReflectFunctions.objectToValue(m.getReturnType(), rv);
                
              }
            catch (IllegalArgumentException iae)
              {
                // Try the next one!
              }
          }

        throw new NoSuchMethodException(method);

      }
    catch (NoSuchMethodException nsm)
      {
        throw new ReteException("call", "No method '" + method + "' found",
                                "or invalid argument types");
      }
    catch (InvocationTargetException ite)
      {
        throw new ReteException("call", "Called method threw an exception",
                                ite.getTargetException().toString()); 
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("call", "Method is not accessible", method);
      }
    catch (IllegalArgumentException iae)
      {
        throw new ReteException("call", "Invalid argument to", method);
      }
  }
}

class _set extends _call
{
  _set() { m_name = RU.putAtom("set");}

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        String propName = vv.get(2).stringValue();
        // note that these are cached, so all the introspection
        // only gets done once.
        PropertyDescriptor [] pd = 
          Introspector.getBeanInfo(vv.get(1).externalAddressValue().getClass()).
          getPropertyDescriptors();
        for (int i=0; i<pd.length; i++)
          {
            Method m = null;
            if (pd[i].getName().equals(propName) &&
                (m = pd[i].getWriteMethod()) != null)
              {
                vv.set(new Value(m.getName(), RU.STRING), 2);          
                return super.call(vv, context);
              }
          }
        throw new ReteException("set", "No such property:", propName);
      }
    catch (IntrospectionException ie)
      {
        throw new ReteException("get", "Introspection Error:", ie.toString());
      }
  }
}

class _get extends _call
{
  _get() { m_name = RU.putAtom("get");}
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        String propName = vv.get(2).stringValue();
        // note that these are cached, so all the introspection
        // only gets done once.
        PropertyDescriptor [] pd = 
          Introspector.getBeanInfo(vv.get(1).externalAddressValue().getClass()).
          getPropertyDescriptors();
        for (int i=0; i<pd.length; i++)
          {
            Method m = null;
            if (pd[i].getName().equals(propName) &&
                (m = pd[i].getReadMethod()) != null)
              {
                vv.set(new Value(m.getName(), RU.STRING), 2);          
                return super.call(vv, context);
              }
          }
        throw new ReteException("get", "No such property:", propName);
      }
    catch (IntrospectionException ie)
      {
        throw new ReteException("get", "Introspection Error:", ie.toString());
      }
  }
}

// **********************************************************************
// Create a Java object from Jess
// The first argument is the full-qualified typename; later arguments are
// the contructor arguments.  We pick methods based on a first-fit algorithm,
// not necessarily a best-fit. If you want to be super selective, you can
// disambiguate by wrapping basic types in object wrappers.
// **********************************************************************

class _new implements Userfunction 
{

  private int m_name = RU.putAtom("new");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String clazz = vv.get(1).stringValue();
    try
      {
        /*
         * Build argument list
         */

        int nargs = vv.size() - 2;
        Object args[] = new Object[nargs];

        Class c = Class.forName(clazz);
        Constructor [] cons = c.getConstructors();
        Object rv = null;
        int i;
        for (i=0; i< cons.length; i++)
          {
            try
              {
                Constructor constructor = cons[i];
                Class[] argTypes = constructor.getParameterTypes();
                if (nargs != argTypes.length)
                  continue;

                // Otherwise give it a try!
                
                for (int j=0; j<nargs; j++)
                  args[j]
                    = ReflectFunctions.valueToObject(argTypes[j], vv.get(j+2));

                rv = constructor.newInstance(args);
                return new Value(rv, RU.EXTERNAL_ADDRESS);

              }
            catch (IllegalArgumentException iae)
              {
                // Try the next one!
              }
          }

        throw new NoSuchMethodException(clazz);       

      }
    catch (InvocationTargetException ite)
      {
        throw new ReteException("new", "Constructor threw an exception",
                                ite.getTargetException().toString());
      }
    catch (NoSuchMethodException nsm)
      {
        throw new ReteException("new", "Constructor not found", clazz);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("new", "Class not found", clazz);
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("new",
                                "Class or constructor is not accessible",
                                clazz);
      }
    catch (InstantiationException ie)
      {
        throw new ReteException("new", "Class cannot be instantiated", clazz);
      }
  }
}

// **********************************************************************
// Set or get a data member of a Java object from Jess
// **********************************************************************

class _field implements Userfunction 
{

  private int m_name;
  public int name() { return m_name; }
  
  _field(String functionName)
  {
    // name should be get-member or set-member
    m_name = RU.putAtom(functionName);
  }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String field = vv.get(2).stringValue();

    boolean doSet = false;

    if (vv.get(0).stringValue().equals("set-member"))
      doSet = true;
    
    Class c = null;
    Object target = null;
    
    Value v = vv.get(1);

    if (v.type() == RU.STRING || v.type() == RU.ATOM)
      {
        try
          {
            c = Class.forName(v.stringValue());
          }
        catch (ClassNotFoundException cnfe)
          {
            throw new ReteException(vv.get(0).stringValue(),
                                    "No such class",
                                    v.stringValue());
          }
      }
    if (c == null)
      {
        target = v.externalAddressValue();
        c = target.getClass();
      }        

    try
      {        
        Field f = c.getField(vv.get(2).stringValue());
        Class argType = f.getType();
        if (doSet)
          {
            f.set(target, ReflectFunctions.valueToObject(argType, vv.get(3)));
            return vv.get(3);
          }
        else
          {
            Object o = f.get(target);
            return ReflectFunctions.objectToValue(argType, o);
          }
      }
    catch (NoSuchFieldException nsfe)
      {
        throw new ReteException(vv.get(0).stringValue(),
                                "No such field " + vv.get(2).stringValue() +
                                " in class ", c.getName());
      }

    catch (IllegalAccessException iae)
      {
        throw new ReteException(vv.get(0).stringValue(),
                                "Field is not accessible",
                                vv.get(2).stringValue());
      }
    catch (IllegalArgumentException iae)
      {
        throw new ReteException(vv.get(0).stringValue(),
                                "Invalid argument",
                                vv.get(1).toString());
      }
  }
}

// **********************************************************************
// Tell Jess to prepare to match on properties of a Java class
// Generates a deftemplate from the class
// **********************************************************************

class defclass implements Userfunction, Clearable
{

  private int m_name = RU.putAtom("defclass");
  public int name() { return m_name; }
  
  // Keys are Jess class names; elements are the Java class names
  private Hashtable m_javaClasses = new Hashtable(101);

  public void clear() { m_javaClasses.clear(); }
  String jessNameToJavaName(String s) { return (String) m_javaClasses.get(s); }

  // SYNTAX: (defclass <jess-classname> <Java-classname>)
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    String jessName = vv.get(1).stringValue();
    String clazz = vv.get(2).stringValue();
    try
      {
        Class c = Class.forName(clazz);
        m_javaClasses.put(jessName, clazz);
        
        Deftemplate dt = new Deftemplate(jessName, RU.UNORDERED_FACT);
        dt.docstring("$JAVA-OBJECT$ " + clazz);
        
        // Make all the readable 'bean properties' into slots
        PropertyDescriptor [] props =
          Introspector.getBeanInfo(c).getPropertyDescriptors();
        for (int i=0; i<props.length; i++)
          {
            Method m = props[i].getReadMethod();
            if (m == null) continue;
            String name = props[i].getName();
            Class rt = m.getReturnType();
            if (rt.isArray())
              dt.addMultiSlot(name, new Value(props[i], RU.EXTERNAL_ADDRESS));
            else
              dt.addSlot(name, new Value(props[i], RU.EXTERNAL_ADDRESS));
          }
        
        // Last slot is special - it holds the active instance
        dt.addSlot("OBJECT", Funcall.NIL());

        // Install our synthetic deftemplate
        context.engine().addDeftemplate(dt);

        return new Value(clazz, RU.ATOM);
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("defclass", "Class not found:", clazz);
      }
    catch (IntrospectionException ie)
      {
        throw new ReteException("defclass", "Introspection error:",
                                ie.toString());
      }
  }
}

// **********************************************************************
// Tell Jess to match on properties of a specific Java object
// **********************************************************************

class definstance implements Userfunction, PropertyChangeListener,
         Clearable, Resetable
{
  Rete m_engine;
  defclass m_defclass;
  definstance(Rete engine, defclass dc)
  {
    m_engine = engine;
    m_defclass = dc;
  }
  
  private int m_name = RU.putAtom("definstance");
  public int name() { return m_name; }
  
  // Keys are objects to match, elements are the facts that represent them.
  private Hashtable m_facts = new Hashtable(101);
  
  // Keys are objects to match, elements are the Jess class names
  private Hashtable m_jessClasses = new Hashtable(101);


  Value undefine(Object o)
  {
    m_facts.remove(o);
    if (m_jessClasses.remove(o) == null)
      return Funcall.FALSE();
    else
      return Funcall.TRUE();
  }

  public void clear() { m_facts.clear(); m_jessClasses.clear(); }
  public void reset() throws ReteException
  {
    Enumeration e = m_facts.keys();
    while (e.hasMoreElements())
      {
        Object o = e.nextElement();
        createFact(o,
                   m_defclass.jessNameToJavaName((String)m_jessClasses.get(o)),
                   null);
      }
  }

  // SYNTAX: (definstance <jess-classname> <external-address>)
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    try
      {
        String jessTypename = vv.get(1).stringValue();

        if (m_defclass.jessNameToJavaName(jessTypename) == null)
          throw new ReteException("defclass", "Unknown object class",
                                  jessTypename);

        // Fetch the object
        Object o = vv.get(2).externalAddressValue();

        // Make sure we're not already matching on this object
        if (m_facts.get(o) != null)
          return Funcall.FALSE();

        String javaTypename = m_defclass.jessNameToJavaName(jessTypename);
        
        if (!o.getClass().isAssignableFrom(Class.forName(javaTypename)))
          throw new ReteException("defclass", "Object is not instance of",
                                  javaTypename);
    
        m_jessClasses.put(o, jessTypename);
        createFact(o, javaTypename, null);

        // Add ourselves to the object as a PropertyChangeListener 

        Class pcl = Class.forName("java.beans.PropertyChangeListener");
        Method apcl = o.getClass().getMethod("addPropertyChangeListener",
                                             new Class[] { pcl });    
        apcl.invoke( o, new Object[] { this });

        return Funcall.TRUE();
      }
    catch (InvocationTargetException ite)
      {
        throw new ReteException("definstance",
                                "Cannot add PropertyChangeListener",
                                ite.getTargetException().toString());
      }
    catch (NoSuchMethodException nsm)
      {
        throw new ReteException("definstance",
                                "Obj doesn't accept PropertyChangeListeners",
                                "");
      }
    catch (ClassNotFoundException cnfe)
      {
        throw new ReteException("definstance", "Class not found", "");
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("definstance",
                                "Class or method is not accessible",
                                "");
      }
  }
  
  private synchronized void createFact(Object o, String javaTypename,
                                       String changedName) 
       throws ReteException
  {
    // Synthesize a fact for this object; remember it for later retraction
    boolean propChange = false;
    try
      {
        Fact fact = (Fact) m_facts.get(o);
        if (fact != null)
          {
            m_engine.retract(fact.factData());
          }
        else
          {
            propChange = true;
            fact = new Fact((String) m_jessClasses.get(o),
                            RU.UNORDERED_FACT, m_engine);
            fact.addValue("OBJECT", new Value(o, RU.EXTERNAL_ADDRESS));
            m_facts.put(o,fact);
          }
        
        ValueVector deft = fact.deft();
        Object [] args = new Object[] {};
        for (int i=RU.FIRST_SLOT; i<deft.size(); i+=RU.DT_SLOT_SIZE)
          {
            if (deft.get(i + RU.DT_SLOT_NAME).stringValue().equals("OBJECT"))
              continue;
            PropertyDescriptor pd = (PropertyDescriptor)
              deft.get(i + RU.DT_DFLT_DATA).externalAddressValue();
            String name = pd.getName();
            Method m = pd.getReadMethod();
            Class rt = m.getReturnType();

            // changedName is null if multiple props changed or
            // completely new fact is desired
            if (changedName != null && !name.equals(changedName))
              continue;
            
            
            Object prop = m.invoke(o, args);
                
            Value oldV = fact.findValue(name);
            Value newV = ReflectFunctions.objectToValue(m.getReturnType(),
                                                        prop);
            if (!oldV.equals(newV))
              {
                fact.addValue(name, newV);
                propChange = true;
              }
            
          }
      
        
        if (propChange)
          m_engine.assert(fact.factData());
        
      }
    catch (InvocationTargetException ite)
      {
        throw new ReteException("call", "Called method threw an exception",
                                ite.getTargetException().toString()); 
      }
    catch (IllegalAccessException iae)
      {
        throw new ReteException("call", "Method is not accessible",
                                iae.toString());
      }
    catch (IllegalArgumentException iae)
      {
        throw new ReteException("call", "Invalid argument", iae.toString());
      }
}
  
  
  public synchronized void propertyChange(PropertyChangeEvent pce)
  {
    Object o = pce.getSource();
    
    try
      {
        String s = (String) m_jessClasses.get(o);
        if (s != null)
          createFact(o, m_defclass.jessNameToJavaName(s), pce.getPropertyName());
      }
    catch (ReteException re)
      {
        System.out.println("Async Error: " + re);
      }
  }

}

class undefinstance implements Userfunction
{
  private definstance m_di;
  
  undefinstance(definstance di)
  {
    m_di = di;
  }
  
  private int m_name = RU.putAtom("undefinstance");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    Value v = vv.get(1);
    return m_di.undefine(v.externalAddressValue());
  }
}
        
