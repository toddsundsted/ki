
package etcee.ki.agent;

import java.io.Serializable;

/**
 * The message.
 *
 * The <CODE>Message</CODE> class defines a message.
 *
 * This class <EM>is not</EM> thread safe.
 *
 * @see MessageResponse
 *
 */

public final class Message
  implements Serializable
{
  /**
   * The message name.
   *
   */

  private String strMessageName = null;

  /**
   * Gets the message name.
   *
   */

  public final String
  getMessageName()
  {
    return strMessageName;
  }

  /**
   * The parameters.
   *
   * The objects in the parameters array must be serializable.
   *
   */

  private Object [] rgobjParameters = null;

  /**
   * Gets the parameters.
   *
   */

  public final Object []
  getParameters()
  {
    Object [] rgobjParameters = new Object [this.rgobjParameters.length];

    System.arraycopy(this.rgobjParameters,
                     0,
                     rgobjParameters,
                     0,
                     rgobjParameters.length);

    return rgobjParameters;
  }

  /**
   * Constructs the message.
   *
   */

  public
  Message(String strMessageName)
  {
    Object [] rgobjParameters = new Object [0];

    constructMessage(strMessageName, rgobjParameters);
  }

  /**
   * Constructs the message.
   *
   */

  public
  Message(String strMessageName, Object obj)
  {
    Object [] rgobjParameters = new Object [1];

    rgobjParameters[0] = obj;

    constructMessage(strMessageName, rgobjParameters);
  }

  /**
   * Constructs the message.
   *
   */

  public
  Message(String strMessageName, Object obj0, Object obj1)
  {
    Object [] rgobjParameters = new Object [2];

    rgobjParameters[0] = obj0;
    rgobjParameters[1] = obj1;

    constructMessage(strMessageName, rgobjParameters);
  }

  /**
   * Constructs the message.
   *
   */

  public
  Message(String strMessageName, Object obj0, Object obj1, Object obj2)
  {
    Object [] rgobjParameters = new Object [3];

    rgobjParameters[0] = obj0;
    rgobjParameters[1] = obj1;
    rgobjParameters[2] = obj2;

    constructMessage(strMessageName, rgobjParameters);
  }

  /**
   * Constructs the message.
   *
   */ 

  public
  Message(String strMessageName, Object [] rgobjParameters)
  {
    constructMessage(strMessageName, rgobjParameters);
  }

  /**
   * Constructs the message.
   *
   */

  private void
  constructMessage(String strMessageName, Object [] rgobjParameters)
  {
    this.strMessageName = strMessageName;

    this.rgobjParameters = new Object [rgobjParameters.length];

    System.arraycopy(rgobjParameters,
                     0,
                     this.rgobjParameters,
                     0,
                     this.rgobjParameters.length);
  }

  /**
   * Tests two objects for equality.
   *
   * The test for equality is based on the message name.
   *
   */

  public final boolean
  equals(Object obj)
  {
    try
    {
      Message message = (Message)obj;

      if (!strMessageName.equals(message.strMessageName))
      {
        return false;
      }
    }
    catch (ClassCastException ex)
    {
      return false;
    }

    return true;
  }

  /**
   * Returns the hashcode.
   *
   * The hashcode is computed from the message name.
   *
   */

  public final int
  hashCode()
  {
    return strMessageName.hashCode();
  }
}
