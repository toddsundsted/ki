package com.etcee.app.ki.server;

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
final
class AgentClassLoader
extends ClassLoader
{
  /**
   * The zipfile.
   *
   * The class loader loads agent classes from the zipfile.
   *
   */
  private
  ZipFile m_zipfile = null;

  /**
   * Constructs the agent class loader.
   *
   */
  AgentClassLoader(File file)
  throws IOException
  {
    m_zipfile = new ZipFile(file);
  }

  /**
   * Disposes of the agent class loader.
   *
   */
  final
  void
  dispose()
  throws IOException
  {
    try
    {
      if (m_zipfile != null) m_zipfile.close();
    }
    finally
    {
      m_zipfile = null;
    }
  }

  /**
   * Finds a class given a class name.
   *
   */
  protected
  final
  Class
  findClass(String stringClassName)
  throws ClassNotFoundException
  {
    Class c = null;
    if ((c = findWrapperClass(stringClassName)) != null) return c;
    if ((c = findAgentClass(stringClassName)) != null) return c;
    throw new ClassNotFoundException(stringClassName);
  }

  /**
   * Finds the wrapper class.
   *
   */
  private
  final
  Class
  findWrapperClass(String stringClassName)
  {
    if (!stringClassName.equals(""))
      return null;
    File file = new File("lib" + File.separatorChar + "Wrapper.class");
    if (!file.exists()) return null;
    byte [] rgb = null;
    try
    {
      FileInputStream fileinputstream = new FileInputStream(file);
      int n = (int)file.length();
      rgb = new byte [n];
      int m = 0;
      while (m < n) m += fileinputstream.read(rgb, m, n - m);
      fileinputstream.close();
    }
    catch (IOException ioexception)
    {
      ioexception.printStackTrace();
      return null;
    }
    return defineClass("Wrapper", rgb, 0, rgb.length);
  }

  /**
   * Finds an agent class.
   *
   */
  private
  final
  Class
  findAgentClass(String stringClassName)
  {
    String stringFileName = stringClassName.replace('.', File.separatorChar) + ".class";
    ZipEntry zipentry = m_zipfile.getEntry(stringFileName);
    if (zipentry == null) return null;
    byte [] rgb = null;
    try
    {
      InputStream inputstream = m_zipfile.getInputStream(zipentry);
      int n = (int)zipentry.getSize();
      rgb = new byte [n];
      int m = 0;
      while (m < n) m += inputstream.read(rgb, m, n - m);
      inputstream.close();
    }
    catch (IOException ioexception)
    {
      ioexception.printStackTrace();
      return null;
    }
    return defineClass(stringClassName, rgb, 0, rgb.length);
  }
}
