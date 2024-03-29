package consulo.remoteServer.impl.internal.runtime;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class ServerConnectionManagerImpl extends ServerConnectionManager {
  private final Map<RemoteServer<?>, ServerConnection> myConnections = new HashMap<RemoteServer<?>, ServerConnection>();
  private final ServerConnectionEventDispatcher myEventDispatcher = new ServerConnectionEventDispatcher();

  @Nonnull
  @Override
  public <C extends ServerConfiguration> ServerConnection getOrCreateConnection(@Nonnull RemoteServer<C> server) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    ServerConnection connection = myConnections.get(server);
    if (connection == null) {
      ServerTaskExecutorImpl executor = new ServerTaskExecutorImpl();
      connection = new ServerConnectionImpl(server, server.getType().createConnector(server.getConfiguration(), executor), this);
      myConnections.put(server, connection);
      myEventDispatcher.fireConnectionCreated(connection);
    }
    return connection;
  }

  @jakarta.annotation.Nullable
  @Override
  public <C extends ServerConfiguration> ServerConnection getConnection(@Nonnull RemoteServer<C> server) {
    return myConnections.get(server);
  }

  public void removeConnection(RemoteServer<?> server) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myConnections.remove(server);
  }

  public ServerConnectionEventDispatcher getEventDispatcher() {
    return myEventDispatcher;
  }

  @Nonnull
  @Override
  public Collection<ServerConnection> getConnections() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return Collections.unmodifiableCollection(myConnections.values());
  }
}
