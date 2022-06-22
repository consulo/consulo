package consulo.ide.impl.idea.remoteServer.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.remoteServer.ServerType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author nik
 */
@Service(ComponentScope.APPLICATION)
public abstract class RemoteServersManager {
  public static RemoteServersManager getInstance() {
    return ServiceManager.getService(RemoteServersManager.class);
  }

  public abstract List<RemoteServer<?>> getServers();

  public abstract <C extends ServerConfiguration> List<RemoteServer<C>> getServers(@Nonnull ServerType<C> type);

  @Nullable
  public abstract <C extends ServerConfiguration> RemoteServer<C> findByName(@Nonnull String name, @Nonnull ServerType<C> type);

  public abstract <C extends ServerConfiguration> RemoteServer<C> createServer(@Nonnull ServerType<C> type, @Nonnull String name);

  public abstract void addServer(RemoteServer<?> server);

  public abstract void removeServer(RemoteServer<?> server);
}
