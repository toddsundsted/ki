/** **********************************************************************
  Some small sample user-defined math functions

  To use one of these functions from Jess, simply register the
  package class in your Java mainline:

  engine.AddUserpackage(new MathFunctions());

  $Id$
  @author Win Carus (C)1997
  ********************************************************************** */

package jess;

public class MathFunctions implements Userpackage
{
  public void add( Rete engine )
  {
    // abs added by Win Carus (9.19.97)
    engine.addUserfunction( new abs( ) );
    // div added by Win Carus (9.19.97)
    engine.addUserfunction( new div( ) );
    // float added by Win Carus (9.19.97)
    engine.addUserfunction( new _float( ) );
    // integer added by Win Carus (9.19.97)
    engine.addUserfunction( new _integer( ) );
    // max added by Win Carus (9.19.97)
    engine.addUserfunction( new max( ) );
    // min added by Win Carus (9.19.97)
    engine.addUserfunction( new min( ) );
    // ** added by Win Carus (9.19.97)
    engine.addUserfunction( new expt( ) );
    // exp added by Win Carus (9.19.97)
    engine.addUserfunction( new exp( ) );
    // log added by Win Carus (9.19.97)
    engine.addUserfunction( new log( ) );
    // log10 added by Win Carus (9.19.97)
    engine.addUserfunction( new log10( ) );
    // pi added by Win Carus (9.19.97)
    engine.addUserfunction( new pi( ) );
    // e added by Win Carus (9.19.97)
    // Note : this is NOT a part of standard CLIPS.
    engine.addUserfunction( new e( ) );
    // round added by Win Carus (9.19.97)
    engine.addUserfunction( new round( ) );
    // sqrt added by Win Carus (9.19.97)
    engine.addUserfunction( new sqrt( ) );
    // random added by Win Carus (9.19.97)
    engine.addUserfunction( new random( ) );
    // TBD : rad-deg
    // engine.AddUserfunction( new rad-deg( ) );
    // TBD : deg-rad
    // engine.AddUserfunction( new deg-rad( ) );
    // TBD : grad-deg
    // engine.AddUserfunction( new grad-deg( ) );
    // TBD : deg-grad
    // engine.AddUserfunction( new deg-grad( ) );
  }
}

class abs implements Userfunction
{
  private int m_name = RU.putAtom( "abs" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    Value v = vv.get( 1 );
    return new Value( Math.abs( v.numericValue( ) ), v.type() );
  }
}

class div implements Userfunction
{
  private int m_name = RU.putAtom( "div" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    int first = (int)vv.get( 1 ).numericValue( );
    int second = (int)vv.get( 2 ).numericValue( );

    return new Value( ( first / second ), RU.INTEGER );
  }
}

class _float implements Userfunction
{
  private int m_name = RU.putAtom( "float" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( vv.get( 1 ).numericValue( ), RU.FLOAT );
  }
}

class _integer implements Userfunction
{
  private int m_name = RU.putAtom( "integer" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( (int)vv.get( 1 ).numericValue( ), RU.INTEGER );
  }
}

class max implements Userfunction
{
  private int m_name = RU.putAtom( "max" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    Value v1 = vv.get(1);
    Value v2 = vv.get(2);
    int type = (v1.type() == RU.FLOAT || v2.type() == RU.FLOAT)
      ? RU.FLOAT: RU.INTEGER;
    return new Value( Math.max( v1.numericValue(), v2.numericValue()), type);
  }
}

class min implements Userfunction
{
  private int m_name = RU.putAtom( "min" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    Value v1 = vv.get(1);
    Value v2 = vv.get(2);
    int type = (v1.type() == RU.FLOAT || v2.type() == RU.FLOAT)
      ? RU.FLOAT: RU.INTEGER;
    return new Value( Math.min( v1.numericValue(), v2.numericValue()), type);
  }
}

class expt implements Userfunction
{
  private int m_name = RU.putAtom( "**" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( Math.pow( vv.get( 1 ).numericValue( ), vv.get( 2 ).numericValue( ) ), RU.FLOAT );
  }
}

class exp implements Userfunction
{
  private int m_name = RU.putAtom( "exp" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( Math.pow( Math.E, vv.get( 1 ).numericValue( ) ), RU.FLOAT );
  }
}

class log implements Userfunction
{
  private int m_name = RU.putAtom( "log" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( Math.log( (double)vv.get( 1 ).numericValue( ) ), RU.FLOAT );
  }
}

class log10 implements Userfunction
{
  private static final double log10 = Math.log( 10.0F );

  private int m_name = RU.putAtom( "log10" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( (Math.log( (double)vv.get( 1 ).numericValue( ) ) / log10 ), RU.FLOAT );
  }
}

class pi implements Userfunction
{
  private int m_name = RU.putAtom( "pi" );
  private static Value s_pi;
  static
  {
    try { s_pi= new Value( Math.PI, RU.FLOAT ); } catch (ReteException re) {}
  }

  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return s_pi;
  }
}

class e implements Userfunction
{
  private int m_name = RU.putAtom( "e" );
  private static Value s_e;
  static
  {
    try { s_e= new Value( Math.E, RU.FLOAT ); } catch (ReteException re) {}
  }
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return s_e;
  }
}

class round implements Userfunction
{
  private int m_name = RU.putAtom( "round" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( Math.round( vv.get( 1 ).numericValue( ) ), RU.INTEGER );
  }
}

class sqrt implements Userfunction
{
  private int m_name = RU.putAtom( "sqrt" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( Math.sqrt( vv.get( 1 ).numericValue( ) ), RU.FLOAT );
  }
}

class random implements Userfunction
{
  private int m_name = RU.putAtom( "random" );
  public int name( ) { return m_name; }

  public Value call( ValueVector vv, Context context ) throws ReteException
  {
    return new Value( (int) (Math.random( ) * 65536), RU.INTEGER );
  }
}

