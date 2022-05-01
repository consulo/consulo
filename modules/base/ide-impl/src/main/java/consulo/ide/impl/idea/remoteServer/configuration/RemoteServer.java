package consulo.ide.impl.idea.remoteServer.configuration;

import consulo.ide.impl.idea.remoteServer.ServerType;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface RemoteServer<C extends ServerConfiguration> {
  @Nonnull
  String getName();

  @Nonnull
  ServerType<C> getType();

  @Nonnull
  C getConfiguration();

  void setName(String name);
}
