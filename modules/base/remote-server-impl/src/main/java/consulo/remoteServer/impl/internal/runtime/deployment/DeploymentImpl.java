package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class DeploymentImpl implements Deployment {
  private final String myName;
  private final DeploymentTask<?> myDeploymentTask;
  private volatile DeploymentState myState;

  public DeploymentImpl(@Nonnull String name, @Nonnull DeploymentStatus status, @Nullable String statusText,
                        @Nullable DeploymentRuntime runtime, @Nullable DeploymentTask<?> deploymentTask) {
    myName = name;
    myDeploymentTask = deploymentTask;
    myState = new DeploymentState(status, statusText, runtime);
  }

  @Nonnull
  public String getName() {
    return myName;
  }

  @Override
  @Nonnull
  public DeploymentStatus getStatus() {
    return myState.getStatus();
  }

  @Nonnull
  public String getStatusText() {
    String statusText = myState.getStatusText();
    return statusText != null ? statusText : getStatus().getPresentableText();
  }

  public DeploymentRuntime getRuntime() {
    return myState.getRuntime();
  }

  @Nullable
  @Override
  public DeploymentTask<?> getDeploymentTask() {
    return myDeploymentTask;
  }

  public boolean changeState(@Nonnull DeploymentStatus oldStatus, @Nonnull DeploymentStatus newStatus, @Nullable String statusText,
                             @Nullable DeploymentRuntime runtime) {
    if (myState.getStatus() == oldStatus) {
      myState = new DeploymentState(newStatus, statusText, runtime);
      return true;
    }
    return false;
  }

  private static class DeploymentState {
    private final DeploymentStatus myStatus;
    private final String myStatusText;
    private final DeploymentRuntime myRuntime;

    private DeploymentState(@Nonnull DeploymentStatus status, @Nullable String statusText, @Nullable DeploymentRuntime runtime) {
      myStatus = status;
      myStatusText = statusText;
      myRuntime = runtime;
    }

    @Nonnull
    public DeploymentStatus getStatus() {
      return myStatus;
    }

    @Nullable
    public String getStatusText() {
      return myStatusText;
    }

    @Nullable
    public DeploymentRuntime getRuntime() {
      return myRuntime;
    }
  }
}
