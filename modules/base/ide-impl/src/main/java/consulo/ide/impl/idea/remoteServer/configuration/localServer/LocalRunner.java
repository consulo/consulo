package consulo.ide.impl.idea.remoteServer.configuration.localServer;

import consulo.process.ExecutionException;
import consulo.execution.ExecutionResult;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.ide.impl.idea.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.ide.impl.idea.remoteServer.configuration.deployment.DeploymentSource;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18:38/01.10.13
 */
public interface LocalRunner<D extends DeploymentConfiguration> {
  ExecutionResult execute(@Nonnull DeploymentSource deploymentSource,
                          D configuration,
                          ExecutionEnvironment environment,
                          @Nonnull Executor executor,
                          @Nonnull ProgramRunner runner) throws ExecutionException;

}
