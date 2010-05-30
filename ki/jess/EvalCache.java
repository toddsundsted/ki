/** **********************************************************************
 * A class to hold a cache of Values and FUncalls for reuse
 * The cache keeps track of values it has handed out, so it can restore itself.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

class EvalCache
{
  private ValueStack m_usedV, m_newV;
  private FuncallStack m_usedF, m_newF;

  EvalCache() 
  {
    m_usedV = new ValueStack();
    m_newV = new ValueStack();
    m_usedF = new FuncallStack();
    m_newF = new FuncallStack();
  }

  final synchronized Value getValue() throws ReteException
  {
    return  m_usedV.push(m_newV.pop());    
  }

  final int markValue() { return m_usedV.size(); }

  final synchronized void restoreValue(int mark) throws ReteException
  {
    while (m_usedV.size() > mark)
      {
        m_newV.push(m_usedV.pop());
      }
  }

  final synchronized Funcall getFuncall() throws ReteException
  {
    return  m_usedF.push(m_newF.pop());    
  }

  final int markFuncall() { return m_usedF.size(); }

  final synchronized void restoreFuncall(int mark) throws ReteException
  {
    while (m_usedF.size() > mark)
      {
        m_newF.push(m_usedF.pop());
      }
  }
  
}


class ValueStack
{
  private Value [] m_v = new Value[30];
  private int m_ptr = 0;
  
  Value push(Value v)
  {
    if (m_ptr >= m_v.length)
      {
        Value [] tmp = new Value[m_v.length * 2];
        System.arraycopy(m_v, 0, tmp, 0, m_v.length);
        m_v = tmp;
      }
    return m_v[m_ptr++] = v;
  }

  Value pop() throws ReteException
  {
    if (m_ptr > 0)
      return m_v[--m_ptr];
    else
      return new Value(0, RU.INTEGER);
  }
  
  int size() { return m_ptr; }

}


class FuncallStack
{
  private Funcall [] m_v = new Funcall[30];
  private int m_ptr = 0;
  
  Funcall push(Funcall v)
  {
    if (m_ptr >= m_v.length)
      {
        Funcall [] tmp = new Funcall[m_v.length * 2];
        System.arraycopy(m_v, 0, tmp, 0, m_v.length);
        m_v = tmp;
      }
    return m_v[m_ptr++] = v;
  }

  Funcall pop() throws ReteException
  {
    if (m_ptr > 0)
      return m_v[--m_ptr];
    else
      return new Funcall(10);
  }
  
  int size() { return m_ptr; }

}



