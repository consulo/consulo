package consulo.remoteServer.configuration;

import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.ServerConfiguration;

import jakarta.annotation.Nonnull;

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
