
package etcee.ki.server;

import java.io.IOException;

/**
 * The repository exception.
 *
 */

class RepositoryException
  extends IOException
{
  /**
   * Constructs the exception.
   *
   */

  RepositoryException()
  {
    super();
  }

  /**
   * Constructs the exception.
   *
   */

  RepositoryException(String str)
  {
    super(str);
  }
}
