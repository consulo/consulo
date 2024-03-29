package consulo.remoteServer.impl.internal.configuration;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.*;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServerListener;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@Singleton
@State(name = "RemoteServers", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/remote-servers.xml")})
@ServiceImpl
public class RemoteServersManagerImpl extends RemoteServersManager implements PersistentStateComponent<RemoteServersManagerState> {
  public static final SkipDefaultValuesSerializationFilters SERIALIZATION_FILTERS = new SkipDefaultValuesSerializationFilters();
  private List<RemoteServer<?>> myServers = new ArrayList<>();
  private List<RemoteServerState> myUnknownServers = new ArrayList<>();
  private final MessageBus myMessageBus;

  @Inject
  public RemoteServersManagerImpl(Application application) {
    myMessageBus = application.getMessageBus();
  }

  @Override
  public List<RemoteServer<?>> getServers() {
    return Collections.unmodifiableList(myServers);
  }

  @Override
  public <C extends ServerConfiguration> List<RemoteServer<C>> getServers(@Nonnull ServerType<C> type) {
    List<RemoteServer<C>> servers = new ArrayList<>();
    for (RemoteServer<?> server : myServers) {
      if (server.getType().equals(type)) {
        servers.add((RemoteServer<C>)server);
      }
    }
    return servers;
  }

  @Nullable
  @Override
  public <C extends ServerConfiguration> RemoteServer<C> findByName(@Nonnull String name, @Nonnull ServerType<C> type) {
    for (RemoteServer<?> server : myServers) {
      if (server.getType().equals(type) && server.getName().equals(name)) {
        return (RemoteServer<C>)server;
      }
    }
    return null;
  }

  @Override
  public <C extends ServerConfiguration> RemoteServer<C> createServer(@Nonnull ServerType<C> type, @Nonnull String name) {
    return new RemoteServerImpl<>(name, type, type.createDefaultConfiguration());
  }

  @Override
  public void addServer(RemoteServer<?> server) {
    myServers.add(server);
    myMessageBus.syncPublisher(RemoteServerListener.class).serverAdded(server);
  }

  @Override
  public void removeServer(RemoteServer<?> server) {
    myServers.remove(server);
    myMessageBus.syncPublisher(RemoteServerListener.class).serverRemoved(server);
  }

  @Nullable
  @Override
  public RemoteServersManagerState getState() {
    RemoteServersManagerState state = new RemoteServersManagerState();
    for (RemoteServer<?> server : myServers) {
      RemoteServerState serverState = new RemoteServerState();
      serverState.myName = server.getName();
      serverState.myTypeId = server.getType().getId();
      serverState.myConfiguration = XmlSerializer.serialize(server.getConfiguration().getSerializer().getState(), SERIALIZATION_FILTERS);
      state.myServers.add(serverState);
    }
    state.myServers.addAll(myUnknownServers);
    return state;
  }

  @Override
  public void loadState(RemoteServersManagerState state) {
    myUnknownServers.clear();
    myServers.clear();
    for (RemoteServerState server : state.myServers) {
      ServerType<?> type = findServerType(server.myTypeId);
      if (type == null) {
        myUnknownServers.add(server);
      }
      else {
        myServers.add(createConfiguration(type, server));
      }
    }
  }

  private static <C extends ServerConfiguration> RemoteServerImpl<C> createConfiguration(ServerType<C> type, RemoteServerState server) {
    C configuration = type.createDefaultConfiguration();
    PersistentStateComponent<?> serializer = configuration.getSerializer();
    ComponentSerializationUtil.loadComponentState(serializer, server.myConfiguration);
    return new RemoteServerImpl<>(server.myName, type, configuration);
  }

  @Nullable
  private static ServerType<?> findServerType(@Nonnull String typeId) {
    for (ServerType serverType : ServerType.EP_NAME.getExtensionList()) {
      if (serverType.getId().equals(typeId)) {
        return serverType;
      }
    }
    return null;
  }
}
