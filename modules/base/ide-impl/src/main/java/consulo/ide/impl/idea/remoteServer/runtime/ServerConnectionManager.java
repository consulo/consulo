package consulo.ide.impl.idea.remoteServer.runtime;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.remoteServer.configuration.RemoteServer;
import consulo.ide.impl.idea.remoteServer.configuration.ServerConfiguration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
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
