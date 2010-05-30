/** **********************************************************************
 * Utilities for Java expert system
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;


public class RU 
{

  /**
    Absolute indexes within the fact and deftemplate arrays
    */

  final public static int CLASS      = 0;
  final public static int DESC       = 1;
  final public static int ID         = 2;
  final public static int FIRST_SLOT = 3;

  /**
    Types of CEs
    */

  final public static int NOT_CE     = 1;
  final public static int AND_CE     = 2;
  final public static int OR_CE     = 3;

  /**
    Relative indexes within a deftemplate's slots

    */

  final public static int DT_SLOT_NAME      = 0;
  final public static int DT_DFLT_DATA      = 1;
  final public static int DT_SLOT_SIZE      = 2;

  /**
    The types of data
    */

  final public static int NONE             =       0;
  final public static int ATOM             = 1 <<  0;
  final public static int STRING           = 1 <<  1;
  final public static int INTEGER          = 1 <<  2;
  final public static int VARIABLE         = 1 <<  3;
  final public static int FACT_ID          = 1 <<  4;
  final public static int FLOAT            = 1 <<  5;
  final public static int FUNCALL          = 1 <<  6;
  final public static int ORDERED_FACT     = 1 <<  7;
  final public static int UNORDERED_FACT   = 1 <<  8;
  final public static int LIST             = 1 <<  9;
  final public static int DESCRIPTOR       = 1 << 10;
  final public static int EXTERNAL_ADDRESS = 1 << 11;
  final public static int INTARRAY         = 1 << 12;
  final public static int MULTIVARIABLE    = 1 << 13;
  final public static int SLOT             = 1 << 14;
  final public static int MULTISLOT        = 1 << 15;

  /**
    The four actions for tokens
    */

  final static int ADD       = 0;
  final static int REMOVE    = 1;
  final static int UPDATE    = 2;
  final static int CLEAR     = 3;

  /**
    Constants specifying that a variable is bound to a fact-index
    or is created during rule execution
    */

  final static int PATTERN = -1;
  final static int LOCAL   = -2;
  final static int GLOBAL  = -3;
  
  /**
    facts bigger than this can cause problems
    */
  final static int MAXFIELDS = 32;

  /**
    The atom Hashtable is shared by all interpreters; this makes good sense.
    */

  private static JessHashtable s_atoms = new JessHashtable();

  /**
    A number used in quickly generating unique symbols.
    */
  
  static int s_gensymIdx = 0;

  /**
    putAtom stores an atom in the hash table, avoiding collisions
    */

  public static synchronized int putAtom(String atom) 
  {
    
    int hci = atom.hashCode();
    String n = null;
    boolean flag = true;
    while (flag) 
      {
        n = s_atoms.get(hci);
        if (n != null && n.equals(atom))
          return hci;
        if (n == null) 
          {
            s_atoms.put(hci, atom);
            return hci;
          }
        ++hci;
      } 
    // NOT REACHED
    return 0;
  }
  
  /**
    grabs an atom from the integer hash code.
    */

  public static String getAtom(int hc) 
  {
    return s_atoms.get(hc);
  }

  public static synchronized int gensym(String prefix) 
  {
    String sym = prefix + s_gensymIdx;
    while (getAtom(sym.hashCode()) != null) 
      {
        ++s_gensymIdx;
        sym = prefix + s_gensymIdx;
      }
    
    return putAtom(sym);
  }

}


