package com.intellij.remoteServer.impl.configuration;

import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class RemoteServersManagerState {
  @Property(surroundWithTag = false)
  @AbstractCollection(surroundWithTag = false)
  public List<RemoteServerState> myServers = new ArrayList<RemoteServerState>();
}
