/** **********************************************************************
 *  Parser functions for Java Expert System
 *
 * $Id$
 * (C) 1998 E.J. Friedman-Hill and the Sandia Corporation
 ********************************************************************** */

package jess;

import java.io.*;

public class Jesp
{
  private final String JAVACALL = "call";

  /**
    Stream where input comes from
    */
  private JessTokenStream m_jts;
  private Rete m_engine;

  /**
    Constructor
    */

  public Jesp(InputStream is, Rete e) 
  {
    // We retrieve the official wrapper, if any.
    DataInputStream dis = e.getInputWrapper(is);

    if (dis == null)
      dis = new DataInputStream(is);
    
    m_jts = new JessTokenStream(dis);
    m_engine = e;
  }  


  /**
    Parses an input file. Returns true on success.
    Argument is true if a prompt should be printed, false
    for no prompt.
    */

  public synchronized Value parse(boolean prompt) throws ReteException 
  {
    Value val = Funcall.TRUE();

    if (prompt)
      {
        m_engine.outStream().print("Jess> ");
        m_engine.outStream().flush();
      }
    while (!m_jts.eof() && val != null)
      {
        val = parseSexp();

        if (prompt)
          {
            if (!val.equals(Funcall.NIL()))
              {
              if (val.type() == RU.LIST)
                // Add parens to list
                m_engine.outStream().print('(');
              m_engine.outStream().print(val);
              if (val.type() == RU.LIST)
                m_engine.outStream().print(')');
              m_engine.outStream().println();
              }

            m_engine.outStream().print("Jess> ");
            m_engine.outStream().flush();
          }
      }
    if (m_jts.eof() && (m_jts.loner() != null))
      val = m_jts.loner();

    return val;
  }

  public void clear()
  {
    m_jts.clear();
  }

  /**
    Parses an input file containing only facts, asserts each one.
    */

  Value loadFacts() throws ReteException 
  {
    Value val = Funcall.TRUE();

    while (!m_jts.eof())
      {
        Fact f = parseFact();
        if (f == null)
          break;
        m_engine.assert(f.factData());
      }

    return val;
  }

  /** **********************************************************************
   * parseSexp
   *
   * Syntax:
   *  ( -Something- )
   *
   ********************************************************************** */

  private Value parseSexp() throws ReteException
  {
    try
      {
        String head = m_jts.head();
        
        if (head.equals("defrule"))
          return parseDefrule();
        
        else if (head.equals("deffacts"))
          return parseDeffacts();
        
        else if (head.equals("deftemplate"))
          return parseDeftemplate();
        
        else if (head.equals("deffunction"))
          return parseDeffunction();
        
        else if (head.equals("defglobal"))
          return parseDefglobal();
        
        else 
          {
            Funcall fc = parseFuncall();
            m_engine.globalContext().push();
            Funcall vv = m_engine.globalContext().expandAction(fc);
            Value v = Funcall.execute(vv, m_engine.globalContext());
            m_engine.globalContext().pop();
            return v;
          }
      }
    catch (ReteException re)
      {
        if (re instanceof ParseException)
          throw re;
        else
          {
            re.m_text1 = re.m_text1 + " " + re.m_text2 + " at line " +
              m_jts.lineno() + ": ";
            re.m_text2 = m_jts.toString();
            m_jts.clear();
            throw re;
          }
      }

  }

  /** **********************************************************************
   * parseDefglobal
   *
   * Syntax:
   *   (defglobal ?x = 3 ?y = 4 ... )
   *
   *********************************************************************** */

  private Value parseDefglobal() throws ReteException 
  {
    Defglobal dg = new Defglobal();

    /* ****************************************
       '(defglobal'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("defglobal")) )
      parseError("parseDefglobal", "Expected (defglobal...");


    /* ****************************************
       varname = value sets
       **************************************** */

    JessToken name, value;
    while ((name = m_jts.nextToken()).m_ttype != ')') 
      {

        if (name.m_ttype != RU.VARIABLE)
          parseError("parseDefglobal", "Expected a variable name");
      
        // Defglobal names must start and end with an asterisk!
        if (name.m_sval.charAt(0) != '*' ||
            name.m_sval.charAt(name.m_sval.length() -1) != '*')
          parseError("parseDefglobal", "Defglobal names must start and " +
                      "end with an asterisk!");

        if (m_jts.nextToken().m_ttype != '=')
          parseError("parseDefglobal", "Expected =");

        value = m_jts.nextToken();

        switch (value.m_ttype) 
          {

          case RU.ATOM: case RU.STRING: case RU.VARIABLE:
            dg.addGlobal(name.m_sval, new Value(value.m_sval, value.m_ttype));
            break;

          case RU.FLOAT:
          case RU.INTEGER:
            dg.addGlobal(name.m_sval, new Value(value.m_nval, value.m_ttype));
            break;

          case '(': 
            {
              m_jts.pushBack(value);
              Funcall fc = parseFuncall();
              m_engine.globalContext().push();
              Funcall vv = m_engine.globalContext().expandAction(fc);
              Value v = Funcall.execute(vv, m_engine.globalContext());
              m_engine.globalContext().pop();
              dg.addGlobal(name.m_sval, v); break;
            }
          default:
            parseError("parseDefglobal", "Bad value");

          }
      }

    m_engine.addDefglobal(dg);
    return Funcall.TRUE();

  }

