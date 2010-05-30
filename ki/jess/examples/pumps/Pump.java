//
// Demo pump bean for Jess
// $Id$
//

package jess.examples.pumps;
import java.beans.*;

public class Pump implements Runnable
{
  private int m_flowRate;
  private Tank m_tank;
  private String m_name;

  public Pump(String name, Tank tank)
  {
    m_tank = tank;
    m_name = name;
    m_flowRate = 0;
    new Thread(this).start();
  }

  public String getName()
  {
    return m_name;
  }

  public int getFlow()
  {
    return m_flowRate;
  }

  public void setFlow(int flowRate)
  {
    if (flowRate >= 0 && flowRate != m_flowRate)
      {
        int tmp = m_flowRate;
        m_flowRate = flowRate;

        pcs.firePropertyChange("flow", new Integer(tmp),
                               new Integer(flowRate));
      }
  }

  public void run()
  {
    while (m_tank.isIntact())
      {
        m_tank.addWater(m_flowRate);
        try { Thread.sleep(1000); } catch (InterruptedException ie) { return; }
      }
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
