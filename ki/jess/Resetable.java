/** **********************************************************************
 * Interface for things that like to know when reset is called
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public interface Resetable
{
  public void reset() throws ReteException;
}
