
import etcee.ki.server.AgentHostImplementation;

public final class Ki
{
  public static void main(String [] rgstr)
  {
    String strArg0 = rgstr.length > 0 ? rgstr[0] : "rmi://etcee/ki";
    String strArg1 = rgstr.length > 1 ? rgstr[1] : "db";
    String strArg2 = rgstr.length > 2 ? rgstr[2] : ".zip";
    String strArg3 = rgstr.length > 3 ? rgstr[3] : ".data";

    System.runFinalizersOnExit(true);

    try
    {
      new AgentHostImplementation(strArg0, strArg1, strArg2, strArg3);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}
