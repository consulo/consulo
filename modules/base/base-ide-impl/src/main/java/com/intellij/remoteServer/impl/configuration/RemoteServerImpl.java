package com.intellij.remoteServer.impl.configuration;

import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.ServerConfiguration;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class RemoteServerImpl<C extends ServerConfiguration> implements RemoteServer<C> {
  private String myName;
  private ServerType<C> myType;
  private C myConfiguration;

  public RemoteServerImpl(String name, ServerType<C> type, C configuration) {
    myName = name;
    myType = type;
    myConfiguration = configuration;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public ServerType<C> getType() {
    return myType;
  }

  @Nonnull
  @Override
  public C getConfiguration() {
    return myConfiguration;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }
}
