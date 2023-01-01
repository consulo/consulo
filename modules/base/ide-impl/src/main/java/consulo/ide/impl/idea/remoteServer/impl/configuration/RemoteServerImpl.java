package consulo.ide.impl.idea.remoteServer.impl.configuration;

import consulo.ide.impl.idea.remoteServer.ServerType;
import consulo.ide.impl.idea.remoteServer.configuration.RemoteServer;
import consulo.ide.impl.idea.remoteServer.configuration.ServerConfiguration;
import javax.annotation.Nonnull;

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
