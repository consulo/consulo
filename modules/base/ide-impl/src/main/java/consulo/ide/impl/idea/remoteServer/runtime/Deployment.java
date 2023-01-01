package consulo.ide.impl.idea.remoteServer.runtime;

import consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentTask;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
