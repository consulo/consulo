package com.intellij.remoteServer.configuration.localServer;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
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
