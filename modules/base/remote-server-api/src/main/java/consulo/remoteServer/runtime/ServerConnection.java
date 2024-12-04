/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.remoteServer.runtime;

import consulo.project.Project;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public interface ServerConnection<D extends DeploymentConfiguration> {
    @Nonnull
    RemoteServer<?> getServer();

    @Nonnull
    ConnectionStatus getStatus();

    @Nonnull
    @Nls
    String getStatusText();

    void connect(@Nonnull Runnable onFinished);

    void disconnect();

    void deploy(@Nonnull DeploymentTask<D> task, @Nonnull Consumer<? super String> onDeploymentStarted);

    void computeDeployments(@Nonnull Runnable onFinished);

    void undeploy(@Nonnull Deployment deployment, @Nullable DeploymentRuntime runtime);

    @Nonnull
    Collection<Deployment> getDeployments();

    @Nullable
    DeploymentLogManager getLogManager(@Nonnull Project project, @Nonnull Deployment deployment);

    void connectIfNeeded(ServerConnector.ConnectionCallback<D> callback);
}
