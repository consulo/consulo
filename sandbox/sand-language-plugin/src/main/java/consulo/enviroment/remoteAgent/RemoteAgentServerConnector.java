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

import consulo.enviroment.remoteAgent.nio.RemoteFileSystemProvider;
import consulo.enviroment.remoteAgent.nio.RemoteNioFileSystem;
import consulo.enviroment.remoteAgent.platform.RemotePlatform;
import consulo.localize.LocalizeValue;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.runtime.ServerConnector;

/**
 * Establishes Thrift connection to the remote agent and creates runtime instance.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
class RemoteAgentServerConnector extends ServerConnector<DeploymentConfiguration> {
    private final RemoteAgentServerConfiguration myConfiguration;

    RemoteAgentServerConnector(RemoteAgentServerConfiguration configuration) {
        myConfiguration = configuration;
    }

    @Override
    public void connect(ConnectionCallback<DeploymentConfiguration> callback) {
        try {
            RemoteAgentConnection connection = new RemoteAgentConnection(myConfiguration.getHost(), myConfiguration.getPort());
            connection.connect();

            RemoteFileSystemProvider fileSystemProvider = new RemoteFileSystemProvider();
            RemoteNioFileSystem nioFileSystem = fileSystemProvider.newFileSystem(connection);

            RemotePlatform platform = new RemotePlatform(connection, nioFileSystem);

            callback.connected(new RemoteAgentServerRuntimeInstance(connection, platform, nioFileSystem));
        }
        catch (RemoteAgentException e) {
            callback.errorOccurred(LocalizeValue.localizeTODO("Failed to connect to remote agent at " + myConfiguration.getHost() + ":" + myConfiguration.getPort() + ": " + e.getMessage()));
        }
    }
}
