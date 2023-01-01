package consulo.ide.impl.idea.remoteServer.runtime.deployment;

import consulo.project.Project;
import consulo.ide.impl.idea.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.ide.impl.idea.remoteServer.configuration.deployment.DeploymentSource;
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
