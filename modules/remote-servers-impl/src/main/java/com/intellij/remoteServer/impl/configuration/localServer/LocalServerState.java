/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.remoteServer.impl.configuration.localServer;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.remoteServer.configuration.ServerConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.localServer.LocalRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class LocalServerState<S extends ServerConfiguration, D extends DeploymentConfiguration> implements RunProfileState {
  @NotNull private final LocalRunner<D> myLocalRunner;
  @NotNull private final DeploymentSource mySource;
  @NotNull private final D myConfiguration;
  @NotNull private final ExecutionEnvironment myEnvironment;

  public LocalServerState(@NotNull LocalRunner<D> localRunner,
                          @NotNull DeploymentSource deploymentSource,
                          @NotNull D deploymentConfiguration,
                          @NotNull ExecutionEnvironment environment) {
    myLocalRunner = localRunner;
    mySource = deploymentSource;
    myConfiguration = deploymentConfiguration;
    myEnvironment = environment;
  }

  @Nullable
  @Override
  public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
    return myLocalRunner.execute(mySource, myConfiguration, myEnvironment, executor, runner);
  }
}
