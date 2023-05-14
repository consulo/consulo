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
package consulo.remoteServer.impl.internal.runtime;

import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.impl.internal.runtime.deployment.DeploymentTaskImpl;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.remoteServer.runtime.deployment.debug.DebugConnector;
import consulo.remoteServer.runtime.ui.RemoteServersView;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author nik
 */
public class DeployToServerState<S extends ServerConfiguration, D extends DeploymentConfiguration> implements RunProfileState {
  @Nonnull
  private final RemoteServer<S> myServer;
  @Nonnull
  private final DeploymentSource mySource;
  @Nonnull
  private final D myConfiguration;
  @Nonnull
  private final ExecutionEnvironment myEnvironment;

  public DeployToServerState(@Nonnull RemoteServer<S> server, @Nonnull DeploymentSource deploymentSource, @Nonnull D deploymentConfiguration, @Nonnull ExecutionEnvironment environment) {
    myServer = server;
    mySource = deploymentSource;
    myConfiguration = deploymentConfiguration;
    myEnvironment = environment;
  }

  @Nullable
  @Override
  public ExecutionResult execute(Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException {
    final ServerConnection connection = ServerConnectionManager.getInstance().getOrCreateConnection(myServer);
    final Project project = myEnvironment.getProject();
    RemoteServersView.getInstance(project).showServerConnection(connection);

    final DebugConnector<?, ?> debugConnector;
    if (DefaultDebugExecutor.getDebugExecutorInstance().equals(executor)) {
      debugConnector = myServer.getType().createDebugConnector();
    }
    else {
      debugConnector = null;
    }
    connection.computeDeployments(new Runnable() {
      @Override
      public void run() {
        connection.deploy(new DeploymentTaskImpl(mySource, myConfiguration, project, debugConnector, myEnvironment), new Consumer<String>() {
          @Override
          public void accept(String s) {
            RemoteServersView.getInstance(project).showDeployment(connection, s);
          }
        });
      }
    });
    return null;
  }
}
