package consulo.remoteServer.runtime;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ServerConnectionManager {
  @Nonnull
  public static ServerConnectionManager getInstance() {
    return Application.get().getInstance(ServerConnectionManager.class);
  }

  @Nonnull
  public abstract <C extends ServerConfiguration> ServerConnection getOrCreateConnection(@Nonnull RemoteServer<C> server);

  @Nullable
  public abstract <C extends ServerConfiguration> ServerConnection getConnection(@Nonnull RemoteServer<C> server);

  @Nonnull
  public abstract Collection<ServerConnection> getConnections();
}
