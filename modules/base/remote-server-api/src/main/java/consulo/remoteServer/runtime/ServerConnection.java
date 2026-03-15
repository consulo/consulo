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
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public interface ServerConnection<D extends DeploymentConfiguration> {
    
    RemoteServer<?> getServer();

    
    ConnectionStatus getStatus();

    
    
    String getStatusText();

    void connect(Runnable onFinished);

    void disconnect();

    void deploy(DeploymentTask<D> task, Consumer<? super String> onDeploymentStarted);

    void computeDeployments(Runnable onFinished);

    void undeploy(Deployment deployment, @Nullable DeploymentRuntime runtime);

    
    Collection<Deployment> getDeployments();

    @Nullable
    DeploymentLogManager getLogManager(Project project, Deployment deployment);

    void connectIfNeeded(ServerConnector.ConnectionCallback<D> callback);
}