  /** **********************************************************************
   * parseFuncall
   *
   * Syntax:
   *   (functor field2 (nested funcall) (double (nested funcall)))
   *
   * Trick: If the functor is a variable, we insert the functor 'call'
   * and assume we're going to make an outcall to Java on the object in
   * the variable!
   *
   ********************************************************************** */

  private Funcall parseFuncall() throws ReteException 
  {
    JessToken tok;
    String name = null;
    Funcall fc;
    
    if (m_jts.nextToken().m_ttype != '(')
      parseError("parseFuncall", "Expected '('");

    /* ****************************************
       functor
       **************************************** */
    tok = m_jts.nextToken();
    switch (tok.m_ttype) 
      {

      case RU.ATOM:
        name = tok.m_sval;
        break;

      case '-': case '/': case '=':
        // special functors
        name = "" + (char) tok.m_ttype;
        break;

      case RU.VARIABLE:
        // insert implied functor
        name = JAVACALL;
        break;

      default:
        parseError("parseFuncall", "Bad functor");
      }
    fc = new Funcall(name, m_engine);

    if (tok.m_ttype == RU.VARIABLE)
      fc.addArgument(tok.m_sval, RU.VARIABLE);

    /* ****************************************
       arguments
       **************************************** */
    tok = m_jts.nextToken();
    while (tok.m_ttype != ')') 
      {

        switch (tok.m_ttype) 
          {

            // simple arguments
          case RU.ATOM: case RU.STRING:
          case RU.VARIABLE: case RU.MULTIVARIABLE:
            fc.addArgument(tok.m_sval, tok.m_ttype); break;

          case RU.FLOAT:
          case RU.INTEGER:
            fc.addArgument(tok.m_nval, tok.m_ttype); break;

            // nested funcalls
          case '(':
            m_jts.pushBack(tok);
            if (name.equals("assert")) 
              {
                Fact fact = parseFact();
                fc.addArgument(fact);
                break;
          
              } 
            else if (name.equals("modify")) 
              {
                ValueVector pair = parseValuePair();
                fc.addArgument(pair, RU.LIST);
                break;

              }
            else 
              {
                Funcall fc2 = parseFuncall();
                fc.addArgument(fc2);
                break;
              }

          default:
            fc.addArgument(String.valueOf((char) tok.m_ttype), RU.STRING);
            break;

          } // switch tok.m_ttype
        tok = m_jts.nextToken();
      } // while tok.m_ttype != ')'

    return fc;
  }

  /** **********************************************************************
   * parseValuePair
   * These are used in (modify) funcalls
   *
   * Syntax:
   *   (ATOM VALUE)
   *
   ********************************************************************** */

  private ValueVector parseValuePair() throws ReteException 
  {
    ValueVector pair = new ValueVector(2);
    JessToken tok = null;

    /* ****************************************
       '(atom'
       **************************************** */

    if (m_jts.nextToken().m_ttype != '(' ||
        (tok = m_jts.nextToken()).m_ttype != RU.ATOM) 
      {
        parseError("parseValuePair", "Expected '( <atom>'");
      }

    pair.add(new Value(tok.m_sval, RU.ATOM));

    /* ****************************************
       value
       **************************************** */
    Value val = null;
    ValueVector multislot = null;

    do
      {
        if (val != null)
          {
            // Looks like this is a multislot
            if (multislot == null)
              multislot = new ValueVector();
            multislot.add(val);
          }
        
        switch ((tok = m_jts.nextToken()).m_ttype) 
          {            
          case RU.ATOM: case RU.STRING:
          case RU.VARIABLE: case RU.MULTIVARIABLE:
            val = new Value(tok.m_sval, tok.m_ttype); break;
            
          case RU.FLOAT:
          case RU.INTEGER:
            val = new Value(tok.m_nval, tok.m_ttype); break;
            
          case '(':
            m_jts.pushBack(tok);
            Funcall fc = parseFuncall();
            val = new Value(fc, RU.FUNCALL); break;
            
          case ')':
            break;

          default:
            parseError("parseValuePair", "Bad argument");
          }
      }
    while (tok.m_ttype != ')');

    // Now put the appropriate value in the pair
    if (multislot != null)
      pair.add(new Value(multislot, RU.LIST));
    else
      pair.add(val);
       
    return pair;
  }
  

