package consulo.remoteServer.runtime.deployment;

import consulo.remoteServer.runtime.RemoteOperationCallback;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class DeploymentRuntime {
  public boolean isUndeploySupported() {
    return true;
  }

  public abstract void undeploy(@Nonnull UndeploymentTaskCallback callback);

  public interface UndeploymentTaskCallback extends RemoteOperationCallback {
    void succeeded();
  }
}
