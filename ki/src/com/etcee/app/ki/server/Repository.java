package com.etcee.app.ki.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Properties;
import java.util.Vector;

/**
 * The repository.
 *
 * The <CODE>Repository</CODE> class defines a repository for agents.
 * It is the only class entitled to create instances of class
 * <CODE>RepositoryEntry</CODE>.
 *
 * This class <EM>is</EM> thread safe.
 *
 * @see RepositoryEntry
 *
 */

class Repository
{
  /**
   * The file path.
   *
   */

  private File filePath = null;

  /**
   * The suffix for resource files.
   *
   */

  private String strResourceSuffix = null;

  /**
   * The suffix for data files.
   *
   */

  private String strDataSuffix = null;

  /**
   * The repository entry vector.
   *
   */

  private Vector vectorEntries = null;

  /**
   * Constructs the repository.
   *
   * Constructs the repository and makes the initial repository entries.
   *
   */

  Repository(Properties properties)
    throws FileNotFoundException
  {
    String strPath =
      properties.getProperty("com.etcee.app.ki.server.Repository.path", "db");
    String strResourceSuffix =
      properties.getProperty("com.etcee.app.ki.server.Repository.resource_suffix", ".zip");
    String strDataSuffix =
      properties.getProperty("com.etcee.app.ki.server.Repository.data_suffix", ".dat");

    this.strResourceSuffix = strResourceSuffix;

    this.strDataSuffix = strDataSuffix;

    filePath = new File(strPath);

    if (!filePath.exists())
    {
      throw new FileNotFoundException(strPath);
    }

    if (!filePath.isDirectory())
    {
      throw new IllegalArgumentException(strPath + " is not a directory");
    }

    FilenameFilter filenamefilter = new FilenameFilter()
      {
	private String strResourceSuffix = Repository.this.strResourceSuffix;

	public boolean accept(File file, String strFilename)
	{
	  return strFilename.endsWith(strResourceSuffix);
	}
      };

    String [] rgstrFiles = filePath.list(filenamefilter);

    vectorEntries = new Vector(rgstrFiles.length);

    for (int i = 0; i < rgstrFiles.length; i++)
    {
      File fileResource = new File(filePath, rgstrFiles[i]);

      File fileData = new File(filePath, transformName(rgstrFiles[i]));

      vectorEntries.addElement(new RepositoryEntry(fileResource, fileData));
    }
  }

  /**
   * Transforms a filename.
   *
   * Transforms the supplied filename from the resource form to the
   * data form, or from the data form to the resource form.
   *
   * <CODE>foo.zip</CODE> -> <CODE>foo.data</CODE>
   * <CODE>foo.data</CODE> -> <CODE>foo.zip</CODE>
   *
   */

  private String transformName(String strName)
  {
    if (strName.endsWith(strResourceSuffix))
    {
      return strName.substring(0, strName.length() - 4) + strDataSuffix;
    }
    else if (strName.endsWith(strDataSuffix))
    {
      return strName.substring(0, strName.length() - 5) + strResourceSuffix;
    }
    else
    {
      throw new IllegalArgumentException(strName);
    }
  }

  /**
   * The alphabet.
   *
   */

  private final static String strAlphabet = "abcdefghijklmnopqrstuvwxyz";

  /**
   * Generates a filename.
   *
   * Generates a unique filename base -- an alphabetic string ten
   * characters long.
   *
   */

  private String generateName()
  {
    String strFilename = null;

    File file = null;

    while (file == null || file.exists())
    {
      char [] rgc = new char [10];

      for (int i = 0; i < rgc.length; i++)
      {
	int n = (int)(Math.random() * (double)strAlphabet.length());

	rgc[i] = strAlphabet.charAt(n);
      }

      strFilename = new String(rgc);

      file = new File(filePath, strFilename);
    }

    return strFilename;
  }

  /**
   * Creates a repository entry.
   *
   */

  RepositoryEntry
  createEntry(byte [] rgbResource,
              byte [] rgbData)
    throws RepositoryException
  {
    File fileResource = null;
    File fileData = null;

    RepositoryEntry repositoryentry = null;

    try
    {
      String strName = generateName();

      fileResource = new File(filePath, strName + strResourceSuffix);
      fileData = new File(filePath, strName + strDataSuffix);

      repositoryentry = new RepositoryEntry(fileResource, fileData);

      repositoryentry.setResourceAsBytes(rgbResource);
      repositoryentry.setDataAsBytes(rgbData);
    }
    catch (IOException ioex)
    {
      throw new RepositoryException("createEntry failed");
    }

    return repositoryentry;
  }

  /**
   * Deletes a repository entry.
   *
   */

  void
  deleteEntry(RepositoryEntry repositoryentry)
    throws RepositoryException
  {
    repositoryentry.delete();
    repositoryentry.invalidate();
  }

  /**
   * Relinquishes all repository entries.
   *
   * Relinquishes all repository entries.  The calling method assumes
   * ownership of the entries.  It must return any entries it doesn't
   * want.
   *
   * @see assumeEntry
   *
   */

  synchronized Vector
  relinquishEntries()
  {
    Vector vector = vectorEntries;

    vectorEntries = new Vector();

    return vector;
  }

  /**
   * Assumes ownership of a repository entry.
   *
   * Assumes ownership of a repository entry.
   *
   * @see relinquishEntries
   *
   */

  synchronized void
  assumeEntry(RepositoryEntry repositoryentry)
  {
    vectorEntries.addElement(new RepositoryEntry(repositoryentry));

    repositoryentry.invalidate();
  }
}
