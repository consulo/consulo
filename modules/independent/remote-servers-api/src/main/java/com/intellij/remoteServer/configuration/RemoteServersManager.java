package com.intellij.remoteServer.configuration;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.remoteServer.ServerType;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public abstract class RemoteServersManager {
  public static RemoteServersManager getInstance() {
    return ServiceManager.getService(RemoteServersManager.class);
  }

  public abstract List<RemoteServer<?>> getServers();

  public abstract <C extends ServerConfiguration> List<RemoteServer<C>> getServers(@Nonnull ServerType<C> type);

  @javax.annotation.Nullable
  public abstract <C extends ServerConfiguration> RemoteServer<C> findByName(@Nonnull String name, @Nonnull ServerType<C> type);

  public abstract <C extends ServerConfiguration> RemoteServer<C> createServer(@Nonnull ServerType<C> type, @Nonnull String name);

  public abstract void addServer(RemoteServer<?> server);

  public abstract void removeServer(RemoteServer<?> server);
}
