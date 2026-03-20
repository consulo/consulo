package consulo.remoteServer.runtime.deployment.debug;

/**
 * @author nik
 */
@Deprecated
public class JavaDebugConnectionData implements DebugConnectionData {
  private final String myHost;
  private final int myPort;

  public JavaDebugConnectionData(String host, int port) {
    myHost = host;
    myPort = port;
  }

  
  public String getHost() {
    return myHost;
  }

  public int getPort() {
    return myPort;
  }
}
