package com.intellij.remoteServer.impl.runtime.deployment;

import com.intellij.remoteServer.runtime.deployment.DeploymentStatus;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class DeploymentInformation {
  private final DeploymentStatus myStatus;
  private final String myStatusText;

  public DeploymentInformation(@Nonnull DeploymentStatus status) {
    myStatus = status;
    myStatusText = status.name();
  }

  public DeploymentInformation(@Nonnull DeploymentStatus status, @Nonnull String statusText) {
    myStatus = status;
    myStatusText = statusText;
  }

  public DeploymentStatus getStatus() {
    return myStatus;
  }

  public String getStatusText() {
    return myStatusText;
  }
}