  /** **********************************************************************
   * parseDeffacts
   *
   * Syntax:
   *  (deffacts <name> ["comment"] (fact) [(fact)...])
   *
   * ********************************************************************** */

  private Value parseDeffacts() throws ReteException 
  {
    Deffacts df = null;
    JessToken tok = null;

    /* ****************************************
       '(deffacts'
       **************************************** */

    if (m_jts.nextToken().m_ttype != '(' ||
        (tok = m_jts.nextToken()).m_ttype != RU.ATOM ||
        !tok.m_sval.equals("deffacts")) 
      {
        parseError("parseDeffacts", "Expected '( deffacts'");
      }

    /* ****************************************
       deffacts name
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDeffacts", "Expected deffacts name");
    df = new Deffacts(tok.m_sval);
  
    tok = m_jts.nextToken();

    /* ****************************************
       optional comment
       **************************************** */

    if (tok.m_ttype == RU.STRING) 
      {
        df.m_docstring = tok.m_sval;
        tok = m_jts.nextToken();
      } 

    /* ****************************************
       list of facts
       **************************************** */

    while (tok.m_ttype == '(') 
      {
        m_jts.pushBack(tok);
        Fact f = parseFact();
        df.addFact(m_engine.globalContext().expandFact(f.factData()));
        tok = m_jts.nextToken();
      }

    /* ****************************************
       closing paren
       **************************************** */

    if (tok.m_ttype != ')')
      parseError("parseDeffacts", "Expected ')'");

    m_engine.addDeffacts(df);
    return Funcall.TRUE();

  }

  /** **********************************************************************
   * parseFact
   * 
   * This is called from the parse routine for Deffacts and from the
   * Funcall parser for 'assert'; because of this latter, it can have
   * variables that need expanding.
   *
   * Syntax:
   *   ordered facts: (atom field1 2 "field3")
   *   unordered facts: (atom (slotname value) (slotname value2))
   *
   *********************************************************************** */

