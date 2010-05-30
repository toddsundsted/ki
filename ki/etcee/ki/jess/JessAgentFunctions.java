
package etcee.ki.jess;

import etcee.ki.agent.Agent;
import etcee.ki.agent.AgentContext;
import etcee.ki.agent.AgentIdentity;
import etcee.ki.agent.Message;
import etcee.ki.agent.MessageResponse;

import jess.Rete;
import jess.ReteException;
import jess.Userpackage;
import jess.Userfunction;
import jess.Context;
import jess.ValueVector;
import jess.Value;
import jess.Funcall;
import jess.RU;

import java.util.Enumeration;

public class JessAgentFunctions
  implements Userpackage
{
  public static Value
  objectToValue(Object object)
    throws ReteException
  {
    Value value = null;

    if (object == null)
    {
      value = Funcall.NIL();
    }
    else if (object instanceof Value)
    {
      value = (Value)object;
    }
    else if (object instanceof String)
    {
      value = new Value((String)object, RU.STRING);
    }
    else if (object instanceof Integer)
    {
      value = new Value(((Integer)object).intValue(), RU.INTEGER);
    }
    else if (object instanceof Double)
    {
      value = new Value(((Double)object).doubleValue(), RU.FLOAT);
    }
    else
    {
      value = new Value(object, RU.EXTERNAL_ADDRESS);
    }

    return value;
  }

  public static Object
  valueToObject(Value value)
    throws ReteException
  {
    Object object = null;

    switch (value.type())
    {
      case RU.ATOM:
      case RU.STRING:
      case RU.VARIABLE:
        object = value.stringValue();
        break;
      case RU.EXTERNAL_ADDRESS:
        object = value.externalAddressValue();
        break;
      case RU.INTEGER:
        object = new Integer(value.intValue());
        break;
      case RU.FLOAT:
        object = new Double(value.floatValue());
        break;
    }

    return object;
  }

  private Agent agent = null;
  private AgentContext agentcontext = null;

  public void
  setAgentAndAgentContext(Agent agent, AgentContext agentcontext)
  {
    this.agent = agent;
    this.agentcontext = agentcontext;
  }

  public void
  add(Rete rete)
  {
    rete.addUserfunction(new agent_self());
    rete.addUserfunction(new agent_hostname());
    rete.addUserfunction(new agent_list());
    rete.addUserfunction(new agent_send());
    rete.addUserfunction(new agent_transfer());
  }

  class agent_self
    implements Userfunction
  {
    public int
    name()
    {
      return RU.putAtom("self");
    }

    public Value
    call(ValueVector valuevector, Context context)
      throws ReteException
    {
      return new Value(agentcontext.getAgentIdentity(), RU.EXTERNAL_ADDRESS);
    }
  }

  class agent_hostname
    implements Userfunction
  {
    public int
    name()
    {
      return RU.putAtom("hostname");
    }

    public Value
    call(ValueVector valuevector, Context context)
      throws ReteException
    {
      return new Value(agentcontext.getAgentHostName(), RU.STRING);
    }
  }

  class agent_list
    implements Userfunction
  {
    public int
    name()
    {
      return RU.putAtom("list");
    }

    public Value
    call(ValueVector valuevector, Context context)
      throws ReteException
    {
      ValueVector valuevectorT = new ValueVector();

      Enumeration enumeration = agentcontext.getPublishedAgents();

      while (enumeration.hasMoreElements())
      {
        valuevectorT.add(new Value((String)enumeration.nextElement(), RU.STRING));
      }

      return new Value(valuevectorT, RU.LIST);
    }
  }

  class agent_send
    implements Userfunction
  {
    public int
      name()
    {
      return RU.putAtom("send");
    }

    public Value
    call(ValueVector valuevector, Context context)
      throws ReteException
    {
      Value value = null;

      AgentIdentity agentidentity = null;

      value = valuevector.get(1);

      if (value.type() == RU.STRING)
      {
        agentidentity = agentcontext.getPublishedAgentIdentity(value.stringValue());
      }
      else if (value.type() == RU.EXTERNAL_ADDRESS)
      {
        agentidentity = (AgentIdentity)value.externalAddressValue();
      }

      if (agentidentity == null)
      {
        return Funcall.FALSE();
      }

      String strMessage = null;

      value = valuevector.get(2);

      if (value.type() == RU.STRING)
      {
        strMessage = value.stringValue();
      }

      if (strMessage == null)
      {
        return Funcall.FALSE();
      }

      Object [] rgobject = new Object [valuevector.size() - 3];

      for (int i = 3; i < valuevector.size(); i++)
      {
        rgobject[i - 3] = valueToObject(valuevector.get(i));
      }

      Message message = new Message(strMessage, rgobject);

      MessageResponse messageresponse = agentcontext.sendMessage(null, agentidentity, message);

      messageresponse.waitForResponse();

      if (messageresponse.getResponseCode() == MessageResponse.FAILED)
      {
        ValueVector valuevectorT = new ValueVector();

        valuevectorT.add(new Value(-1, RU.INTEGER));
        valuevectorT.add(objectToValue(messageresponse.getException()));

        return new Value(valuevectorT, RU.LIST);
      }
      else
      {
        ValueVector valuevectorT = new ValueVector();

        valuevectorT.add(new Value(0, RU.INTEGER));
        valuevectorT.add(objectToValue(messageresponse.getResponse()));

        return new Value(valuevectorT, RU.LIST);
      }
    }
  }

  class agent_transfer
    implements Userfunction
  {
    public int
    name()
    {
      return RU.putAtom("transfer");
    }

    public Value
    call(ValueVector valuevector, Context context)
      throws ReteException
    {
      Value value = null;

      String strAgentHostName = null;

      value = valuevector.get(1);

      if (value.type() == RU.STRING)
      {
        strAgentHostName = value.stringValue();
      }

      if (strAgentHostName == null)
      {
        return Funcall.FALSE();
      }

      agentcontext.requestToTransfer(strAgentHostName);

      return Funcall.TRUE();
    }
  }
}
