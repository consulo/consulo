package consulo.remoteServer.runtime.deployment;

import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
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
