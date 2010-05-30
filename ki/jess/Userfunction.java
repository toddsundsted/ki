/** **********************************************************************
 * Interface for user-defined functions
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public interface Userfunction
{
  public int name();  
  public Value call(ValueVector vv, Context context) throws ReteException;

}
