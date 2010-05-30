/** **********************************************************************
 * Interface for things that like to know when clear is called
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

public interface Clearable
{
  public void clear() throws ReteException;
}
