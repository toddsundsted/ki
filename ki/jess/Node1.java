/** **********************************************************************
 * Single-input nodes of the pattern network
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

public abstract class Node1 extends Node 
{
  
  final static int TEQ = 1;
  final static int TECT = 2;
  final static int TEV1 = 3;
  final static int TNEV1 = 4;
  final static int TNEQ = 5;
  final static int TELN = 6;
  final static int TMF = 7;
  final static int MTEQ = 20;
  final static int MTNEQ = 21;
  final static int MTELN = 22;
  final static int MTMF = 23;
  final static int NONE = 30;

  int R1, R2, R3, R4;
  Value m_value;

  /**
    Constructor
    */

  Node1(int command, int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(engine);
    m_command = command;
    this.R1 = R1;
    this.R2 = R2;
    this.R3 = R3;
    this.R4 = R4;
    m_value = value;
  }

  /**
    Do the business of this node.
    The input token of a Node1 should only be single-fact tokens.
    In this version, both ADD and REMOVE Tokens are handled the same
    way; I think this is correct. We don't test the token to see if
    there's a fact there, for speed's sake; if we throw an exception,
    well, there's a bug!

    RU.CLEAR means flush two-input ndoe memories; we just pass these along.
    All one-input nodes must call this and just immediately return *false*
    if it returns true!
    */
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    if (token.m_tag == RU.CLEAR)
      {
        passAlong(token);
        return true;
      }
    return false;
  }

  /**
    callNode can call this to print debug info
    */

  void debugPrint(Token token, int callType, ValueVector fact,
                  boolean result) throws ReteException
  {
    
    System.out.print("TEST " + toString() + ";ct=" + callType);
    System.out.println(";id=" + fact.get(RU.ID).factIDValue() + ";tag=" +
                       token.m_tag + ";" +  result);
  }
  
 
  public String toString()
  {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[Node1 command=");
    
    switch (m_command)
      {
      case TEQ:
        sb.append("TEQ;data=");
        sb.append(m_value);
        sb.append(";type=");
        sb.append(R2);
        sb.append(";idx=");
        sb.append(R3); break;
        
      case MTEQ:
        sb.append("MTEQ;data=");
        sb.append(m_value);
        sb.append(";type=");
        sb.append(R2);
        sb.append(";idx=");
        sb.append(R3);
        sb.append(";subidx=");
        sb.append(R4); break;
        
      case TMF:
        sb.append("TMF"); break;
        
      case MTMF:
        sb.append("MTMF"); break;
        
      case TNEQ:
        sb.append("TNEQ;data=");
        sb.append(m_value);
        sb.append(";type=");
        sb.append(R2);
        sb.append(";idx=");
        sb.append(R3); break;
        
      case MTNEQ:
        sb.append("MTNEQ;data=");
        sb.append(m_value);
        sb.append(";type=");
        sb.append(R2);
        sb.append(";idx=");
        sb.append(R3);
        sb.append(";subidx=");
        sb.append(R4); break;
        
      case TECT:
        sb.append("TECT;class=");
        sb.append(RU.getAtom(R1));
        sb.append(";ordr=");
        sb.append(R2);  break;
        
      case TELN:
        sb.append("TELN;length=");
        sb.append(R1);  break;
        
      case MTELN:
        sb.append("MTELN;idx=");
        sb.append(R1);
        sb.append(";length=");
        sb.append(R2);  break;
        
      case TEV1:
        sb.append("TEV1;idx1=");
        sb.append(R1);
        sb.append(";idx2=");
        sb.append(R2); break;
        
      case TNEV1:
        sb.append("TNEV1;idx1=");
        sb.append(R1);
        sb.append(";idx2=");
        sb.append(R2); break;

      case NONE:
        sb.append("NONE (dummy node)"); break;
      }
    
    sb.append(";usecount = ");
    sb.append(m_usecount);
    sb.append("]");
    return sb.toString();
  }
  
  /*
   * Node1 Factory method
   */

  static final Node1 create(int cmd, int R1, int R2, int R3,
                            int R4, Value value, Rete rete)
       throws ReteException
  {
    switch(cmd)
      {
      case TMF:
        return new Node1TMF(R1, R2, R3, R4, value, rete);
      case MTMF:
        return new Node1MTMF(R1, R2, R3, R4, value, rete);
      case TEQ:
        return new Node1TEQ(R1, R2, R3, R4, value, rete);
      case MTEQ:
        return new Node1MTEQ(R1, R2, R3, R4, value, rete);
      case MTNEQ:
        return new Node1MTNEQ(R1, R2, R3, R4, value, rete);
      case TEV1:
        return new Node1TEV1(R1, R2, R3, R4, value, rete);
      case TNEV1:
        return new Node1TNEV1(R1, R2, R3, R4, value, rete);
      case TNEQ:
        return new Node1TNEQ(R1, R2, R3, R4, value, rete);
      case TELN:
        return new Node1TELN(R1, R2, R3, R4, value, rete);
      case MTELN:
        return new Node1MTELN(R1, R2, R3, R4, value, rete);
      case TECT:
        return new Node1TECT(R1, R2, R3, R4, value, rete);
      case NONE:
        return new Node1NONE(R1, R2, R3, R4, value, rete);
      default:
        throw new ReteException("Node1::create",
                                "invalid command code:",
                                String.valueOf(cmd));
      }
  }

  static final Node1 create(int cmd, int R1, int R2, int R3,
                            int R4, Rete rete)
       throws ReteException
  {
    return create(cmd, R1, R2, R3, R4, null, rete);
  }

  static final Node1 create(int cmd, Value value, int R3, int R4, Rete rete)
       throws ReteException
  {
    return create(cmd, 0, 0, R3, R4, value, rete);
  }


}

