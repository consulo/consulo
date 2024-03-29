package consulo.remoteServer.runtime.deployment;

import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.runtime.RemoteOperationCallback;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class ServerRuntimeInstance<D extends DeploymentConfiguration> {

  public abstract void deploy(@Nonnull DeploymentTask<D> task, @Nonnull DeploymentLogManager logManager,
                              @Nonnull DeploymentOperationCallback callback);

  public abstract void computeDeployments(@Nonnull ComputeDeploymentsCallback callback);

  @Nonnull
  public String getDeploymentName(@Nonnull DeploymentSource source) {
    return source.getPresentableName();
  }

  public abstract void disconnect();

  public interface DeploymentOperationCallback extends RemoteOperationCallback {
    void succeeded(@Nonnull DeploymentRuntime deployment);
  }

  public interface ComputeDeploymentsCallback extends RemoteOperationCallback {
    void addDeployment(@Nonnull String deploymentName);
    void succeeded();
  }
}
