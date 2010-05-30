// -*- java -*-
//////////////////////////////////////////////////////////////////////
// ValueVector.java
// A Vector that holds Values only.
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
// $Id: ValueVector.java,v 1.4 1997/07/04 19:55:25 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package jess;

/**
 A mini version of Vector that only holds Values.
@author E.J. Friedman-Hill (C)1996
*/

public class ValueVector implements Cloneable
{
  Value[] m_v;
  int m_ptr = 0;

  public ValueVector()
  {
    this(10);
  }

  public ValueVector(int size)
  {
    m_v = new Value[size];
  }

  public final int size()
  {
    return m_ptr;
  }

  public Object clone()
  {
    ValueVector vv = new ValueVector(m_ptr);
    vv.m_ptr = m_ptr;
    System.arraycopy(m_v, 0, vv.m_v, 0, m_ptr);
    return vv;
  }

  public final Value get(int i)
  {
    // let Java throw the exception
    return m_v[i];
  }

  public final void setLength(int i) 
  {
    if (i > m_v.length) 
      {
        Value[] nv = new Value[i];
        System.arraycopy(m_v, 0, nv, 0, m_v.length);
        m_v = nv;
      }
    m_ptr = i;
  }


  public final void set(Value val, int i) 
  {
    // let Java throw the exception
    m_v[i] = val;
  }

  public final void add(Value val) 
  {
    if (m_ptr >= m_v.length) 
      {
        Value[] nv = new Value[m_v.length * 2];
        System.arraycopy(m_v, 0, nv, 0, m_v.length);
        m_v = nv;
      }
    m_v[m_ptr++] = val;
  }

  // Important that argtype is Object here!
  public boolean equals(Object o) 
  {
    if (this == o)
      return true;
    
    else if (! (o instanceof ValueVector) )
      return false;
    
    ValueVector vv = (ValueVector) o;

    if (m_ptr != vv.m_ptr)
      return false;
    
    for (int i=m_ptr -1; i>-1; i--)
      if (!m_v[i].equals(vv.m_v[i]))
        return false;

    return true;
  }

  public String toString() 
  {
    StringBuffer sb = new StringBuffer(100);
    for (int i=0; i < m_ptr; i++) 
      {
        if (i > 0)
          sb.append(" ");
        sb.append(m_v[i]);
      }
    return sb.toString();
  }


}