  Fact parseFact() throws ReteException 
  {
    String name, slot;
    int slot_type;
    Fact f;
    JessToken tok = null;

    /* ****************************************
       '( atom'
       **************************************** */

    if (m_jts.nextToken().m_ttype != '(' ||
        (tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseFact", "Expected '( <atom>'");
    
    name = tok.m_sval;
  
    /* ****************************************
       slot data
       What we do next depends on whether we're parsing
       an ordered or unordered fact. We can determine this very easily:
       If there is an existing unordered deftemplate, this is an
       unordered fact. Otherwise, it is ordered!
       **************************************** */

    // get a deftemplate if one already exists.

    ValueVector deft = m_engine.findDeftemplate(name);
    int ordered;
    if (deft == null)
      ordered = RU.ORDERED_FACT;
    else
      ordered = deft.get(RU.DESC).descriptorValue();

    if (ordered == RU.UNORDERED_FACT) 
      {

        /* ****************************************
           SLOT DATA FOR UNORDERED FACT
           **************************************** */
        f = new Fact(name, RU.UNORDERED_FACT, m_engine);
        tok = m_jts.nextToken();

        while (tok.m_ttype != ')') 
          {

            // Opening parenthesis
            if (tok.m_ttype != '(')
              parseError("parseFact", "Expected '('");

            // Slot name
            if  ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
              parseError("parseFact", "Bad slot name");
            slot = tok.m_sval;
        
            // Is this a slot or a multislot?
            slot_type = Deftemplate.slotType(deft, slot);

            switch (slot_type) 
              {

                // Data in normal slot
              case RU.SLOT:
                switch ((tok = m_jts.nextToken()).m_ttype) 
                  {
            
                  case RU.ATOM:
                  case RU.STRING:
                  case RU.VARIABLE: case RU.MULTIVARIABLE:
                    f.addValue(slot, tok.m_sval, tok.m_ttype); break;
            
                  case RU.FLOAT:
                  case RU.INTEGER:
                    f.addValue(slot, tok.m_nval, tok.m_ttype); break;
            
                  case '=':
                    tok = m_jts.nextToken();
                    if (tok.m_ttype != '(')
                      throw new ReteException("Jesp::parseFact",
                                              "'=' cannot appear as an " +
                                              "atom within a fact", "");
                    // FALLTHROUGH
                  case '(': 
                    {
                      m_jts.pushBack(tok);
                      Funcall fc = parseFuncall();
                      f.addValue(slot, fc, RU.FUNCALL); break;
                    }

                  default:
                    parseError("parseFact", "Bad slot value");
                  }
          
                if  ((tok = m_jts.nextToken()).m_ttype != ')')
                  parseError("parseFact", "Expected ')'");
                break;

              case RU.MULTISLOT:
                // Data in multislot
                // Code is very similar, but bits of data are added to a multifield
                ValueVector slot_vv = new ValueVector();
                tok = m_jts.nextToken();
                while (tok.m_ttype != ')') 
                  {
                    switch (tok.m_ttype) 
                      {
              
                      case RU.ATOM:
                      case RU.STRING:
                      case RU.VARIABLE: case RU.MULTIVARIABLE:
                        slot_vv.add(new Value(tok.m_sval, tok.m_ttype)); break;
                        
                      case RU.FLOAT:
                      case RU.INTEGER:
                        slot_vv.add(new Value(tok.m_nval, tok.m_ttype)); break;
              
                      case '=':
                        tok = m_jts.nextToken();
                        if (tok.m_ttype != '(')
                          throw new ReteException("Jesp::parseFact",
                                                  "'=' cannot appear as an " +
                                                  "atom within a fact", "");
                        // FALLTHROUGH
                      case '(': 
                        {
                          m_jts.pushBack(tok);
                          Funcall fc = parseFuncall();
                          slot_vv.add(new Value(fc, RU.FUNCALL)); break;
                        }

                      default:
                        parseError("parseFact", "Bad slot value");
                      }
            
                    tok = m_jts.nextToken();
            
                  }
                f.addValue(slot, new Value(slot_vv, RU.LIST));          
                break;

              default:
                parseError("parseFact", "No such slot in deftemplate");
              }          

            // hopefully advance to next ')'
            tok = m_jts.nextToken();
        
          }
      }
    else 
      {

        /* ****************************************
           parse SLOT DATA FOR ORDERED FACT
           **************************************** */

        f = new Fact(name, RU.ORDERED_FACT, m_engine);
        tok = m_jts.nextToken();

        while (tok.m_ttype != ')') 
          {

            switch (tok.m_ttype) 
              {

              case RU.ATOM:
              case RU.STRING:
              case RU.VARIABLE: case RU.MULTIVARIABLE:
                f.addValue(tok.m_sval, tok.m_ttype); break;

              case RU.FLOAT:
              case RU.INTEGER:
                f.addValue(tok.m_nval, tok.m_ttype); break;

              case '=':
                tok = m_jts.nextToken();
                if (tok.m_ttype != '(')
                  throw new ReteException("Jesp::parseFact",
                                          "'=' cannot appear as an " +
                                          "atom within a fact", "");
                // FALLTHROUGH
              case '(': 
                {
                  m_jts.pushBack(tok);
                  Funcall fc = parseFuncall();
                  f.addValue(fc, RU.FUNCALL); break;
                }

              default:
                f.addValue(tok.toString(), RU.ATOM); break;
              }
            tok = m_jts.nextToken();
          }
      }
    if (tok.m_ttype != ')')
      parseError("parseFact", "Expected ')'");

    return f;
    
  }

  /** **********************************************************************
   * parseDeftemplate
   * 
   *
   * Syntax:
   *   (deftemplate (slot foo (default <value>)) (multislot bar))
   *
   *********************************************************************** */

  private Value parseDeftemplate() throws ReteException 
  {
    Deftemplate dt;
    String name;
    int slot_type = RU.SLOT;
    Value default_value = null;
    JessToken tok;

    /* ****************************************
       '(deftemplate'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("deftemplate")) )
      parseError("parseDeftemplate", "Expected (deftemplate...");

    /* ****************************************
       deftemplate name, optional comment
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDeftemplate", "Expected deftemplate name");

    name = tok.m_sval;
    dt = new Deftemplate(tok.m_sval, RU.UNORDERED_FACT);

    if ((tok = m_jts.nextToken()).m_ttype == RU.STRING) 
      {
        dt.m_docstring = tok.m_sval;
        tok = m_jts.nextToken();
      }

    /* ****************************************
       individual slot descriptions
       **************************************** */
    
    // ( <slot type>

    while (tok.m_ttype == '(') 
      { // slot 
        if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM ||
            !(tok.m_sval.equals("slot") || tok.m_sval.equals("multislot")))
          parseError("parseDeftemplate", "Bad slot type");

        //if (tok.m_sval.equals("multislot"))
        //parseError("parseDeftemplate", "No multislot support yet!");
        
        slot_type = tok.m_sval.equals("slot") ? RU.SLOT : RU.MULTISLOT;
      
        // <slot name>
        if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
          parseError("parseDeftemplate", "Bad slot name");
        name = tok.m_sval;      
      
        // optional slot qualifiers
      
        default_value = new Value(RU.NONE, RU.NONE);
      
        tok = m_jts.nextToken();
        while (tok.m_ttype == '(') 
          { // slot qualifier
            if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
              parseError("parseDeftemplate", "Slot qualifier must be atom");
        
            // default value qualifier
        
            if (tok.m_sval.equalsIgnoreCase("default")) 
              {
                tok = m_jts.nextToken();
                switch (tok.m_ttype) 
                  {

                  case RU.ATOM: case RU.STRING:
                    default_value = new Value(tok.m_sval, tok.m_ttype); break;

                  case RU.FLOAT:
                  case RU.INTEGER:
                    default_value = new Value(tok.m_nval, tok.m_ttype); break;

                  default:
                    parseError("parseDeftemplate", "Illegal default slot value");
                  }
              }
            else if (tok.m_sval.equalsIgnoreCase("type")) 
              {
                // type is allowed, but ignored
                tok = m_jts.nextToken();
              } 
            else
              parseError("parseDeftemplate", "Unimplemented slot qualifier");
      
            if ((tok = m_jts.nextToken()).m_ttype != ')')
              parseError("parseDeftemplate", "Expected ')'");
      
            tok = m_jts.nextToken();
          }
        if (tok.m_ttype != ')')
          parseError("parseDeftemplate", "Expected ')'");
      
        if (slot_type == RU.SLOT)
          dt.addSlot(name, default_value);
        else
          dt.addMultiSlot(name, new Value(new ValueVector(), RU.LIST));
      
        tok = m_jts.nextToken();
      }
    if (tok.m_ttype != ')')
      parseError("parseDeftemplate", "Expected ')'");

    m_engine.addDeftemplate(dt);
    return Funcall.TRUE();
  }
  

  /** **********************************************************************
   * parseDefrule
   * Wrapper around doParseDefrule
   * We're going to split defrules into multiple rules is we see an (or) CE
   *********************************************************************** */
  private Value parseDefrule() throws ReteException 
  {
    Value v;
    v = doParseDefrule();
    return v;
  }

  /** **********************************************************************
   * doParseDefrule
   * 
   *
   * Syntax:
   * (defrule name
   *  [ "docstring...." ]
   *  [ (declare (salience 1)) ]
   *   (pattern 1)
   *   ?foo <- (pattern 2)
   *   (pattern 3)
   *  =>
   *   (action 1)
   *   (action ?foo)
   *   )
   *
   *********************************************************************** */

  private Value doParseDefrule() throws ReteException 
  {
    Defrule dr;
    JessToken tok, tok2, tok3, tok4, tok5;

    /* ****************************************
       '(defrule'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("defrule")) )
      parseError("parseDefrule", "Expected (defrule...");


    /* ****************************************
       defrule name, optional comment
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDefrule", "Expected defrule name");
    dr = new Defrule(tok.m_sval, m_engine);

    if ((tok = m_jts.nextToken()).m_ttype == RU.STRING) 
      {
        dr.m_docstring = tok.m_sval;
        tok = m_jts.nextToken();
      }

    int factIndex = 0;

    // check for salience declaration
    if (tok.m_ttype == '(')
      if ((tok2 = m_jts.nextToken()).m_ttype == RU.ATOM &&
          tok2.m_sval.equals("declare")) 
        {

          if ((tok2 = m_jts.nextToken()).m_ttype != '(' ||
              (tok2 = m_jts.nextToken()).m_ttype != RU.ATOM ||
              !tok2.m_sval.equals("salience")) 
            {
              parseError("parseDefrule", "Expected (salience ...");
            }
          if ((tok2 = m_jts.nextToken()).m_ttype != RU.INTEGER)
            parseError("parseDefrule", "Expected <integer>");
            
          dr.m_salience = (int) tok2.m_nval;
          if (m_jts.nextToken().m_ttype != ')' ||
              m_jts.nextToken().m_ttype != ')')
            parseError("parseDefrule", "Expected '))('");

          tok = m_jts.nextToken();

        }
      else 
        { // head wasn't 'declare'
          m_jts.pushBack(tok2);
        }

    // now we're looking for just patterns

    while (tok.m_ttype == '(' || tok.m_ttype == RU.VARIABLE) 
      {
        switch (tok.m_ttype) 
          {

          case '(': 
            {
              // pattern not bound to a var
              m_jts.pushBack(tok);
              Pattern p = parsePattern(0);

              // This is still an error for now
              if (p.negated() > 1)
                parseError("parseDefrule",
                            "Nested not CEs are not allowed yet.");

              // m_hasVariables will only be true if the pattern contains
              // NAMED variables - blank ones are fine.
              if (factIndex == 0 && (p.negated() != 0 ||
                                     p.name() == RU.putAtom("test")))
                {
                  if (p.m_hasVariables)
                    parseError("parseDefrule",
                                "First pattern in rule is a 'not' or 'test'" +
                                " CE containing variables");
                  else
                    {
                      dr.addPattern(new Pattern("initial-fact",
                                                RU.ORDERED_FACT, m_engine, 0));
                      ++factIndex;
                    }
                }
              dr.addPattern(p);
              ++factIndex;
              break;
            }

          case RU.VARIABLE: 
            {
              // pattern bound to a variable
              // These look like this:
              // ?name <- (pattern 1 2 3)

              dr.addBinding(RU.putAtom(tok.m_sval), factIndex, RU.PATTERN, -1);

              if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM ||
                  !tok.m_sval.equals("<-"))
                parseError("parseDefrule", "Expected '<-'");

              Pattern p = parsePattern(0);

              String docstring = 
                m_engine.findFullDeftemplate(RU.getAtom(p.name())).m_docstring;

              if (p.negated() != 0)
                parseError("parseDefrule",
                           "'not' and 'test' CE's cannot be bound to variables");
              
              dr.addPattern(p);
              ++factIndex;
              break;
            }
          } 
        tok = m_jts.nextToken();
      }

    if (factIndex == 0) 
      {
        // No patterns for this rule; we will fire on "initial-fact".
        Pattern p = new Pattern("initial-fact", RU.ORDERED_FACT, m_engine, 0);
        dr.addPattern(p);
      }

    if (tok.m_ttype != RU.ATOM || !tok.m_sval.equals("=>"))
      parseError("parseDefrule", "Expected '=>'");
    
    tok = m_jts.nextToken();

    while (tok.m_ttype == '(') 
      {
        m_jts.pushBack(tok);
        Funcall f = parseFuncall();
        dr.addAction(f);
        tok = m_jts.nextToken();
      }

    if (tok.m_ttype != ')')
      parseError("parseDefrule", "Expected ')'");

    m_engine.addDefrule(dr);
    return Funcall.TRUE();
  }

  
  /** **********************************************************************
   * parsePattern
   * 
   * parse a Pattern object in a Rule LHS context
   *
   * Syntax:
   * Like that of a fact, except that values can have complex forms like
   * 
   * ~value       (test for not a value)
   * ?X&~red      (store the value in X; fail match if not red)
   * ?X&:(> ?X 3) (store the value in X; fail match if not greater than 3)
   *
   *********************************************************************** */

  private Pattern parsePattern(int negcnt) throws ReteException 
  {
    String name, slot;
    Pattern p;
    JessToken tok = null;

    /* ****************************************
       ' ( <atom> '
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! ((tok = m_jts.nextToken()).m_ttype == RU.ATOM))
      parseError("parsePattern", "Expected '( <atom>'");

    name = tok.m_sval;

    /* ****************************************
       Special handling for NOT CEs
       **************************************** */

    if (name.equals("not")) 
      {
        // this is a negated pattern; strip off the (not ) and 
        // recursively parse the actual pattern.

        p = parsePattern(negcnt + 1);
        if (m_jts.nextToken().m_ttype != ')')
          parseError("parsePattern", "Expected ')'");
        return p;
      } 

    /* ****************************************
       Special handling for TEST CEs
       Note that these can be nested inside of NOTs.
       **************************************** */

    if (name.equals("test")) 
      {
        // this is a 'test' pattern. We trick up a fake one-slotted
        // pattern which will get treated specially by the compiler.
        p = new Pattern(name, RU.ORDERED_FACT, m_engine, 0);

        Funcall f = parseFuncall();
        p.addTest(new Value(f, RU.FUNCALL),
                  (negcnt % 2) == 1 ? true : false); 
        p.advance();

        if (m_jts.nextToken().m_ttype != ')')
          parseError("parsePattern", "Expected ')'");

        return p;
      } 

    /* ****************************************
       What we do next depends on whether we're parsing
       an ordered or unordered fact. We can determine this very
       easily: If there is a deftemplate, this is an unordered
       fact. Otherwise, it is ordered!
       **************************************** */

    ValueVector deft = m_engine.findDeftemplate(name);

    int ordered = (deft == null) ?
      RU.ORDERED_FACT : deft.get(RU.DESC).descriptorValue();

    if (ordered == RU.UNORDERED_FACT) 
      {

        /* ****************************************
           Actual Pattern slot data for unordered facts
           **************************************** */
        p = new Pattern(name, RU.UNORDERED_FACT, m_engine, negcnt);
        tok = m_jts.nextToken();
        while (tok.m_ttype == '(') 
          {
            if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
              parseError("parsePattern", "Bad slot name");

            slot = tok.m_sval;
            boolean multislot = (Deftemplate.slotType(deft, slot) == RU.MULTISLOT);

            tok = m_jts.nextToken();
            int subidx = (multislot ? 0 : -1);
            while (tok.m_ttype != ')') 
              {

                // if this is a '~'  pattern, keep track
                boolean not_slot = false;
                if (tok.m_ttype == '~') 
                  {
                    not_slot = true;
                    tok = m_jts.nextToken();
                  }
          
                switch (tok.m_ttype) 
                  {
                  case RU.VARIABLE: case RU.MULTIVARIABLE:
                    if (!tok.isBlankVariable())
                      p.m_hasVariables = true;

                    // FALL THROUGH
                  case RU.ATOM: case RU.STRING:
                    p.addTest(slot, new Value(tok.m_sval, tok.m_ttype),
                              subidx, not_slot);
                    break;

                  case RU.FLOAT:
                  case RU.INTEGER:
                    p.addTest(slot, new Value(tok.m_nval, tok.m_ttype),
                              subidx, not_slot);
                    break;

                  case ':':
                    {
                      Funcall f = parseFuncall();
                      p.addTest(slot, new Value(f, RU.FUNCALL), subidx,
                                not_slot);
                      break;
                    }

                    // We're going to handle these by transforming them into
                    // predicate constraints.

                  case '=':
                    {
                      Funcall inner = parseFuncall();

                      // We're building (eq* <this-slot> <inner>)
                      Funcall outer = new Funcall("eq*", m_engine);

                      // We need a variable that refers to this slot
                      int varname = RU.gensym("_=_");
                      p.addTest(slot, new Value(varname, RU.VARIABLE),
                                subidx, false);

                      // Finish up the Funcall
                      outer.addArgument(varname, RU.VARIABLE);
                      outer.addArgument(inner, RU.FUNCALL);

                      p.addTest(slot, new Value(outer, RU.FUNCALL), subidx,
                                not_slot);
                    }
                    break;

                  default:
                    parseError("parsePattern", "Bad slot value");
                  }
          
                tok = m_jts.nextToken();
          
                if (tok.m_ttype == '&')
                  tok = m_jts.nextToken();
                else
                  if (!multislot && tok.m_ttype != ')')
                    parseError("parsePattern", slot + " is not a multislot");
                  else
                    ++subidx;
              }

            if (multislot)
              p.setMultislotLength(slot, subidx);

            tok = m_jts.nextToken();
          
          }
        return p;

      }
    else 
      {

        /* ****************************************
           Actual Pattern slot data for ordered facts
           **************************************** */

        p = new Pattern(name, RU.ORDERED_FACT, m_engine, negcnt);
        // We definitely accept a superset of correct syntax here.
        // multifields and single fields can be mixed, and things like
        // ?a&?b&?c are parsed.

        tok = m_jts.nextToken();
        while (tok.m_ttype != ')') 
          {

            do 
              { // loop while the next token is a '&'
                boolean not_slot = false;
          
                if (tok.m_ttype == '&') 
                  {
                    tok = m_jts.nextToken();
                  }
          
                if (tok.m_ttype == '~') 
                  {
                    not_slot = true;
                    tok = m_jts.nextToken();
                  }
          
                switch (tok.m_ttype) 
                  {

                  case RU.VARIABLE: case RU.MULTIVARIABLE:
                    if (!tok.isBlankVariable())
                      p.m_hasVariables = true;

                    // FALL THROUGH
                  case RU.ATOM: case RU.STRING:
                    p.addTest(new Value(tok.m_sval, tok.m_ttype), not_slot);
                    break;

                  case RU.FLOAT:
                  case RU.INTEGER:
                    p.addTest(new Value(tok.m_nval, tok.m_ttype), not_slot);
                    break;

                  case ':':
                    Funcall f = parseFuncall();
                    p.addTest(new Value(f, RU.FUNCALL), not_slot); break;

                  case '=':
                    {
                      Funcall inner = parseFuncall();

                      // We're building (eq* <this-slot> <inner>)
                      Funcall outer = new Funcall("eq*", m_engine);

                      // We need a variable that refers to this slot
                      int varname = RU.gensym("_=_");
                      p.addTest(new Value(varname, RU.VARIABLE),
                                false);

                      // Finish up the Funcall
                      outer.addArgument(varname, RU.VARIABLE);
                      outer.addArgument(inner, RU.FUNCALL);

                      p.addTest(new Value(outer, RU.FUNCALL),
                                not_slot);
                    }
                    break;

                  default:
                    parseError("parsePattern", "Badslot value");
                  }
                tok = m_jts.nextToken();
              } while (tok.m_ttype == '&');
            p.advance();
          }
        return p;
      }
    
  }

  /** **********************************************************************
   * parseDeffunction
   * 
   * Syntax:
   *   (deffunction name ["doc-comment"] (<arg1><arg2...) ["doc-comment"]
   *   (action)
   *    value
   *   (action))
   *
   *********************************************************************** */
  
  private Value parseDeffunction() throws ReteException 
  {
    Deffunction df;
    JessToken tok;

    /* ****************************************
       '(deffunction'
       **************************************** */

    if (  (m_jts.nextToken().m_ttype != '(') ||
          ! (m_jts.nextToken().m_sval.equals("deffunction")) )
      parseError("parseDeffunction", "Expected (deffunction...");


    /* ****************************************
       defrule name
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype != RU.ATOM)
      parseError("parseDeffunction", "Expected deffunction name");
    df = new Deffunction(tok.m_sval, m_engine);
    
    /* ****************************************
       optional comment
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype == RU.STRING) 
      {
        df.m_docstring = tok.m_sval;
        tok = m_jts.nextToken();
      }

    /* ****************************************
       Argument list
       **************************************** */

    if (tok.m_ttype != '(') 
      parseError("parseDeffunction", "Expected '('");
    
    while ((tok = m_jts.nextToken()).m_ttype == RU.VARIABLE ||
           tok.m_ttype == RU.MULTIVARIABLE)
      df.addArgument(tok.m_sval);

    if (tok.m_ttype != ')') 
      parseError("parseDeffunction", "Expected ')'");


    /* ****************************************
       optional comment
       **************************************** */

    if ((tok = m_jts.nextToken()).m_ttype == RU.STRING) 
      {
        df.m_docstring = tok.m_sval;
        tok = m_jts.nextToken();
      }

    /* ****************************************
       function calls and values
       **************************************** */

    while (tok.m_ttype != ')') 
      {
        if (tok.m_ttype == '(') 
          {
            m_jts.pushBack(tok);
            Funcall f = parseFuncall();
            df.addAction(f);
          }
        else 
          {
            switch (tok.m_ttype) 
              {
          
              case RU.ATOM: case RU.STRING:
              case RU.VARIABLE: case RU.MULTIVARIABLE:
                df.addValue(new Value(tok.m_sval, tok.m_ttype)); break;
          
              case RU.FLOAT:
              case RU.INTEGER:
                df.addValue(new Value(tok.m_nval, tok.m_ttype)); break;
          
              default:
                parseError("parseDeffunction", "Unexpected character");
              }
          }
        tok = m_jts.nextToken();
      }

    m_engine.addDeffunction(df);
    return Funcall.TRUE();
  }

  /**
    Make error reporting a little more compact.
    */

  private void parseError(String routine, String msg) throws ReteException 
  {
    try
      {
        throw new ParseException("Jesp::" + routine,
                                msg  + " at line " + m_jts.lineno() + ": ",
                                m_jts.toString());
      }
    finally
      {
        m_jts.clear();
      }

  }

}


class ParseException extends ReteException
{
  ParseException(String s1, String s2, String s3) { super(s1, s2, s3); }
}


