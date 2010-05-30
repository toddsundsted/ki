/** **********************************************************************
 * A class to represent a Jess typed value;  Does some 'type conversions'
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public final class Value 
{
  private final int STRING_TYPES = RU.ATOM | RU.STRING | RU.VARIABLE |
                                   RU.MULTIVARIABLE | RU.SLOT | RU.MULTISLOT;
  private final int NUM_TYPES = RU.INTEGER | RU.FLOAT;
  private final int FACT_TYPES = RU.FACT_ID | NUM_TYPES;

  private int             m_type;
  private int            m_intval;
  private double       m_floatval;
  private Object      m_objectval;

  public Value(int value, int type) throws ReteException 
  {
    resetValue(value, type);
  }
  
  public Value(Value v) 
  {
    resetValue(v);
  }

  public Value(String s, int type) throws ReteException 
  {
    resetValue(s, type);
  }

  public Value(ValueVector f, int type) throws ReteException 
  {
    resetValue(f, type);
  }

  public Value(double d, int type) throws ReteException 
  {
    resetValue(d, type);
  }

  public Value(Object o, int type) throws ReteException 
  {
    resetValue(o, type);
  }

  public Value(int[] a, int type) throws ReteException 
  {
    resetValue(a, type);
  }

  public int[] intArrayValue() throws ReteException 
  {
    if (m_type == RU.INTARRAY)
      return (int[]) m_objectval;
    throw new ReteException("Value::intArrayValue",
                            "Not an int[]: " + toString(), 
                            "type = " + m_type);

  }

  public final Object externalAddressValue() throws ReteException 
  {
    switch (m_type)
      {
      case RU.EXTERNAL_ADDRESS:
        return m_objectval;
      case RU.STRING:
      case RU.ATOM:
        return stringValue().intern();
      }

    throw new ReteException("Value::externalAddressValue",
                            "Not an external address: " + toString(),
                            "type = " + m_type);

  }


  public final Funcall funcallValue() throws ReteException 
  {
    if (m_type == RU.FUNCALL)
      return (Funcall) m_objectval;
    throw new ReteException("Value::funcallValue",
                            "Not a Funcall: " + toString(),
                            "type = " + m_type);
  }

  public final ValueVector factValue() throws ReteException 
  {
    if (m_type == RU.ORDERED_FACT || m_type == RU.UNORDERED_FACT)
      return (ValueVector) m_objectval;
    throw new ReteException("Value::factValue",
                            "Not a Fact: " + toString(),
                            "type = " + m_type);


  }

  public final ValueVector listValue() throws ReteException 
  {
    if (m_type == RU.LIST)
      return (ValueVector) m_objectval;
    throw new ReteException("Value::listValue",
                            "Not a List: " + toString(), 
                            "type = " + m_type);


  }

  public final double numericValue() throws ReteException 
  {
    if (m_type == RU.FLOAT)
      return m_floatval;
    else if (m_type == RU.INTEGER)
      return (double) m_intval;

    throw new ReteException("Value::numericValue",
                            "Not a number: " + toString(),
                            "type = " + m_type);
      
  }


  public final int descriptorValue() throws ReteException 
  {
    if (m_type == RU.DESCRIPTOR)
      return m_intval;

    throw new ReteException("Value::descriptorValue",
                            "Not a descriptor: " + toString(), 
                            "type = " + m_type);
  }

  public final int intValue() throws ReteException 
  {
    if (m_type == RU.INTEGER)
      return m_intval;
    else if (m_type == RU.FLOAT)
      return (int) m_floatval;
    else
      throw new ReteException("Value::intValue",
                              "Not a number: " + toString(),
                              "type = " + m_type);
      
  }

  public final double floatValue() throws ReteException 
  {
    return numericValue();
  }

  public final String stringValue() throws ReteException 
  {
    if ((m_type & STRING_TYPES) != 0)
      return RU.getAtom(m_intval);
    throw new ReteException("Value::stringValue",
                            "Not a string: " + toString(),
                            "type = " + m_type);
  }

  public final int atomValue() throws ReteException 
  {
    if ((m_type & STRING_TYPES) != 0)
      return m_intval;
    throw new ReteException("Value::atomValue",
                            "Not an atom: " + toString(),
                            "type = " + m_type);
  }

  public final int variableValue() throws ReteException 
  {
    if (m_type == RU.VARIABLE || m_type == RU.MULTIVARIABLE)
      return m_intval;
    throw new ReteException("Value::variableValue",
                            "Not a Variable: " + toString(),
                            "type = " + m_type);
  }

  public final int factIDValue() throws ReteException 
  {
    if ((m_type & FACT_TYPES) != 0)
      return m_intval;
    throw new ReteException("Value::factIDValue",
                            "Not a Fact-ID: " + toString(),
                            "type = " + m_type);
  }

  private String escape(String s)
  {
    if (s.indexOf('"') == -1)
      return s;
    else
      {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i< s.length(); i++)
          {
            char c = s.charAt(i);
            if (c == '"' || c == '\\')
              sb.append('\\');
            sb.append(c);
          }
        return sb.toString();
      }
  }

  public final String toString() 
  {
    switch (m_type) 
      {
      case RU.INTEGER:
        return "" + m_intval;
      case RU.FLOAT:
        return "" + m_floatval;
      case RU.STRING:
        return "\"" + escape(RU.getAtom(m_intval)) + "\"";
      case RU.ATOM:
      case RU.SLOT:
      case RU.MULTISLOT:
        return RU.getAtom(m_intval);
      case RU.VARIABLE:
        return "?" + RU.getAtom(m_intval);
      case RU.MULTIVARIABLE:
        return "$?" + RU.getAtom(m_intval);
      case RU.FACT_ID:
        return ("<Fact-" + m_intval + ">");
      case RU.DESCRIPTOR:
        return (m_intval == RU.ORDERED_FACT) ? "<ordered>" : "<unordered>";
      case RU.FUNCALL:
      case RU.LIST:
      case RU.UNORDERED_FACT:
      case RU.ORDERED_FACT:
        return m_objectval.toString();
      case RU.INTARRAY: 
        {
          int[] binding = (int[]) m_objectval;
          return "?" + binding[0] + "," + binding[1] + "," + binding[2];
        }
      case RU.EXTERNAL_ADDRESS:
        return "<External-Address:" + m_objectval.getClass().getName() + ">";
      case RU.NONE:
        return Funcall.NIL().toString();
      default:
        return "<UNKNOWN>";
      }
  }


  public final int sortCode() 
  {
    switch (m_type) 
      {
      case RU.INTEGER:
      case RU.STRING:
      case RU.ATOM:
      case RU.SLOT:
      case RU.MULTISLOT:
      case RU.FACT_ID:
      case RU.VARIABLE:
      case RU.MULTIVARIABLE:
      case RU.DESCRIPTOR:
        return m_intval;
      case RU.FLOAT:
        return (int) m_floatval;
      case RU.FUNCALL:
      case RU.UNORDERED_FACT:
      case RU.ORDERED_FACT:
        return ((ValueVector)m_objectval).get(0).m_intval;
      case RU.LIST:
        return ((ValueVector)m_objectval).get(0).sortCode();
      default:
        return 0;
      }
  }


  public final int type() 
  {
    return m_type;
  }

  final int ITYPES = RU.STRING | RU.ATOM | RU.SLOT | RU.MULTISLOT |
  RU.FACT_ID | RU.VARIABLE | RU.MULTIVARIABLE | RU.DESCRIPTOR | RU.INTEGER;

  final int VTYPES = RU.FUNCALL | RU.ORDERED_FACT | RU.UNORDERED_FACT |
  RU.LIST;

  final public boolean equals(Object v) 
  {
    if (v instanceof Value)
      return equals((Value) v);
    else
      return false;
  }

  final public boolean equals(Value v) 
  {

    if (this == v)
      return true;

    int t = m_type;
    if (v.m_type != t)
      return false;

    if ((t & ITYPES) != 0)
      return (m_intval == v.m_intval);

    if (t == RU.FLOAT)
      return (m_floatval == v.m_floatval);

    return m_objectval.equals(v.m_objectval);

  }
  
  // Like above, but returns true for 3 == 3.0

  final public boolean equalsStar(Value v) throws ReteException
  {
    if (this == v)
      return true;

    if ((m_type & NUM_TYPES) != 0 && (v.m_type & NUM_TYPES) != 0)
      return (numericValue() == v.numericValue());

    else
      return equals(v);
  }
  
  // Value Recycling - not public (for internal use only)

  final Value resetValue(int value, int type) throws ReteException 
  {
    m_type = type;
    switch (m_type) 
      {
      case RU.ATOM:
      case RU.SLOT:
      case RU.MULTISLOT:
      case RU.NONE:
      case RU.STRING:
      case RU.INTEGER:
      case RU.FACT_ID:
      case RU.MULTIVARIABLE:
      case RU.VARIABLE:
      case RU.DESCRIPTOR:
        m_intval = value; break;

      default:
        throw new ReteException("Value::resetValue",
                                "Not an integral type",
                                "type = " + m_type);
      }
    return this;
  }
  
  final Value resetValue(Value v) 
  {
    m_type = v.m_type;
    m_intval = v.m_intval;
    m_floatval = v.m_floatval;
    m_objectval = v.m_objectval;
    return this;
  }

  final Value resetValue(String s, int type) throws ReteException 
  {
    if ((type & STRING_TYPES) == 0)
      throw new ReteException("Value::Value",
                              "not a string type",
                              "type = " + m_type);

    m_type = type;
    m_intval = RU.putAtom(s);
    return this;
  }

  final Value resetValue(ValueVector f, int type) throws ReteException 
  {
    if (type != RU.FUNCALL && type != RU.ORDERED_FACT &&
        type != RU.UNORDERED_FACT && type != RU.LIST)
      throw new ReteException("Value::Value",
                              "not a vector type",
                              "type = " + m_type);

    m_type = type;
    m_objectval = f;
    return this;
  }

  final Value resetValue(double d, int type) throws ReteException 
  {
    if (type != RU.FLOAT && type != RU.INTEGER  && type != RU.FACT_ID)
      throw new ReteException("Value::Value",
                              "not a float type:",
                              "type = " + m_type);
    m_type = type;
    if (type == RU.FLOAT)
      m_floatval = d;
    else
      m_intval = (int) d;
    
    return this;
  }

  final Value resetValue(Object o, int type) throws ReteException 
  {
    if (type != RU.EXTERNAL_ADDRESS)
      throw new ReteException("Value::Value",
                              "Not an External Address type",
                              "type = " + m_type);

    m_type = type;
    m_objectval = o;
    return this;
  }

  final Value resetValue(int[] a, int type) throws ReteException 
  {
    if (type != RU.INTARRAY)
      throw new ReteException("Value::Value",
                              "Not an int[] type",
                              "type = " + m_type);
    m_type = type;
    m_objectval = a;
    return this;
  }

}

