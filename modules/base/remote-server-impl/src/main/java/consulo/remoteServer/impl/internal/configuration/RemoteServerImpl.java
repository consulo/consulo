package consulo.remoteServer.impl.internal.configuration;

import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import jakarta.annotation.Nonnull;

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
