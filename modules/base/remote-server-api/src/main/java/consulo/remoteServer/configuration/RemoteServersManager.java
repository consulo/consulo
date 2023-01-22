package consulo.remoteServer.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.remoteServer.ServerType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RemoteServersManager {
  @Deprecated
  public static RemoteServersManager getInstance() {
    return Application.get().getInstance(RemoteServersManager.class);
  }

  public abstract List<RemoteServer<?>> getServers();

  public abstract <C extends ServerConfiguration> List<RemoteServer<C>> getServers(@Nonnull ServerType<C> type);

  @Nullable
  public abstract <C extends ServerConfiguration> RemoteServer<C> findByName(@Nonnull String name, @Nonnull ServerType<C> type);

  public abstract <C extends ServerConfiguration> RemoteServer<C> createServer(@Nonnull ServerType<C> type, @Nonnull String name);

  public abstract void addServer(RemoteServer<?> server);

  public abstract void removeServer(RemoteServer<?> server);
}
