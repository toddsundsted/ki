/** **********************************************************************
 * Some small sample user-defined multifield functions
 * Many functions contributed by Win Carus (Win_Carus@inso.com)
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
import java.util.Hashtable;

public class MultiFunctions implements Userpackage
{
  public void add(Rete engine)
  {
    engine.addUserfunction(new createmf());
    engine.addUserfunction(new deletemf());
    engine.addUserfunction(new explodemf());
    engine.addUserfunction(new firstmf());
    // implode$ added by Win Carus (9.17.97)
    engine.addUserfunction(new implodemf());
    // insert$ added by Win Carus (9.17.97)
    engine.addUserfunction(new insertmf());
    engine.addUserfunction(new lengthmf());
    engine.addUserfunction(new membermf());
    engine.addUserfunction(new nthmf());
    // replace$ added by Win Carus (9.17.97)
    engine.addUserfunction(new replacemf());
    engine.addUserfunction(new restmf());
    // subseq$ added by Win Carus (9.17.97)
    engine.addUserfunction(new subseqmf());
    // subsetp added by Win Carus (9.17.97); revised (10.2.97)
    engine.addUserfunction(new subsetp());
    // union added by Win Carus (10.2.97)
    engine.addUserfunction(new union());
    // intersection added by Win Carus (10.2.97)
    engine.addUserfunction(new intersection());
    // complement added by Win Carus (10.2.97)
    engine.addUserfunction(new complement());
  }
}

class createmf implements Userfunction
{
  private int m_name = RU.putAtom( "create$" );
  public int name() { return m_name; }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector mf = new ValueVector();

    for ( int i = 1; i < vv.size( ); i++ )
      {
        switch ( vv.get( i ).type( ) )
          {
          case RU.LIST:
            ValueVector list = vv.get( i ).listValue( );
            for ( int k = 0; k < list.size( ); k++ )
              {
                mf.add( list.get( k ) );
              }
            break;
          default:
            mf.add( vv.get(i ) );
            break;
          }
      }
    return new Value( mf, RU.LIST );
  }
}

class deletemf implements Userfunction
{
  private int m_name = RU.putAtom( "delete$" );
  public int name( ) { return m_name; }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector newmf = new ValueVector( );

    ValueVector mf = vv.get( 1 ).listValue( );
    int begin = (int) vv.get( 2 ).numericValue( );
    int end = (int) vv.get( 3 ).numericValue( );

    if (end < begin || begin < 1 || end > mf.size())
      throw new ReteException( "delete$",
                               "invalid range",
                               "(" + begin + "," + end + ")");      
    for ( int i = 0; i < mf.size( ); i++ )
      {
        if ( i >= (begin-1) && i <= (end-1) )
          {
            continue;
          }
        newmf.add(mf.get(i));
      }

    return new Value( newmf, RU.LIST );
  }
}

class firstmf implements Userfunction
{
  private int m_name = RU.putAtom( "first$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).listValue( );
    ValueVector newmf = new ValueVector(1);
    if (mf.size() > 0)
      newmf.add(mf.get( 0 ));
    return new Value(newmf, RU.LIST);
  }
}

class implodemf implements Userfunction
{
  private int m_name = RU.putAtom( "implode$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).listValue( );

    StringBuffer buf = new StringBuffer( "" );

    for ( int i = 0; i < mf.size( ); i++ )
      {
        buf.append( mf.get( i ).stringValue( ) + " ");
      }

    String result = buf.toString( );
    int len = result.length( );

    if ( len == 0 )
      return new Value( result, RU.STRING );
    else
      return new Value( result.substring( 0, len - 1 ), RU.STRING );
  }
}

class insertmf implements Userfunction
{
  private int m_name = RU.putAtom( "insert$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).listValue( );
    int idx = (int) vv.get( 2 ).numericValue( );
    ValueVector insertedmf = vv.get( 3 ).listValue( );

    if ( idx < 1 || idx > mf.size( ) + 1 )
      {
        throw new ReteException( "insert$", "index must be >= 1 and <= " + (mf.size( ) + 1), ": " + idx );
      }

    ValueVector newmf = new ValueVector( );
    // adjust for zero indexing
    --idx;
    for ( int i = 0; i < idx; i++ )
      {
        newmf.add( mf.get( i ) );
      }

    for ( int j = 0; j < insertedmf.size( ); j++ )
      {
        newmf.add( insertedmf.get( j ) );
      }

    for ( int i = idx; i < mf.size( ); i++ )
      {
        newmf.add( mf.get( i ) );
      }

    return new Value( newmf, RU.LIST );
  }
}

class nthmf implements Userfunction
{
  private int m_name = RU.putAtom( "nth$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    int idx = (int) vv.get( 1 ).numericValue( );

    if ( idx < 1 )
      {
        throw new ReteException( "nth$", "index must be > 0", "" + idx );
      }
    ValueVector mf = vv.get( 2 ).listValue( );

    if ( idx > mf.size( ) )
      {
        throw new ReteException( "nth$", "index out of bounds", "" + idx );
      }

    return mf.get( idx - 1 );
  }
}

class lengthmf implements Userfunction
{
  private int m_name = RU.putAtom( "length$" );
  public int name() { return m_name; }

  public Value call(ValueVector vv, Context context) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).listValue( );
    return new Value( mf.size( ), RU.INTEGER );
  }
}

class replacemf implements Userfunction
{
  private int m_name = RU.putAtom( "replace$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {

    ValueVector mf = vv.get( 1 ).listValue( );
    int startIdx = (int) vv.get( 2 ).numericValue( );
    int endIdx = (int) vv.get( 3 ).numericValue( );
    ValueVector insertedmf = vv.get( 4 ).listValue( );

    if ( startIdx < 1 || startIdx > mf.size( ) + 1 ||
         endIdx < 1 || endIdx > mf.size( ) + 1 || startIdx > endIdx )
      {
        throw new ReteException( "replace$", "index must be >= 1 and <= " +
                                 (mf.size( ) + 1),
                                 ": " + startIdx + " " + endIdx);
      }

    ValueVector newmf = new ValueVector( );

    // adjust for 0-based
    --startIdx;
    --endIdx;

    for ( int i = 0; i <= startIdx - 1; i++ )
      {
        newmf.add( mf.get( i ) );
      }
    
    for ( int j = 0; j < insertedmf.size( ); j++ )
      {
        newmf.add( insertedmf.get( j ) );
      }

    for ( int i = endIdx + 1; i < mf.size( ); i++ )
      {
        newmf.add( mf.get( i ) );
      }

    return new Value( newmf, RU.LIST );
  }
}

class restmf implements Userfunction
{
  private int m_name = RU.putAtom( "rest$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector newmf = new ValueVector( );

    ValueVector mf = vv.get( 1 ).listValue( );

    for ( int i = 1; i < mf.size( ); i++ )
      {
        newmf.add( mf.get( i ) );
      }

    return new Value( newmf, RU.LIST );
  }
}

class membermf implements Userfunction
{
  private int m_name = RU.putAtom( "member$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    Value target = vv.get( 1 );
    ValueVector list = vv.get( 2 ).listValue( );

    for ( int i = 0; i < list.size( ); i++ )
      {
        if ( target.equals( list.get( i ) ) )
          {
            return new Value( i + 1, RU.INTEGER );
          }
      }
    return Funcall.FALSE( );
  }
}

class subseqmf implements Userfunction
{
  private int m_name = RU.putAtom( "subseq$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector mf = vv.get( 1 ).listValue( );
    int startIdx = (int) vv.get( 2 ).numericValue( );
    int endIdx = (int) vv.get( 3 ).numericValue( );

    // Note: multifield indices are 1-based, not 0-based.

    if ( startIdx < 1 )
      startIdx = 1;

    if ( endIdx > mf.size( ) )
      endIdx = mf.size( );

    ValueVector newmf = new ValueVector( );

    if ( startIdx <= mf.size( ) &&
         endIdx <= mf.size( ) &&
         startIdx <= endIdx )
      {
        if ( startIdx == endIdx )
          {
            newmf.add( mf.get( startIdx - 1 ) );
          }
        else
          {
            for ( int i = startIdx; i <= endIdx; ++i )
              {
                newmf.add( mf.get( i - 1 ) );
              }
          }
      }
    return new Value( newmf, RU.LIST );
  }
}

class subsetp implements Userfunction
{
  private int m_name = RU.putAtom( "subsetp" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector firstmf = vv.get( 1 ).listValue( );
    ValueVector secondmf = vv.get( 2 ).listValue( );

    // if ( the first multifield is empty ) then return TRUE
    if ( firstmf.size( ) == 0 )
      {
        return Funcall.TRUE( );
      }

    // if ( the second multifield is empty and the first is not )
    // then return FALSE.
    if ( secondmf.size( ) == 0 )
      {
        return Funcall.FALSE( );
      }

    // if ( one member in the first multifield is not in the second multifield )
    // then return FALSE else return TRUE
        
    Hashtable ht = new Hashtable( );
    Integer value = new Integer( 0 );

    for ( int i = 0; i < secondmf.size( ); ++i )
      {
        String key = secondmf.get( i ).toString( ) + secondmf.get( i ).type( );
        if ( !ht.containsKey( key ) )
          {
            ht.put( key, value );
          }
      }
        
    for ( int i = 0; i < firstmf.size( ); ++i )
      {
        String key = firstmf.get( i ).toString( ) + firstmf.get( i ).type( );

        if ( !ht.containsKey( key ) )
          {
            return Funcall.FALSE( );
          }
      }
    return Funcall.TRUE( );
  }
}
    
class union implements Userfunction
{
  private int m_name = RU.putAtom( "union$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector firstmf = vv.get( 1 ).listValue( );
    ValueVector secondmf = vv.get( 2 ).listValue( );
        
    ValueVector newmf = new ValueVector( );
        
    Hashtable ht = new Hashtable( );
    Integer value = new Integer( 0 );

    for ( int i = 0; i < firstmf.size( ); ++i )
      {
        newmf.add( firstmf.get( i ) );
        // key concatenates the String representation of the element
        // and its type; this would be better handled by overloading
        // the hashCode( ) method for all RU data types
        String key = firstmf.get( i ).toString( ) + firstmf.get( i ).type( );            
        ht.put( key, value );
      }
            
    for ( int i = 0; i < secondmf.size( ); ++i )
      {
        String key = secondmf.get( i ).toString( ) + secondmf.get( i ).type( );            
        if ( !ht.containsKey( key ) )
          {
            newmf.add( secondmf.get( i ) );
          }
      }
    return new Value( newmf, RU.LIST );
  }
}

class intersection implements Userfunction
{
  private int m_name = RU.putAtom( "intersection$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector firstmf = vv.get( 1 ).listValue( );
    ValueVector secondmf = vv.get( 2 ).listValue( );
        
    ValueVector newmf = new ValueVector( );
        
    Hashtable ht = new Hashtable( );
    Integer value = new Integer( 0 );

    for ( int i = 0; i < firstmf.size( ); ++i )
      {
        String key = firstmf.get( i ).toString( ) + firstmf.get( i ).type( );            
        ht.put( key, value );
      }
            
    for ( int i = 0; i < secondmf.size( ); ++i )
      {
        String key = secondmf.get( i ).toString( ) + secondmf.get( i ).type( );            
        if ( ht.containsKey( key ) )
          {
            newmf.add( secondmf.get( i ) );
          }
      }
    return new Value( newmf, RU.LIST );
  }
}

class complement implements Userfunction
{
  private int m_name = RU.putAtom( "complement$" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector firstmf = vv.get( 1 ).listValue( );
    ValueVector secondmf = vv.get( 2 ).listValue( );
        
    ValueVector newmf = new ValueVector( );
        
    Hashtable ht = new Hashtable( );
    Integer value = new Integer( 0 );

    for ( int i = 0; i < firstmf.size( ); ++i )
      {
        String key = firstmf.get( i ).toString( ) + firstmf.get( i ).type( );            
        ht.put( key, value );
      }
            
    for ( int i = 0; i < secondmf.size( ); ++i )
      {
        String key = secondmf.get( i ).toString( ) + secondmf.get( i ).type( );            
        if ( !ht.containsKey( key ) )
          {
            newmf.add( secondmf.get( i ) );
          }
      }
         
    return new Value( newmf, RU.LIST );
  }
}


class explodemf implements Userfunction
{
  private int m_name = RU.putAtom( "explode$" );

  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    ValueVector retval = new ValueVector();
    
    StringBufferInputStream sbis 
      = new StringBufferInputStream(vv.get(1).stringValue());
    JessTokenStream jts = new JessTokenStream(new DataInputStream(sbis));

    JessToken jt = jts.getOneToken();

    while (jt.m_ttype != RU.NONE)
      {
        // Turn the token into a value.
        switch (jt.m_ttype) 
          {
          case RU.ATOM:
          case RU.STRING:
            retval.add(new Value(jt.m_sval, jt.m_ttype)); break; 
          case RU.FLOAT:
          case RU.INTEGER:
            retval.add(new Value(jt.m_nval, jt.m_ttype)); break; 
          default:
            retval.add(new Value(RU.putAtom("" + (char) jt.m_ttype),
                                 RU.STRING)); break;
          }
        jt = jts.getOneToken();
      }
    return new Value(retval, RU.LIST);

  }
}
