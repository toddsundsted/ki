package com.etcee.app.ki.server;

import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

class
AgentObjectOutputStream
extends ObjectOutputStream
{
  AgentObjectOutputStream(OutputStream outputstream)
  throws IOException
  {
    super(outputstream);
  }
}
