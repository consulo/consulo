/*
 * Copyright 2013-2026 consulo.io
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
package consulo.enviroment.remoteAgent;

import consulo.enviroment.remoteAgent.nio.RemoteNioFileSystem;
import consulo.enviroment.remoteAgent.nio.RemoteVirtualFileSystem;
import consulo.enviroment.remoteAgent.platform.RemotePlatform;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.platformAware.PlatformAwareServerRuntimeInstance;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import consulo.remoteServer.runtime.deployment.ServerRuntimeInstance;

/**
 * Runtime instance for a connected remote agent.
 * Holds the connection, platform, and filesystem objects.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
class RemoteAgentServerRuntimeInstance extends PlatformAwareServerRuntimeInstance<DeploymentConfiguration> {
    private final RemoteAgentConnection myConnection;
    private final RemotePlatform myPlatform;
    private final RemoteNioFileSystem myNioFileSystem;
    private final RemoteVirtualFileSystem myVirtualFileSystem;

    RemoteAgentServerRuntimeInstance(RemoteAgentConnection connection,
                                     RemotePlatform platform,
                                     RemoteNioFileSystem nioFileSystem) {
        myConnection = connection;
        myPlatform = platform;
        myNioFileSystem = nioFileSystem;
        myVirtualFileSystem = new RemoteVirtualFileSystem(connection, nioFileSystem);
    }

    @Override
    public void deploy(DeploymentTask<DeploymentConfiguration> task,
                       DeploymentLogManager logManager,
                       DeploymentOperationCallback callback) {
        callback.errorOccurred(LocalizeValue.localizeTODO("Deployment is not supported for remote agent connections"));
    }

    @Override
    public void computeDeployments(ComputeDeploymentsCallback callback) {
        callback.succeeded();
    }

    @Override
    public void disconnect() {
        myConnection.close();
    }

    public RemoteAgentConnection getConnection() {
        return myConnection;
    }

    @Override
    public Platform getPlatform() {
        return myPlatform;
    }

    public RemoteNioFileSystem getNioFileSystem() {
        return myNioFileSystem;
    }

    public RemoteVirtualFileSystem getVirtualFileSystem() {
        return myVirtualFileSystem;
    }
}
