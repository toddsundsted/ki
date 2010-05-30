/** **********************************************************************
 * Generates a pattern network
 *
 * See the paper
 * "Rete: A Fast Algorithm for the Many Pattern/ Many Object Pattern
 * Match Problem", Charles L.Forgy, Artificial Intelligence 19 (1982), 17-37.
 *
 * The implementation used here does not follow this paper; the present
 * implementation models the Rete net more literally as a set of networked Node
 * objects with interconnections.
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.util.*;

class ReteCompiler 
{

  final private int VARIABLE_TYPES = RU.VARIABLE | RU.MULTIVARIABLE;
  private Rete m_engine;

  /**
    The roots of the pattern network
    */

  private Vector m_roots = new Vector();
  public final Vector roots() { return m_roots; }

  /**
    Have any rules been added?
    */

  private boolean m_dirty = false;

  /**
    Constructor
    */

  public ReteCompiler(Rete r) 
  {
    m_engine = r;
  }

  /**
    Evaluate a funcall, replacing all variable references with the int[]
    mini-bindings used by the nodes for predicate funcall evaluation.
    Bind each variable to the first occurrence in rule
    */

  private Value eval(int[][] table, Value v, Defrule dr)
       throws ReteException 
  {
    ValueVector vv = (ValueVector) v.funcallValue().clone();
    for (int i=0; i<vv.size(); i++) 
      {
        if ((vv.get(i).type() &  VARIABLE_TYPES) != 0) 
          {
            int[] binding = new int[3];
            int name = vv.get(i).variableValue();
            binding[0] = -1;
            for (int j=0; j<table[0].length; j++)
              if (table[0][j] == name) 
                {
                  binding[0] = table[1][j];
                  binding[1] = table[2][j];
                  binding[2] = table[3][j];
                  break;
                }
            if (binding[0] == -1)
              {
                // Not a regular matched variable. Must be either a 
                // defglobal, a pattern index, or a typo.

                // typo
                Binding b = dr.findBinding(name);
                if (b == null)
                  throw new ReteException("ReteCompiler:eval()",
                                          "Unbound variable found in funcall:",
                                          vv.get(i).stringValue());

                // pattern index (?x <- (patt))
                else if (b.m_factIndex >= 0)
                  {
                    // This is clever. Translate this into a binding like the
                    // above. We can do this because the FACT-ID of a fact
                    // is stored inside the fact itself in a known location.
                    binding[0] = b.m_factIndex;
                    binding[1] = RU.ID;
                    binding[2] = -1;
                    vv.set(new Value(binding, RU.INTARRAY), i);
                  }

                // Global variable reference
                else if (b.m_factIndex == RU.LOCAL)
                  {
                    Funcall f = new Funcall("get-var", m_engine);
                    f.add(vv.get(i));
                    vv.set(new Value(f, RU.FUNCALL), i);
                  }
              }
            else
              // A good old-fashioned matched variable.
              vv.set(new Value(binding, RU.INTARRAY), i);

          }
        else if (vv.get(i).type() ==  RU.FUNCALL) 
          {
            // nested funcalls
            vv.set(eval(table, vv.get(i), dr), i);
          }
      }
    return new Value(vv, RU.FUNCALL);
  }

  /**
    Call this on a funcall value AFTER eval has modified it.
    it returns true iff this funcall contains binding to patterns other
    than the one named by index.
    */
  private boolean checkForMultiPattern(Value v, int index)
       throws ReteException 
  {

    ValueVector vv = v.funcallValue();
    for (int i=0; i<vv.size(); i++) 
      {
        if (vv.get(i).type() ==  RU.INTARRAY &&
            vv.get(i).intArrayValue()[0] != index) 
          {
            return true;
          }
        
        else if (vv.get(i).type() ==  RU.FUNCALL &&
                 checkForMultiPattern(vv.get(i), index)) 
          {
            return true;
          }
      }
    return false;

  }

  void freeze() 
  {
    if (m_dirty) 
      {
        for (int i=0; i< m_roots.size(); i++) 
          {
            ((Successor) m_roots.elementAt(i)).m_node.freeze();
          }
      }
    m_dirty = false;
  }

  /**
    Add a rule's patterns to the network
    */

  public void addRule(Defrule r) throws ReteException 
  {
    if (m_engine.watchCompilations())
      m_engine.outStream().print(RU.getAtom(r.m_name) + ": ");

    // Tell the rule to fix up its binding table; it must be complete now.
    r.freeze();

    doAddRule(r);
  }

  private void doAddRule(Defrule r) throws ReteException
  {

    // First construct a set of three parallel int[]s, each of which
    // is n_variables long, which contain: a variable name (atom),
    // the number of the first pattern in this rule containing that
    // variable, and the index within that pattern where the variable
    // first appears. If the first occurrence of a variable is negated,
    // throw an exception - this is a syntax error.
    // this table is used to replace variable names in function calls and
    // also in pass three to produce two-input nodes.

    int[][] table = makeVarTable(r);


    // 'terminals' will be where we hold onto the final links in the
    // chain of nodes built during the first pass for each pattern
    Successor[] terminals = new Successor[r.m_patts.size()];
    Node[] ruleRoots = new Node[r.m_patts.size()];
    
    /* *********
       FIRST PASS
       ********* */

    // In the first pass, we just create some of the one-input
    // nodes for each pattern.
    // These one-input nodes compare a certain slot in a fact (token) with a
    // fixed, typed, piece of data. Each pattern gets one special
    // one-input node, the TECT node, which checks the class type
    // and class name.


    // iterate over each of the rule's patterns
    for (int i=0; i<r.m_patts.size(); i++) 
      {

        // get this pattern

        Pattern p = (Pattern) r.m_patts.elementAt(i);

        // If this is a 'test' CE, we have to treat it slightly differently
        boolean isTest = (p.name() == RU.putAtom("test"));

          
        ////////////////////////////////////////////////////////////
        // Every pattern must have a definite class name
        // Therefore, the first node in a chain always
        // checks the class name and the orderedness of a token
        // (except for test CEs)
        ////////////////////////////////////////////////////////////

        int length = RU.FIRST_SLOT + p.m_tests.length;
        Successor last = createSuccessor(m_roots,
                                         isTest ? Node1.NONE : Node1.TECT,
                                         p.name(), p.ordered(),
                                         0, r);
        ruleRoots[i] = last.m_node;

        // First we have to find all the multifields, because these change the
        // 'shape' of the facts as they go through the net. 

        for (int j=0; !isTest && j<p.m_tests.length; j++) 
          {

            // any tests on this slot?
            if (p.m_tests[j] == null)
              continue;

            int testIdx = RU.FIRST_SLOT + j;
        
            for (int k=0; k<p.m_tests[j].length; k++) 
              {
                Test1 test = p.m_tests[j][k];

                if (test.m_slotValue.type() == RU.MULTIVARIABLE) 
                  {
                    if (test.m_subIdx == -1) 
                      {

                        // Split this fact into many possible multifield facts
                        last = createSuccessor(last.m_node.succ(),
                                               Node1.TMF, testIdx, 0, 0, r);
                      }
                    else 
                      {
                        // Split this multislot into many multislot facts
                        last = createSuccessor(last.m_node.succ(),
                                               Node1.MTMF, testIdx,
                                               test.m_subIdx, 0, r);
                      }
                  } 
          
              }

          }


        ////////////////////////////////////////////////////////////
        // Good time to check size, now that it's deinitely set.
        ////////////////////////////////////////////////////////////

        if (!isTest)
          last = createSuccessor(last.m_node.succ(), Node1.TELN,
                                 length, 0, 0, r);
      
        // Test multislot sizes too...
        for (int j=0; !isTest && j<p.m_tests.length; j++) 
          {
        
            // if multislot, size is determinate now (splitting done above)
            if (p.m_slotLengths != null && p.m_slotLengths[j] != -1) 
              last = createSuccessor(last.m_node.succ(), Node1.MTELN,
                                     RU.FIRST_SLOT + j, p.m_slotLengths[j],
                                     0, r);

          }

        ////////////////////////////////////////////////////////////
        // Simplest basic tests are done here
        ////////////////////////////////////////////////////////////

        for (int j=0; !isTest && j<p.m_tests.length; j++) 
          {

            // any tests on this slot?
            if (p.m_tests[j] == null)
              continue;

            int testIdx = RU.FIRST_SLOT + j;
        
            for (int k=0; k<p.m_tests[j].length; k++) 
              {
                Test1 test = p.m_tests[j][k];
                int slot_test = (test.m_subIdx == -1 ? Node1.TEQ : Node1.MTEQ);
                if (test.m_test == Test1.NEQ)
                  slot_test = (test.m_subIdx == -1 ? Node1.TNEQ : Node1.MTNEQ);

                if (test.m_slotValue.type() == RU.VARIABLE) 
                  {

                    // don't make nodes for variables during this pass

                  }
                else if (test.m_slotValue.type() == RU.MULTIVARIABLE) 
                  {

                    // Just did these!

                  }
                else if (test.m_slotValue.type() == RU.FUNCALL) 
                  {

                    // expand the variable references to index, slot, subslot  triples

                    Value v = eval(table, test.m_slotValue, r);
                    // if only this fact is mentioned, generate a test
                    if (!checkForMultiPattern(v, i)) 
                      {
                        last = createSuccessor(last.m_node.succ(),
                                               slot_test, v,
                                               testIdx, test.m_subIdx, r);
                      }


                  } 
                else // not a FUNCALL
                  last = createSuccessor(last.m_node.succ(),
                                         slot_test, test.m_slotValue,
                                         testIdx, test.m_subIdx, r);

              }
          }
        terminals[i] = last;
      }

    /* *********
       SECOND PASS
       ********* */

    // In this pass, we are looking for variables which must be
    // instantiated the same way twice in one fact. IE, the pattern look like
    // (foo ?X foo ?X), and we're looking for facts like (foo bar foo bar).
    // NOT versions are handled as well.

    // iterate over each of the rule's patterns
    for (int i=0; i<r.m_patts.size(); i++) 
      {
          
        // get this pattern
        Pattern p = (Pattern) r.m_patts.elementAt(i);

        // If this is a 'test' CE, we have to treat it slightly differently
        if (p.name() == RU.putAtom("test"))
          continue;

        // workspace to track variables that have been done.
        int[] done_vars = new int[256];
        int nvars = 0;

        // find a variable slot, if there is one. If one is found,
        // look at the rest of
        // the fact for another one with the same name.
        // If one is found, create the
        // appropriate node and put it in place.

        // NOTs make things a bit more complex.
        // There are a few cases for a varname
        // appearing twice in a pattern:

        // ?X ?X        ->        generate a TEV1 node.
        // ?X ~?X       ->        generate a TNEV1 node.
        // ~?X ?X       ->        generate a TNEV1 node.
        // ~?X ~?X      ->        (DO NOTHING!)


        // look for a slot in the pattern containing a variable

        for (int j= 0; j < p.m_tests.length; j++) 
          {
            // any tests for this slot?
            if (p.m_tests[j] == null)
              continue;
          k_loop:
            for (int k= 0; k < p.m_tests[j].length; k++) 
              {
                if ((p.m_tests[j][k].m_slotValue.type() & VARIABLE_TYPES) == 0 ) 
                  continue;
                
                // see if we've done this one before.
                for (int m=0; m<nvars; m++) 
                  {
                    if (p.m_tests[j][k].m_slotValue.variableValue() == done_vars[m])
                      continue k_loop;
                  }
                
                // no, we haven't. Find each other occurrence.
                
                for (int n=j + 1; n < p.m_tests.length; n++) 
                  {
                    if (p.m_tests[n] == null)
                      continue;
                    for (int o= 0; o < p.m_tests[n].length; o++) 
                      {
                        if ((p.m_tests[n][o].m_slotValue.type() & VARIABLE_TYPES) != 0 &&
                            p.m_tests[n][o].m_slotValue.variableValue() ==
                            p.m_tests[j][k].m_slotValue.variableValue()) 
                          {
                            // we've identified another slot with the same variable.
                            // Do what's described in the table above.
                            int slot1 = j + RU.FIRST_SLOT;
                            int slot2 = n + RU.FIRST_SLOT;
                            if (p.m_tests[j][k].m_test == Test1.EQ) 
                              {
                                if (p.m_tests[n][o].m_test == Test1.EQ)
                                  terminals[i]
                                    = createSuccessor(terminals[i].m_node.succ(),
                                                      Node1.TEV1, slot1,
                                                      slot2,
                                                      p.m_tests[j][k].m_subIdx,
                                                      p.m_tests[n][o].m_subIdx,
                                                      r);
                                else
                                  terminals[i]
                                    = createSuccessor(terminals[i].m_node.succ(),
                                                      Node1.TNEV1, slot1,
                                                      slot2,
                                                      p.m_tests[j][k].m_subIdx,
                                                      p.m_tests[n][o].m_subIdx,
                                                      r);
                                
                              }
                            else 
                              {
                                if (p.m_tests[n][o].m_test == Test1.EQ)
                                  terminals[i]
                                    = createSuccessor(terminals[i].m_node.succ(),
                                                      Node1.TNEV1, slot1,
                                                      slot2,
                                                      p.m_tests[j][k].m_subIdx,
                                                      p.m_tests[n][o].m_subIdx,
                                                      r);
                                else
                                  ;
                              }
                          }
                      }
                  }
                done_vars[nvars++] = p.m_tests[j][k].m_slotValue.variableValue();
              }
          }
        
      } // end of second pass
    
    /* *********
       THIRD PASS
       ********* */

    // Now we start making some two-input nodes. These nodes check that
    // a variable with the same name in two patterns is instantiated the
    // same way in each of two facts; or not, in the case of negated
    // variables. An important thing to remember: the first instance of a
    // variable can never be negated. We'll check that here and throw
    // an exception if it's violated.  We can compare every other instance
    // of the variable in this rule against this first one - this simplifies
    // things a lot! We'll use simplified logic which will lead to a few
    // redundant, but correct tests.

    // Two-input nodes can contain many tests, so they are rather more
    // complex than the one-input nodes. To share them, what we'll do is build
    // a new node, then compare this new one to all possible shared ones.
    // If we can share, we just throw the new one out. The inefficiency is
    // gained back in spades at runtime, both in memory and speed. Note that
    // NodeNot2 type nodes cannot be shared.

    /*
      The number of two-input nodes that we create is *determinate*: it is
      always one less than the number of patterns. For example, w/ 4 patterns,
      numbered 0,1,2,3, and the following varTable:
      (Assuming RU.FIRST_SLOT == 2 and RU.SLOT_SIZE = 2)

      <PRE>

      X  Y  N
      0  1  2
      2  4  4

      </PRE>
      generated from the following rule LHS:

      <PRE>
      (foo ?X ?X)
      (bar ?X ?Y)
      (Goal (Type Simplify) (Object ?N))
      (Expression (Name ?N) (Arg1 0) (Op +) (Arg2 ~?X))
      </PRE>

      Would result in the following nodes being generated
      (Assuming SLOT_DATA == 0, SLOT_TYPE == 1, SLOT_SIZE == 2):

      <PRE>
        0     1
         \   /
      ___L___R____
      |          |            2
      | 0,2 = 2? |           /
      |          |          /
      ------------ \0,1    /
                   _L______R__                3
                  |          |               /
                  | NO TEST  |              /
                  |          |             /
                  ------------ \0,1,2     /
                                L_______R__
                                | 0,2 != 8?|
                                | 2,4 = 2? |
                                |          |
                                ------------
                                     |0,1,2,3
                                     |
                                (ACTIVATE)

      <PRE>

      Where the notation 2,4 = 8? means that this node tests tbat index 4 of
      fact 2 in the left token is equal to index 8 in the right
      token's single fact. L and R indicate Left and Right inputs.
      */

    // for each pattern, starting with the second one

    for (int i=1; i < (r.m_patts.size()); i++) 
      {

        // get this pattern
        Pattern p = (Pattern) r.m_patts.elementAt(i);

        // If this is a 'test' CE, we have to treat it slightly differently
        boolean isTest = (p.name() == RU.putAtom("test"));

        // construct an appropriate 2 input node...
        NodeTest n2;
        
        if (isTest)
          n2 = new NodeTest(m_engine);
        else if (p.negated() != 0)
          n2 = new NodeNot2(m_engine);
        else
          n2 = new Node2(m_engine);
        // now tell the node what tests to perform

        // for each field in this pattern
        for (int j=0; j< p.m_tests.length; j++) 
          {

            // any tests for this slot?
            if (p.m_tests[j] == null)
              continue;

            // for every test on this slot..
            for (int k=0; k< p.m_tests[j].length; k++) 
              {

                // if this test is against a variable...
                if ((p.m_tests[j][k].m_slotValue.type() & VARIABLE_TYPES) != 0) 
                  {
            
                    // find this variable in the table
                    int n = 0;
                    while (table[0][n] != p.m_tests[j][k].m_slotValue.variableValue()) n++;
            
                    // table[1][n] can't be greater than i
                    if (table[1][n] > i)
                      compilerError("AddRule", "Corrupted VarTable: table[1][n] > i");

                    // if this is the first appearance, no test.
                    else if (table[1][n] == i)
                      continue;

                    if (p.m_tests[j][k].m_test == Test1.EQ)
                      n2.addTest(Test2.EQ,
                                 table[1][n],
                                 table[2][n],
                                 table[3][n],
                                 RU.FIRST_SLOT + j,
                                 p.m_tests[j][k].m_subIdx);
                    else
                      n2.addTest(Test2.NEQ,
                                 table[1][n],
                                 table[2][n],
                                 table[3][n],
                                 RU.FIRST_SLOT + j,
                                 p.m_tests[j][k].m_subIdx);

            
                    // if this test is a function call
                  }
                else if (p.m_tests[j][k].m_slotValue.type() == RU.FUNCALL) 
                  {

                    // expand the variable references to index, slot pairs
                    // we do this again even though we did it in pass one
                    // we don't want to destroy the patterns themselves
                    // Tell Eval to bind variables to first occurrence in Rule.
                    Value v = eval(table, p.m_tests[j][k].m_slotValue, r);

                    // if other facts besides this one are mentioned, generate a test

                    if (isTest || checkForMultiPattern(v, i)) 
                      {
                        if (p.m_tests[j][k].m_test == Test1.EQ)
                          n2.addTest(Test1.EQ, RU.FIRST_SLOT + j,
                                     p.m_tests[j][k].m_subIdx, v);
                        else
                          n2.addTest(Test1.NEQ, RU.FIRST_SLOT + j,
                                     p.m_tests[j][k].m_subIdx, v);
                      }
                  }
              }
          }
      
        // search through the successors of this pattern and the next one.
        // Do they have any in common, and if so, are they equivalent to the
        // one we just built? If so, we don't need to add the new one!
      
 
        boolean new_node = true;
        Successor s1 = null, s2 = null;
        if (p.negated() == 0) 
          {
          j_loop:
            for (int j=0; j<terminals[i-1].m_node.succ().size(); j++) 
              {
                Node jnode = ((Successor)terminals[i-1].m_node.succ().elementAt(j)).m_node;
                if (!(jnode instanceof Node2) || (jnode instanceof NodeNot2))
                  continue;
                for (int k=0; k<terminals[i].m_node.succ().size(); k++) 
                  {
                    if (jnode==((Successor)terminals[i].m_node.succ().elementAt(k)).m_node) 
                      {
                        s1 = (Successor) terminals[i-1].m_node.succ().elementAt(j);
                        s2 = (Successor) terminals[i].m_node.succ().elementAt(k);
                        if (s1.m_callType == Node.LEFT &&
                            s2.m_callType == Node.RIGHT &&
                            ((Node2)jnode).equals(n2)) 
                          {
                            new_node = false;
                            ++jnode.m_usecount;
                            r.addNode(s1);
                            break j_loop;
                          }
                      }
                  }
              }
          }
      
        if (new_node) 
          {
            // attach it to the tails of the node chains from
            // this pattern and the next
        
            s1 = new Successor(n2, Node.LEFT);
            terminals[i-1].m_node.succ().addElement(s1);
       
            s2 = new Successor(n2, Node.RIGHT);
            terminals[i].m_node.succ().addElement(s2);
       
            // OK, it's done; speed it up!
            n2.complete();
            if (m_engine.watchCompilations())
              m_engine.outStream().print("+2"); 

            r.addNode(s1);
          }
        else 
          if (m_engine.watchCompilations())
            m_engine.outStream().print("=2");
      
        /* Multiple not CEs are handled here.
           This is really very simple: we insert single (left) input
           NodeNot2's for the second and successive not CEs.

           It's still not right, but I think it's the germ of an idea.
           */
        /*

        for (int nn = 1; nn < p.negated(); nn++)
          {
            NodeNot2 negnode = new NodeNot2(m_engine);
            s1 = new Successor(negnode, Node.LEFT);
            terminals[i-1].m_node.succ().addElement(s1);
            terminals[i-1] = s1;
            s2 = new Successor(negnode, Node.RIGHT);
            n2.succ().addElement(s2);
            terminals[i] = s2;
            negnode.complete();
            n2 = negnode;

            if (m_engine.watchCompilations())
              m_engine.outStream().print("+2");
          }

          */
        
        // Advance the tails
        terminals[i-1] = s1;
        terminals[i] = s2;

      }
    
    /* ************
       FOURTH PASS
       ************ */

    // All that's left to do is to create the terminal node.
    // This is very easy.

    NodeTerm nt = new NodeTerm(r, m_engine);
    Successor s = new Successor(nt, Node.ACTIVATE);
    terminals[r.m_patts.size() - 1].m_node.succ().addElement(s);
    r.addNode(s);

    if (m_engine.watchCompilations())
      m_engine.outStream().println("+t");

    // we need freezing.
    m_dirty = true;

    //Tell the engine to update this rule if the agenda isn't empty
    m_engine.updateNodes(ruleRoots);

  }

  /**

    Construct a table (patterns x names) listing the first index at which
    each var appears in each pattern; -1 marks entries in
    which the var does not appear in the pattern.

    */

  private int[][] makeVarTable(Defrule r) throws ReteException 
  {

    // First we need a list of all the unique variable names
    // in this rule. We take the brute force approach. Note: we assume
    // that no more than 256 variable references can occur on the LHS of a rule.

    int[] names =new int[256];
    int num_names = 0;

    // new the 2-D table
    int[][] scrTable = new int[4][256];

    // iterate over each of the rule's patterns
    for (int patt=0; patt<r.m_patts.size(); patt++) 
      {

        // get this pattern
        Pattern p = (Pattern) r.m_patts.elementAt(patt);
        // for each slot in this pattern
        for (int slot= 0; slot < p.m_tests.length; slot++) 
          {
            // are there any tests for this slot?
            if (p.m_tests[slot] == null)
              continue;
            // for each test in this slot
            for (int test= 0; test < p.m_tests[slot].length; test++) 
              {
                // if this test is against a variable,
                if ((p.m_tests[slot][test].m_slotValue.type() & VARIABLE_TYPES) != 0) 
                  {

                    // store it!

                    // If we haven't seen this one before
                    // checkfor illegal initial negation

                    if (-1 ==
                        findIntInArray(scrTable[0], num_names,
                                       p.m_tests[slot][test].m_slotValue.variableValue()))
                      if (p.m_tests[slot][test].m_test == Test1.NEQ)
                        compilerError("makeVarTable","Variable " +
                                       RU.getAtom(p.m_tests[slot][test]
                                                  .m_slotValue.variableValue()) +
                                       " is used before definition, Rule " +
                                       RU.getAtom(r.m_name));

                    if (p.m_tests[slot][test].m_test != Test1.NEQ) 
                      {
                        scrTable[0][num_names] =
                          p.m_tests[slot][test].m_slotValue.variableValue();
                        scrTable[1][num_names] = patt;
                        scrTable[2][num_names] = RU.FIRST_SLOT + slot;
                        scrTable[3][num_names++] = p.m_tests[slot][test].m_subIdx;
                      }
                  }
              }
          }
      }
    // new a 2-D table just the right size
    int[][] table = new int[4][num_names];
    
    for (int i=0; i<4; i++)
      System.arraycopy(scrTable[i], 0, table[i], 0, num_names);
    
    
    /*
    // debug print the vartable
    System.out.println("VARTBL");
    for (int i=0; i<num_names; i++) 
      {
        System.out.print(RU.getAtom(table[0][i]) + " ");
        for (int j=1; j<4; j++) 
          System.out.print(table[j][i] + " ");
        System.out.println();
      }
      */
    
    
    return table;

  }
  

  /**
    Returns index if i exists in array[size], else -1
    */

  private int findIntInArray(int[] array, int size, int i) 
  {
    int rv = -1;
    for (int j=0; j< size; j++)
      if (array[j] == i) 
        {
          rv = j;
          break;
        }
    return rv;
  }



  /**
    Return an old or new one-input node of a given description
    within the given Vector of nodes.
    */

  Successor createSuccessor(Vector amongst, int cmd,
                            int R1, int R2, int R3, Defrule r)
       throws ReteException
  {
    return createSuccessor(amongst, cmd, R1, R2, R3, -1, r);
  }

  Successor createSuccessor(Vector amongst, int cmd,
                            int R1, int R2, int R3, int R4, Defrule r) 
       throws ReteException
  {
    for (int i=0; i< amongst.size(); i++) 
      {
        Successor test = (Successor) amongst.elementAt(i);
        if (test.m_node instanceof Node1) 
          {
            Node1 node = (Node1) test.m_node;
            if ((node.m_command == cmd) &&
                (node.R1 == R1) &&
                (node.R2 == R2) &&
                (node.R3 == R3) &&
                (node.R4 == R4)) 
              {
                if (m_engine.watchCompilations())
                  m_engine.outStream().print("=1");
                r.addNode(test);
                ++node.m_usecount;
                return test;
              }
          }
      }
    Node1 n = Node1.create(cmd, R1, R2, R3, R4, m_engine);
    Successor rv = new Successor(n, Node.SINGLE);
    amongst.addElement(rv);
    if (m_engine.watchCompilations())
      m_engine.outStream().print("+1");
    r.addNode(rv);
    return rv;
  }


  private void cleanupBindings(Value v) throws ReteException 
  {
    ValueVector vv = v.funcallValue();
    for (int i=0; i<vv.size(); i++) 
      {
        if (vv.get(i).type() ==  RU.INTARRAY)
          vv.get(i).intArrayValue()[0] = 0;
        else if (vv.get(i).type() ==  RU.FUNCALL)
          cleanupBindings(vv.get(i));
      }
  }


  Successor createSuccessor(Vector amongst, int cmd,
                            Value value, int R3, Defrule r)
       throws ReteException 
  {
    return createSuccessor(amongst, cmd, value, R3, -1, r);
  }
  Successor createSuccessor(Vector amongst, int cmd,
                            Value value, int R3, int R4, Defrule r)
       throws ReteException 
  {
    
    // Note that the 'value' object will contain bindings that refer to the real
    // pattern index; but tests will actually happen for single-pattern tokens
    // only. Therefore, some cleaning up is in order.

    if (value.type() == RU.FUNCALL)
      cleanupBindings(value);

    else if (value.type() == RU.INTARRAY)
      value.intArrayValue()[0] = 0;


    for (int i=0; i< amongst.size(); i++) 
      {
        Successor test = (Successor) amongst.elementAt(i);
        if (test.m_node instanceof Node1) 
          {
            Node1 node = (Node1) test.m_node;
            if ((node.m_command == cmd) && value.equals(node.m_value)
                && node.R3 == R3 && node.R4 == R4) 
              {
                if (m_engine.watchCompilations())
                  m_engine.outStream().print("=1");
                ++node.m_usecount;
                r.addNode(test);
                return test;
              }
          }
      }
    Node1 n = Node1.create(cmd, value, R3, R4, m_engine);
    Successor rv = new Successor(n, Node.SINGLE);
    amongst.addElement(rv);
    r.addNode(rv);
    if (m_engine.watchCompilations())
      m_engine.outStream().print("+1");

    return rv;
  }

  private void compilerError(String routine, String message)
       throws ReteException 
  {
    throw new ReteException("ReteCompiler::" + routine, message, "");
  }

}

