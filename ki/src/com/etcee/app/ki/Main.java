package com.etcee.app.ki;

import com.etcee.app.ki.server.AgentHostImplementation;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Properties;

public
final
class Main
{
  public
  static
  void
  main(String [] arstring)
  {
    try
    {
      if (arstring.length > 1)
      {
        System.err.println("Usage: java com.etcee.app.Ki <property file>");
        System.exit(-1);
      }
      Properties properties = new Properties();
      // Load properties from file.
      if (arstring.length == 1)
      {
        FileInputStream fileinputstream = new FileInputStream(arstring[0]);
        properties.load(fileinputstream);
      }
      // Load properties from classpath.
      else
      {
        InputStream inputstream = Main.class.getResourceAsStream("ki.properties");
        properties.load(inputstream);
      }
      // Read properties.
      String stringHostname = properties.getProperty("com.etcee.app.ki.Main.hostname", "localhost");
      String stringPort = properties.getProperty("com.etcee.app.ki.Main.port", "6565");
      String stringName = "rmi://" + stringHostname + ":" + stringPort + "/ki";
      // Create and register a host.
      AgentHostImplementation agenthostimplementation = new AgentHostImplementation(stringName, properties);
      Registry registry = LocateRegistry.createRegistry(Integer.parseInt(stringPort));
      registry.bind(stringName, agenthostimplementation);
    }
    catch (Exception exception)
    {
      exception.printStackTrace();
    }
  }
}