/**
 * A test that always fails
 */

class Node1NONE extends Node1
{
  
  Node1NONE(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(NONE, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    super.callNode(token, callType);
    return false;
  }
}


/**
 * Test that two slots in the same fact DO NOT have the same type and value.
 * Fails if either type or value differ.
 * Absolute index of 1st slot start in R1, second in R2.
 */

class Node1TNEV1 extends Node1
{
  
  Node1TNEV1(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(TNEV1, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    boolean result = false;
    ValueVector fact = token.fact(0);

    Value v1, v2;
    ValueVector slot;
    // make sure both facts are big enough
    if (fact.size() >= R1 && fact.size() >= R2)
      {
        if (R3 != -1)  // i.e., first variable is in a multislot
          v1 = fact.get(R1).listValue().get(R3);
        else 
          v1 = fact.get(R1);
        
        if (R4 != -1)  // i.e., first variable is in a multislot
          v2 = fact.get(R2).listValue().get(R4);
        else 
          v2 = fact.get(R2);
        
        result = ! (v1.equals(v2));
      }

    if (result)
      // Pass this token on to all successors.
      passAlong(token);
    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Test that a slot value and type are NOT that same as some type and value;
 * test passes if either type or value differs. value in R1, type in R2, 
 * absolute index of slot start in R3
 */

class Node1TNEQ extends Node1 
{  
  Node1TNEQ(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(TNEQ, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    boolean result = false;
    ValueVector fact = token.fact(0);

    if (m_value.type() == RU.FUNCALL)
      {
        if (fact.size() >= R3)
          {
            int markF = m_cache.markFuncall();
            int markV = m_cache.markValue();
            if (Funcall.execute(eval(m_value, token),
                                m_engine.globalContext(),
                                m_cache).equals(Funcall.FALSE()))
              
              result = true;

            m_cache.restoreFuncall(markF);
            m_cache.restoreValue(markV);
          }
      }
    else if (fact.size() >= R3 && ! fact.get(R3).equals(m_value))
      {
        result = true;
      }
    
    if (result)
      passAlong(token);

    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Not a test at all. Split a fact into lots of possible alternate facts,
 * each representing a possible multifield mapping. R1 = absolute index
 */

class Node1TMF extends Node1 
{

  Node1TMF(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(TMF, R1, R2, R3, R4, value, engine);
  }

  boolean callNode(Token old_token, int callType) throws ReteException 
  {
    if (super.callNode(old_token, callType))
      return false;

    int i,j;
    ValueVector fact = old_token.fact(0);
    int fact_size = fact.size();
    int new_fact_size, multi_size;

    for (i=0; i < (fact_size - R1 + 1); i++) 
      {
        new_fact_size = R1 + 1 + i;
        multi_size = fact_size - new_fact_size + 1;

        ValueVector new_fact = new ValueVector(new_fact_size);
        // Start of new and old are the same
        for (j=0; j < R1; j++)
          new_fact.add( fact.get(j));

        // Middle of old moved into special multifield in new
        ValueVector multi
          = new ValueVector(multi_size);
        new_fact.add( new Value(multi, RU.LIST));

        for (j=R1; j < multi_size + R1; j++)
          multi.add( fact.get(j));

        // End of new and old are the same
        for (j=R1 + multi_size; j < new_fact_size - 1 + multi_size; j++)
          new_fact.add( fact.get(j));

        // Handy for debugging
        // System.out.println(new_fact);

        // Now propagate to our successors
        Token token = new Token(old_token.m_tag, new_fact);

        // Pass this token on to all successors.
        passAlong(token);
      }

    return true;
  }
  
}

/**
 * Test that two slots in the same fact have the same type and value.
 * Absolute index of 1st slot start in R1, second in R2.
 */

class Node1TEV1 extends Node1
{
  Node1TEV1(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(TEV1, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    boolean result = false;
    ValueVector fact = token.fact(0);

    Value v1, v2;
    ValueVector slot;
    // make sure both facts are big enough
    if (fact.size() >= R1 && fact.size() >= R2)
      {
        if (R3 != -1)  // i.e., first variable is in a multislot
          v1 = fact.get(R1).listValue().get(R3);
        else 
          v1 = fact.get(R1);
        
        if (R4 != -1)  // i.e., first variable is in a multislot
          v2 = fact.get(R2).listValue().get(R4);
        else 
          v2 = fact.get(R2);
        
        result = (v1.equals(v2));
      }

    if (result)
      passAlong(token);

    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Test slot value and type. value in 'value;', type in R2, 
 * absolute index of slot start in R3; R1 is unused
 */

class Node1TEQ extends Node1 
{
  
  Node1TEQ(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(TEQ, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    boolean result = false;
    ValueVector fact = token.fact(0);

    if (m_value.type() == RU.FUNCALL) 
      {
        if (fact.size() >= R3)
          {
            int markF = m_cache.markFuncall();
            int markV = m_cache.markValue();
            if (!Funcall.execute(eval(m_value, token),
                            m_engine.globalContext(),
                            m_cache).equals(Funcall.FALSE()))
          
              result = true;
            m_cache.restoreFuncall(markF);
            m_cache.restoreValue(markV);
          }
      }
    else if (fact.size() >= R3 && fact.get(R3).equals(m_value)) 
      {
        result = true;
      }
    
    if (result)
      passAlong(token);

    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Test just the fact length; length in R1
 */

class Node1TELN extends Node1 
{
  
  Node1TELN(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(TELN, R1, R2, R3, R4, value, engine);
  }

  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    ValueVector fact = token.fact(0);
    boolean result = (fact.size() == R1);

    if (result)
      passAlong(token);

    // debugPrint(token, callType, fact, result);
    return result;
  }


}

/**
  Test class type; class in R1, orderedness in R2, fact length in R3. 
  Length test is >=.
  */

class Node1TECT extends Node1 
{
  
  Node1TECT(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(TECT, R1, R2, R3, R4, value, engine);
  }

  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    ValueVector fact = token.fact(0);
    boolean result = 
      (fact.get(RU.CLASS).atomValue() == R1
       && fact.get(RU.DESC).descriptorValue() == R2);
       
    if (result)  
      passAlong(token);
    

    // debugPrint(token, callType, fact, result);
    return result;
  }


}

/**
 * Test multislot value and type for inequality. value in R1, type in R2, 
 * absolute index of slot in R3, absolute index of subslot in R4
 */

class Node1MTNEQ extends Node1 
{

  Node1MTNEQ(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(MTNEQ, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    boolean result = false;
    ValueVector fact = token.fact(0);

    if (fact.size() >= R3) 
      {
        Value s;
        if ((s = fact.get(R3)).type() == RU.LIST) 
          {
            ValueVector vv = s.listValue();
            if (vv.size() >= R4) 
              {
                Value subslot = vv.get(R4);
                if (m_value.type() == RU.FUNCALL) 
                  {
                    int markF = m_cache.markFuncall();
                    int markV = m_cache.markValue();
                    if (Funcall.execute(eval(m_value,token),
                                        m_engine.globalContext(),
                                        m_cache).equals(Funcall.FALSE()))
                      result = true;
                    m_cache.restoreFuncall(markF);
                    m_cache.restoreValue(markV);
                  }
                else
                  if ( !subslot.equals(m_value))
                    result = true;
              }
          }
      }
    
    if (result)
      passAlong(token);

    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Not a test at all. Split a fact into lots of possible alternate facts,
 * each representing a possible multifield mapping within a multislot.
 * R1 = absolute index, R2 = subindex.
 */

class Node1MTMF extends Node1 
{

  Node1MTMF(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(MTMF, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token old_token, int callType) throws ReteException
  {
    if (super.callNode(old_token, callType))
      return false;

    ValueVector fact = old_token.fact(0);
    
    int i,j;
    int slot_idx = R1;
    int sub_idx = R2;
    if (fact.get(slot_idx).equals(Funcall.NIL()))
      return false;
    
    ValueVector mslot = fact.get(slot_idx).listValue();
    int slot_size = mslot.size();
    int new_slot_size, multi_size;
    
    for (i=0; i < (slot_size - sub_idx + 1); i++) 
      {
        new_slot_size = sub_idx + 1 + i;
        multi_size = slot_size - new_slot_size + 1;
      
        ValueVector new_slot = new ValueVector(new_slot_size);
        // Start of new and old are the same
        for (j=0; j < sub_idx; j++)
          new_slot.add( mslot.get(j));
      
        // Middle of old moved into special multifield in new
        ValueVector multi = new ValueVector(multi_size);
        new_slot.add( new Value(multi, RU.LIST));
      
        for (j=sub_idx; j < multi_size + sub_idx; j++) 
          multi.add(mslot.get(j));
      
        // End of new and old are the same
        for (j=sub_idx + multi_size; j < new_slot_size - 1 + multi_size; j++)
          new_slot.add( mslot.get(j));
      
        ValueVector new_fact = (ValueVector) fact.clone();
        new_fact.set(new Value(new_slot, RU.LIST), slot_idx);
      
        // Handy for debugging
        // System.out.println(new_fact);
      
        // Pass this token on to all successors.
        Token token = new Token(old_token.m_tag, new_fact);
        passAlong(token);

      }
    return true;
  }
  
}

/**
 * Test multislot value and type. value in R1, type in R2, 
 * absolute index of slot in R3, absolute index of subslot in R4
 */

class Node1MTEQ extends Node1 
{

  Node1MTEQ(int R1, int R2, int R3, int R4, Value value, Rete engine) 
  {
    super(MTEQ, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    boolean result = false;
    ValueVector fact = token.fact(0);

    if (fact.size() >= R3) 
      {
        Value s;
        if ((s = fact.get(R3)).type() == RU.LIST) 
          {
            ValueVector vv = s.listValue();
            if (vv.size() >= R4) 
              {
                Value subslot = vv.get(R4);
                if (m_value.type() == RU.FUNCALL) 
                  {
                    int markF = m_cache.markFuncall();
                    int markV = m_cache.markValue();
                    if (!Funcall.execute(eval(m_value, token), 
                                        m_engine.globalContext(),
                                        m_cache).equals(Funcall.FALSE()))
                      result = true;
                    m_cache.restoreFuncall(markF);
                    m_cache.restoreValue(markV);
                  }
                else
                  if ( subslot.equals(m_value))
                    result = true;
              }
          }
      }
    
    if (result)
      passAlong(token);

    //debugPrint(token, callType, fact, result);
    return result;
  }

}

/**
 * Test multislot length. Absolute index of slot in R1, length in R2.
 */

class Node1MTELN extends Node1 
{
  
  Node1MTELN(int R1, int R2, int R3, int R4, Value value, Rete engine)
  {
    super(MTELN, R1, R2, R3, R4, value, engine);
  }
  
  boolean callNode(Token token, int callType) throws ReteException
  {
    if (super.callNode(token, callType))
      return false;

    ValueVector fact = token.fact(0);
    boolean result = false;

    if (fact.size() >= R1) 
      {
        Value s;
        if ((s = fact.get(R1)).type() == RU.LIST) 
          {
            ValueVector vv = s.listValue();
            if (vv.size() == R2)
              result = true;
          }
      }

    if (result)
      passAlong(token);

    // debugPrint(token, callType, fact, result);
    return result;
  }
}




