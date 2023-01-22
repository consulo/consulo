package consulo.remoteServer.runtime.deployment.debug;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
@Deprecated
public class JavaDebugConnectionData implements DebugConnectionData {
  private final String myHost;
  private final int myPort;

  public JavaDebugConnectionData(@Nonnull String host, int port) {
    myHost = host;
    myPort = port;
  }

  @Nonnull
  public String getHost() {
    return myHost;
  }

  public int getPort() {
    return myPort;
  }
}
