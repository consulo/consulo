package com.intellij.remoteServer.impl.configuration.localServer;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 18:38/01.10.13
 */
public interface LocalRunner<D extends DeploymentConfiguration> {
  ExecutionResult execute(@NotNull DeploymentSource deploymentSource,
                          D configuration,
                          ExecutionEnvironment environment,
                          @NotNull Executor executor,
                          @NotNull ProgramRunner runner) throws ExecutionException;

}
