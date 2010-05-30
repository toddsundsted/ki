//
// Demo simple bean for Jess
// $Id$
//

package jess.examples.simple;
import java.beans.*;

public class Simple
{
  private int m_serial;
  private float m_fraction;
  private boolean m_truth;
  private String m_name;

  public Simple(String name)
  {
    m_name = name;
    m_serial = 0;
    m_fraction = 0;
    m_truth = false;
  }

  public String getName()
  {
    return m_name;
  }

  public void setName(String s)
  {
    String tmp = m_name;
    m_name = s;
    pcs.firePropertyChange("name", tmp,
                           m_name);
  }

  public int getSerial()
  {
    return m_serial;
  }

  public void setSerial(int serial)
  {
    int tmp = m_serial;
    m_serial = serial;
    
    pcs.firePropertyChange("serial", new Integer(tmp),
                           new Integer(m_serial));
  }

  public float getFraction()
  {
    return m_fraction;
  }

  public void setFraction(float fraction)
  {
    float tmp = m_fraction;
    m_fraction = fraction;
    
    pcs.firePropertyChange("fraction", new Float(tmp),
                           new Float(m_fraction));
  }

  public boolean getTruth()
  {
    return m_truth;
  }

  public void setTruth(boolean truth)
  {
    boolean tmp = m_truth;
    m_truth = truth;
    
    pcs.firePropertyChange("truth", new Boolean(tmp),
                           new Boolean(m_truth));
  }

  private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  public void addPropertyChangeListener(PropertyChangeListener pcl)
  {
    pcs.addPropertyChangeListener(pcl);
  }
  public void removePropertyChangeListener(PropertyChangeListener pcl)
  {
    pcs.removePropertyChangeListener(pcl);
  }

}
