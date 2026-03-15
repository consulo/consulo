package consulo.remoteServer.runtime;

import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.runtime.deployment.ServerRuntimeInstance;


/**
 * @author nik
 */
public abstract class ServerConnector<D extends DeploymentConfiguration> {
  public abstract void connect(ConnectionCallback<D> callback);

  public interface ConnectionCallback<D extends DeploymentConfiguration> extends RemoteOperationCallback {
    void connected(ServerRuntimeInstance<D> serverRuntimeInstance);
  }
}
