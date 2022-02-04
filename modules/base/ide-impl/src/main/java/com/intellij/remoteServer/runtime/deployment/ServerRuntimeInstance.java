package com.intellij.remoteServer.runtime.deployment;

import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.RemoteOperationCallback;
import javax.annotation.Nonnull;

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
