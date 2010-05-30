
package etcee.ki.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import java.util.Hashtable;

/**
 * The agent class loader.
 *
 * This class <EM>is</EM> thread safe.
 *
 */

final class AgentClassLoader
  extends ClassLoader
{
  /**
   * The zipfile.
   *
   * The class loader loads agent classes from the zipfile.
   *
   */

  private ZipFile zipfile = null;

  /**
   * The cache.
   *
   * The class loader caches loaded agent classes in the hashtable.
   *
   */

  private Hashtable hashtableClasses = new Hashtable();

  /**
   * The first time indicator.
   *
   * Things run a bit loosely the first time through the load.  In
   * particular, the agent class loader can load both the wrapper
   * class and its supertype.
   *
   */

  private boolean boolFirstTime = true;

  /**
   * Constructs the agent class loader.
   *
   */

  AgentClassLoader(File file)
    throws IOException
  {
    zipfile = new ZipFile(file);
  }

  /**
   * Deconstructs the agent class loader.
   *
   */

  final void
  deconstruct()
    throws IOException
  {
    zipfile.close();
  }

  /**
   * Transforms a classname into a filename.
   *
   */

  private final String
  transform(String strClassName)
  {
    return strClassName.replace('.', '/') + ".class";
  }

  /**
   * Checks the package.
   *
   */

  private final boolean
  checkPackage(String strClassName)
  {
    if (strClassName.equalsIgnoreCase("etcee.ki.server.AgentRoot"))
    {
      return true;
    }

    if (boolFirstTime && strClassName.equalsIgnoreCase("etcee.ki.server.AgentWrapper"))
    {
      boolFirstTime = false;

      return true;
    }

    if (strClassName.toLowerCase().startsWith("etcee.ki.agenthost") ||
        strClassName.toLowerCase().startsWith("etcee.ki.server"))
    {
      return false;
    }

    return true;
  }

  /**
   * Loads the wrapper class.
   *
   * This method loads the wrapper class.  It works once.
   *
   */

  private final Class
  loadWrapperClass(String strClassName)
    throws ClassNotFoundException
  {
    if (!boolFirstTime || !strClassName.equals(""))
    {
      return null;
    }

    byte [] rgb = null;

    try
    {
      FileInputStream fileinputstream = new FileInputStream("Wrapper.class");

      int n = fileinputstream.available();

      rgb = new byte [n];

      fileinputstream.read(rgb);
    }
    catch (IOException ex)
    {
      throw new ClassNotFoundException();
    }

    Class c = defineClass("Wrapper", rgb, 0, rgb.length);

    return c;
  }

  /**
   * Loads an agent class.
   *
   */

  private final Class
  loadAgentClass(String strClassName)
    throws ClassNotFoundException
  {
    Class c = null;

    if ((c = (Class)hashtableClasses.get(strClassName)) != null)
    {
      return c;
    }

    String strFileName = transform(strClassName);

    ZipEntry zipentry = zipfile.getEntry(strFileName);

    if (zipentry == null)
    {
      throw new ClassNotFoundException(strClassName);
    }

    byte [] rgb = null;

    try
    {
      int n = (int)zipentry.getSize();

      rgb = new byte [n];

      InputStream inputstream = zipfile.getInputStream(zipentry);

      int m = 0;

      while (m < n)
      {
        m += inputstream.read(rgb, m, n - m);
      }
    }
    catch (IOException ex)
    {
      throw new ClassNotFoundException(strClassName);
    }

    c = defineClass(strClassName, rgb, 0, rgb.length);

    hashtableClasses.put(strClassName, c);

    return c;
  }

  /**
   * Loads a system class.
   *
   */

  private final Class
  loadSystemClass(String strClassName)
  {
    Class c = null;

    try
    {
      c = findSystemClass(strClassName);
    }
    catch (ClassNotFoundException ex)
    {
    }

    return c;
  }

  /**
   * Loads a class.
   *
   * This method handles three cases.  If the class name is the empty
   * string (""), the class loader loads the special wrapper class
   * used for bootstrapping the agent.  If the class name names a
   * system class, the class loader loads the system class.
   * Otherwise, the class loader loads the named agent class from the
   * zipfile.
   *
   */

  protected final synchronized Class
  loadClass(String strClassName, boolean boolResolve)
    throws ClassNotFoundException
  {
    if (!checkPackage(strClassName))
    {
      throw new ClassNotFoundException(strClassName);
    }

    Class c = null;

    if ((c = loadWrapperClass(strClassName)) == null)
    {
      if ((c = loadSystemClass(strClassName)) == null)
      {
        if ((c = loadAgentClass(strClassName)) == null)
        {
          throw new ClassNotFoundException(strClassName);
        }
      }
    }

    if (boolResolve)
    {
      resolveClass(c);
    }

    return c;
  }

  /**
   * Loads a resource.
   *
   */

  public URL
  getResource(String strResource)
  {
    return null;
  }
   
  /**
   * Loads a resource.
   *
   */

  public InputStream
  getResourceAsStream(String strResource)
  {
    ZipEntry zipentry = zipfile.getEntry(strResource);

    try
    {
      return zipentry == null ? null : zipfile.getInputStream(zipentry);
    }
    catch (IOException ex)
    {
      return null;
    }
  }
}
