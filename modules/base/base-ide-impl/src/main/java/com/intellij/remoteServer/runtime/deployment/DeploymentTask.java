package com.intellij.remoteServer.runtime.deployment;

import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface DeploymentTask<D extends DeploymentConfiguration> {
  @Nonnull
  DeploymentSource getSource();

  @Nonnull
  D getConfiguration();

  @Nonnull
  Project getProject();

  boolean isDebugMode();
}
