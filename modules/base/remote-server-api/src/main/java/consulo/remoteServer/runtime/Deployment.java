package consulo.remoteServer.runtime;

import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.remoteServer.runtime.deployment.DeploymentTask;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public interface Deployment {
  @Nonnull
  String getName();

  @Nonnull
  DeploymentStatus getStatus();

  @Nonnull
  String getStatusText();

  @Nullable
  DeploymentRuntime getRuntime();

  @Nullable
  DeploymentTask<?> getDeploymentTask();
}
