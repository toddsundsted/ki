/** **********************************************************************
 * Example user-defined functions for the Jess Expert System Shell
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public class StringFunctions implements Userpackage
{

  public void add( Rete engine )
  {
    engine.addUserfunction( new lowcase( ) );
    engine.addUserfunction( new strcat( ) );
    engine.addUserfunction( new strcompare( ) );
    engine.addUserfunction( new strindex( ) );
    engine.addUserfunction( new strlength( ) );
    engine.addUserfunction( new substring( ) );
    engine.addUserfunction( new upcase( ) );
  }
}


class strcat implements Userfunction
{
  private int m_name = RU.putAtom( "str-cat" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {

    if (vv.size() == 2 && vv.get(1).type() == RU.STRING)
      return vv.get(1);

    StringBuffer buf = new StringBuffer( "" );
      
    for ( int i = 1; i < vv.size( ); i++ )
      {
        Value v = vv.get(i);
        if (v.type() == RU.STRING)
          buf.append( v.stringValue());
        else
          buf.append (v.toString());
      }
      
    return new Value( buf.toString( ), RU.STRING );
      
  }
}

class upcase implements Userfunction
{
  private int m_name = RU.putAtom( "upcase" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( vv.get( 1 ).stringValue( ).toUpperCase( ), RU.STRING );
  }
}

class lowcase implements Userfunction
{
  private int m_name = RU.putAtom( "lowcase" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( vv.get( 1 ).stringValue( ).toLowerCase( ), RU.STRING );
  }
}

class strcompare implements Userfunction
{
  private int m_name = RU.putAtom( "str-compare" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( vv.get( 1 ).stringValue( ).compareTo( vv.get( 2 ).stringValue( ) ), RU.INTEGER );
  }
}

class strindex implements Userfunction
{
  private int m_name = RU.putAtom( "str-index" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    int rv = vv.get( 2 ).stringValue( ).indexOf( vv.get( 1 ).stringValue( ) );
    return rv == -1 ? Funcall.FALSE( ) : new Value( rv + 1, RU.INTEGER );
  }
}

class strlength implements Userfunction
{
  private int m_name = RU.putAtom( "str-length" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( vv.get( 1 ).stringValue( ).length( ), RU.INTEGER );
  }
}

class substring implements Userfunction
{
  private int m_name = RU.putAtom( "sub-string" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    int begin = (int) vv.get( 1 ).numericValue( ) -1;
    int end = (int) vv.get( 2 ).numericValue( );
    String s = vv.get( 3 ).stringValue( );

    if (begin < 0 || begin > s.length() - 1 ||
        end > s.length() || end <= 0)
      throw new ReteException("sub-string",
                              "Indices must be between 1 and " +
                              s.length(), "");
    return new Value(s.substring(begin, end), RU.STRING); 
  }
}

