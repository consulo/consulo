package consulo.remoteServer.runtime.deployment;

import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;

public interface DeploymentTask<D extends DeploymentConfiguration> {
    
    DeploymentSource getSource();

    
    D getConfiguration();

    
    Project getProject();

    boolean isDebugMode();

    
    ExecutionEnvironment getExecutionEnvironment();
}
