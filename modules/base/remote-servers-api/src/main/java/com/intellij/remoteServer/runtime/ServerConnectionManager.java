package com.intellij.remoteServer.runtime;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.ServerConfiguration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * @author nik
 */
public abstract class ServerConnectionManager {
  @Nonnull
  public static ServerConnectionManager getInstance() {
    return ServiceManager.getService(ServerConnectionManager.class);
  }

  @Nonnull
  public abstract <C extends ServerConfiguration> ServerConnection getOrCreateConnection(@Nonnull RemoteServer<C> server);

  @Nullable
  public abstract <C extends ServerConfiguration> ServerConnection getConnection(@Nonnull RemoteServer<C> server);

  @Nonnull
  public abstract Collection<ServerConnection> getConnections();
}
