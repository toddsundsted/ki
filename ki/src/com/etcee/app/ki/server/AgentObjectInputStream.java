package com.etcee.app.ki.server;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;

class
AgentObjectInputStream
extends ObjectInputStream
{
  private
  ClassLoader m_classloader = null;

  AgentObjectInputStream(ClassLoader classloader, InputStream inputstream)
  throws IOException
  {
    super(inputstream);
    m_classloader = classloader;
  }

  protected
  Class
  resolveClass(ObjectStreamClass objectstreamclass)
  throws ClassNotFoundException,
         IOException
  {
    return m_classloader.loadClass(objectstreamclass.getName());
  }
}
