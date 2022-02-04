package com.intellij.remoteServer.runtime;

import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.deployment.DeploymentStatus;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
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
