package consulo.remoteServer.impl.internal.configuration;

import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;

import java.util.UUID;

/**
 * @author nik
 */
public class RemoteServerImpl<C extends ServerConfiguration> implements RemoteServer<C> {
  private final String myId;
  private String myName;
  private ServerType<C> myType;
  private C myConfiguration;

  public RemoteServerImpl(String name, ServerType<C> type, C configuration) {
    this(UUID.randomUUID().toString(), name, type, configuration);
  }

  public RemoteServerImpl(String id, String name, ServerType<C> type, C configuration) {
    myId = id;
    myName = name;
    myType = type;
    myConfiguration = configuration;
  }

  @Override
  public String getId() {
    return myId;
  }

  
  @Override
  public String getName() {
    return myName;
  }

  
  @Override
  public ServerType<C> getType() {
    return myType;
  }

  
  @Override
  public C getConfiguration() {
    return myConfiguration;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }
}
