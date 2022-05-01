package consulo.ide.impl.idea.remoteServer.runtime;

import consulo.ide.impl.idea.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.ide.impl.idea.remoteServer.runtime.deployment.ServerRuntimeInstance;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class ServerConnector<D extends DeploymentConfiguration> {
  public abstract void connect(@Nonnull ConnectionCallback<D> callback);

  public interface ConnectionCallback<D extends DeploymentConfiguration> extends RemoteOperationCallback {
    void connected(@Nonnull ServerRuntimeInstance<D> serverRuntimeInstance);
  }
}
