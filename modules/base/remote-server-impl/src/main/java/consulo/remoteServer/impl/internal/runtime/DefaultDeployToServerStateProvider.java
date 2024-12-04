/*
 * Copyright 2013-2024 consulo.io
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

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ExecutionException;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.runtime.deployment.DeployToServerStateProvider;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-03
 */
@ExtensionImpl(order = "last")
public class DefaultDeployToServerStateProvider implements DeployToServerStateProvider {
    @Nullable
    @Override
    public RunProfileState getState(RemoteServer<?> server, Executor executor, ExecutionEnvironment env, DeploymentSource source, DeploymentConfiguration config) throws ExecutionException {
        return new DeployToServerState<>(server, source, config, env);
    }
}
