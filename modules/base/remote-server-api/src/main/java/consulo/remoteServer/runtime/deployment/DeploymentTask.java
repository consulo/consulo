package consulo.remoteServer.runtime.deployment;

import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import jakarta.annotation.Nonnull;

public interface DeploymentTask<D extends DeploymentConfiguration> {
    @Nonnull
    DeploymentSource getSource();

    @Nonnull
    D getConfiguration();

    @Nonnull
    Project getProject();

    boolean isDebugMode();

    @Nonnull
    ExecutionEnvironment getExecutionEnvironment();
}
