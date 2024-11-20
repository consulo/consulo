package consulo.remoteServer.runtime.deployment;

import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import org.jetbrains.annotations.NotNull;

public interface DeploymentTask<D extends DeploymentConfiguration> {
    @NotNull
    DeploymentSource getSource();

    @NotNull
    D getConfiguration();

    @NotNull
    Project getProject();

    boolean isDebugMode();

    @NotNull
    ExecutionEnvironment getExecutionEnvironment();
}
