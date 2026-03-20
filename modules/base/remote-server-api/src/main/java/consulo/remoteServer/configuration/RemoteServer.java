package consulo.remoteServer.configuration;

import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.ServerConfiguration;

/**
 * @author nik
 */
public interface RemoteServer<C extends ServerConfiguration> {
  
  String getName();

  
  ServerType<C> getType();

  
  C getConfiguration();

  void setName(String name);
}
