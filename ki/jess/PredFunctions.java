/** **********************************************************************
 * Predicate functions (is X of type Y?)
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public class PredFunctions implements Userpackage 
{
  public void add(Rete engine) 
  {
    engine.addUserfunction(new evenp());
    engine.addUserfunction(new oddp());
    engine.addUserfunction(new floatp());
    engine.addUserfunction(new integerp());
    engine.addUserfunction(new lexemep());
    engine.addUserfunction(new multifieldp());
    engine.addUserfunction(new numberp());
    engine.addUserfunction(new stringp());
    engine.addUserfunction(new symbolp());
  }
}



class evenp implements Userfunction 
{

  private int m_name = RU.putAtom("evenp");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {

    boolean b = ((((int) vv.get(1).numericValue()) % 2) == 0);
    return b ? Funcall.TRUE() : Funcall.FALSE();
  }
}

class oddp implements Userfunction 
{

  private int m_name = RU.putAtom("oddp");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {

    boolean b = ((((int) vv.get(1).numericValue()) % 2) == 0);
    return b ? Funcall.FALSE() : Funcall.TRUE();
  }
}

// For the moment, the definition of an integer is "rounded version
// equal to original". This is subject to change.

class floatp implements Userfunction 
{

  private int m_name = RU.putAtom("floatp");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {
    return (vv.get(1).type() == RU.FLOAT) ? Funcall.TRUE() : Funcall.FALSE();
  }
}

class integerp implements Userfunction 
{

  private int m_name = RU.putAtom("integerp");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {
    return (vv.get(1).type() == RU.INTEGER) ? Funcall.TRUE() : Funcall.FALSE();
  }
}

class lexemep implements Userfunction 
{

  private int m_name = RU.putAtom("lexemep");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {

    if (vv.get(1).type() == RU.ATOM ||
        vv.get(1).type() == RU.STRING)
      return Funcall.TRUE();
    else
      return Funcall.FALSE();
  }
}

class multifieldp implements Userfunction 
{

  private int m_name = RU.putAtom("multifieldp");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {

    if (vv.get(1).type() == RU.LIST)
      return Funcall.TRUE();
    else
      return Funcall.FALSE();
  }
}

class numberp implements Userfunction 
{

  private int m_name = RU.putAtom("numberp");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {
    if (vv.get(1).type() == RU.INTEGER ||
        vv.get(1).type() == RU.FLOAT)
      return Funcall.TRUE();
    else
      return Funcall.FALSE();
  }
}

class stringp implements Userfunction 
{

  private int m_name = RU.putAtom("stringp");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {
    if (vv.get(1).type() == RU.STRING)
      return Funcall.TRUE();
    else
      return Funcall.FALSE();
  }
}

class symbolp implements Userfunction 
{

  private int m_name = RU.putAtom("symbolp");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException 
  {
    if (vv.get(1).type() == RU.ATOM)
      return Funcall.TRUE();
    else
      return Funcall.FALSE();
  }
}






