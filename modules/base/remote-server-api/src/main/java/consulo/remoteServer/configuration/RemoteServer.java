package consulo.remoteServer.configuration;

import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.ServerConfiguration;

/**
 * @author nik
 */
public interface RemoteServer<C extends ServerConfiguration> {
  /**
   * Identifies the server for good - its name and its configuration (an address) may change.
   */
  String getId();

  String getName();

  
  ServerType<C> getType();

  
  C getConfiguration();

  void setName(String name);
}
