
package etcee.ki.server;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * The responsitory entry.
 *
 * The <CODE>RepositoryEntry</CODE> class defines repository entries.
 *
 * A repository entry consists of two pieces: a resource piece and a
 * data piece.  Each piece is an instance of the <CODE>File</CODE>
 * class.
 *
 * Repository entries are only constructed/destroyed by a repository.
 *
 * This class <EM>is not</EM> thread safe.
 *
 * @see Repository
 *
 */

class RepositoryEntry
{
  /**
   * The resource file.
   *
   */

  private File fileResource = null;

  /**
   * The data file.
   *
   */

  private File fileData = null;

  /**
   * Constructs the repository entry.
   *
   * Repository entries are only constructed by a repository.
   *
   * @see Repository.createEntry
   *
   */

  RepositoryEntry(File fileResource, File fileData)
  {
    this.fileResource = fileResource;
    this.fileData = fileData;
  }

  /**
   * Constructs the repository entry.
   *
   * Repository entries are only constructed by the repository.
   *
   * @see Repository.createEntry
   *
   */

  RepositoryEntry(RepositoryEntry repositoryentry)
  {
    this.fileResource = repositoryentry.fileResource;
    this.fileData = repositoryentry.fileData;
  }

  /**
   * Deletes the resource piece of the repository entry.
   *
   */

  void
  deleteResource()
  {
    fileResource.delete();
  }

  /**
   * Deletes the data piece of the repository entry.
   *
   */

  void
  deleteData()
  {
    fileData.delete();
  }

  /**
   * Deletes the repository entry.
   *
   */

  void
  delete()
  {
    deleteResource();
    deleteData();
  }

  /**
   * Invalidates the resource piece of the repository entry.
   *
   */

  void
  invalidateResource()
  {
    fileResource = null;
  }

  /**
   * Invalidates the data piece of the repository entry.
   *
   */

  void
  invalidateData()
  {
    fileData = null;
  }

  /**
   * Invalidates the repository entry.
   *
   */

  void
  invalidate()
  {
    invalidateResource();
    invalidateData();
  }

  /**
   * Gets the resource piece of the repository entry.
   *
   */

  File
  getResource()
  {
    return fileResource;
  }

  /**
   * Gets the data piece of the repository entry.
   *
   */

  File
  getData()
  {
    return fileData;
  }

  /**
   * Sets the resource piece of the repository entry.
   *
   */

  void
  setResource(File fileResource)
  {
    deleteResource();

    this.fileResource = fileResource;
  }

  /**
   * Sets the data piece of the repository entry.
   *
   */

  void
  setData(File fileData)
  {
    deleteData();

    this.fileData = fileData;
  }

  /**
   * Gets the resource piece of the repository entry as an array of
   * bytes.
   *
   */

  byte []
  getResourceAsBytes()
    throws IOException
  {
    if (!fileResource.exists())
    {
      return null;
    }

    return readBytes(fileResource);
  }

  /**
   * Gets the data piece of the repository entry as an array of
   * bytes.
   *
   */

  byte []
  getDataAsBytes()
    throws IOException
  {
    if (!fileData.exists())
    {
      return null;
    }

    return readBytes(fileData);
  }

  /**
   * Sets the resource piece of the repository entry as an array of
   * bytes.
   *
   */

  void
  setResourceAsBytes(byte [] rgbResource)
    throws IOException
  {
    deleteResource();

    if (rgbResource == null)
    {
      return;
    }

    writeBytes(fileResource, rgbResource);
  }

  /**
   * Sets the data piece of the repository entry as an array of
   * bytes.
   *
   */

  void
  setDataAsBytes(byte [] rgbData)
    throws IOException
  {
    deleteData();

    if (rgbData == null)
    {
      return;
    }

    writeBytes(fileData, rgbData);
  }

  /**
   * Reads an array of bytes from a file.
   *
   */

  private static byte []
  readBytes(File file)
    throws IOException
  {
    RandomAccessFile randomaccessfile = new RandomAccessFile(file, "r");

    long n = randomaccessfile.length();

    if (n > Integer.MAX_VALUE)
    {
      throw new IOException("file " + file.getName() + ": too large");
    }

    byte [] rgb = new byte [(int)n];

    randomaccessfile.read(rgb);

    randomaccessfile.close();

    return rgb;
  }

  /**
   * Writes an array of bytes to a file.
   *
   */

  private static void
  writeBytes(File file, byte [] rgb)
    throws IOException
  {
    RandomAccessFile randomaccessfile = new RandomAccessFile(file, "rw");

    randomaccessfile.write(rgb);

    randomaccessfile.close();
  }
}
