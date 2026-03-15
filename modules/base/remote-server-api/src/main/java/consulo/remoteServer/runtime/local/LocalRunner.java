package consulo.remoteServer.runtime.local;

import consulo.execution.ExecutionResult;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ExecutionException;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;


/**
 * @author VISTALL
 * @since 18:38/01.10.13
 */
public interface LocalRunner<D extends DeploymentConfiguration> {
  ExecutionResult execute(DeploymentSource deploymentSource, D configuration, ExecutionEnvironment environment, Executor executor, ProgramRunner runner)
          throws ExecutionException;

}
