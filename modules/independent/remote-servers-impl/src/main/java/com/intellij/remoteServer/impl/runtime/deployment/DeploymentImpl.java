package com.intellij.remoteServer.impl.runtime.deployment;

import com.intellij.remoteServer.runtime.Deployment;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.deployment.DeploymentStatus;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public class DeploymentImpl implements Deployment {
  private final String myName;
  private final DeploymentTask<?> myDeploymentTask;
  private volatile DeploymentState myState;

  public DeploymentImpl(@Nonnull String name, @Nonnull DeploymentStatus status, @javax.annotation.Nullable String statusText,
                        @javax.annotation.Nullable DeploymentRuntime runtime, @javax.annotation.Nullable DeploymentTask<?> deploymentTask) {
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

  @javax.annotation.Nullable
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

    private DeploymentState(@Nonnull DeploymentStatus status, @javax.annotation.Nullable String statusText, @Nullable DeploymentRuntime runtime) {
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

    @javax.annotation.Nullable
    public DeploymentRuntime getRuntime() {
      return myRuntime;
    }
  }
}
