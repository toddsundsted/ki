//
// Demo tank bean for Jess
// $Id$
//

package jess.examples.pumps;
import java.beans.*;

public class Tank implements Runnable
{
  private int m_level;
  private String m_name;

  public Tank(String name)
  {
    m_level = 500;
    m_name = name;
    new Thread(this).start();
  }

  public int getLevel()
  {
    return m_level;
  }

  public String getName()
  {
    return m_name;
  }

  public boolean isHigh()
  {
    return m_level > 750;
  }

  public boolean isLow()
  {
    return m_level < 250;
  }

  public boolean isIntact()
  {
    return (m_level < 1000 && m_level > 0);
  }

  public void addWater(int amt)
  {
    if (amt != 0)
      {
        String name = "level";
        boolean hi = isHigh();
        boolean lo = isLow();
        boolean intact = isIntact();

        int tmp = m_level;
        m_level += amt;

        // Check if any other properties were affected
        if (hi != isHigh() || lo != isLow() || intact != isIntact())
          name = null;

        pcs.firePropertyChange(name, new Integer(tmp),
                               new Integer(m_level));
        System.out.println("Tank " + m_name + " level now " + m_level);
      }
  }

  public void run()
  {
    while (isIntact())
      {
        addWater(-5);
        try { Thread.sleep(300); } catch (InterruptedException ie) { break; }
      }

    if (m_level >= 1000)
      System.out.println("Tank exploded!");
    else if (m_level <= 0)
      System.out.println("Tank ran dry and caught fire!");
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
