/** **********************************************************************
 * A table of Strings hashed by ints
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.io.*;

/**
 * Hashtable collision list.
 */
class JessHashtableEntry
{
  int m_hash;
  int m_key;
  String m_value;
  JessHashtableEntry m_next;
}

class JessHashtable
{
  /**
   * The hash table data.
     */
  private JessHashtableEntry m_table[];

  /**
     * The total number of entries in the hash table.
     */
  private int m_count;

  /**
     * Rehashes the table when count exceeds this threshold.
     */
  private int m_threshold;

  /**
     * The load factor for the hashtable.
     */
  private float m_loadFactor = 0.75f;

  JessHashtable() 
  {
    int size = 101;
    m_table = new JessHashtableEntry[size];
    m_threshold = (int)(size * m_loadFactor) ;
  }

  synchronized String get(int key) 
  {
    JessHashtableEntry tab[] = m_table;
    int hash = key;
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (JessHashtableEntry e = tab[index] ; e != null ; e = e.m_next) 
      {
        if ((e.m_hash == hash) && e.m_key == key) 
          {
            return e.m_value;
          }
      }
    return null;
  }

  private void rehash() 
  {
    int oldCapacity = m_table.length;
    JessHashtableEntry oldTable[] = m_table;

    int newCapacity = oldCapacity * 2 + 1;
    JessHashtableEntry newTable[] = new JessHashtableEntry[newCapacity];

    m_threshold = (int)(newCapacity * m_loadFactor);
    m_table = newTable;

    for (int i = oldCapacity ; i-- > 0 ;) 
      {
        for (JessHashtableEntry old = oldTable[i] ; old != null ; ) 
          {
            JessHashtableEntry e = old;
            old = old.m_next;

            int index = (e.m_hash & 0x7FFFFFFF) % newCapacity;
            e.m_next = newTable[index];
            newTable[index] = e;
          }
      }
  }

  synchronized String put(int key, String value) 
  {
    // Make sure the value is not null
    if (value == null) 
      {
        throw new NullPointerException();
      }

    // Makes sure the key is not already in the hashtable.
    JessHashtableEntry tab[] = m_table;
    int hash = key;
    int index = (hash & 0x7FFFFFFF) % tab.length;
    for (JessHashtableEntry e = tab[index] ; e != null ; e = e.m_next) 
      {
        if ((e.m_hash == hash) && e.m_key == key) 
          {
            String old = e.m_value;
            e.m_value = value;
            return old;
          }
      }

    if (m_count >= m_threshold) 
      {
        // Rehash the table if the threshold is exceeded
        rehash();
        return put(key, value);
      } 

    // Creates the new entry.
    JessHashtableEntry e = new JessHashtableEntry();
    e.m_hash = hash;
    e.m_key = key;
    e.m_value = value;
    e.m_next = tab[index];
    tab[index] = e;
    m_count++;
    return null;
  }

}


