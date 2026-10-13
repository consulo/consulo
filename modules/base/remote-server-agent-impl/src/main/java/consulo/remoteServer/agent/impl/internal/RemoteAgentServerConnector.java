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
package consulo.remoteServer.agent.impl.internal;

import consulo.remoteServer.agent.impl.internal.nio.RemoteFileSystemProvider;
import consulo.remoteServer.agent.impl.internal.nio.RemoteNioFileSystem;
import consulo.remoteServer.agent.impl.internal.platform.RemotePlatform;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.virtualFileSystem.VirtualFileManager;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.Executor;

/**
 * Connects to the remote agent over gRPC and creates the runtime instance.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
class RemoteAgentServerConnector extends ServerConnector<DeploymentConfiguration> {
    private static final Logger LOG = Logger.getInstance(RemoteAgentServerConnector.class);

    private final RemoteAgentServerConfiguration myConfiguration;
    private final RemoteAgentCredentials myCredentials;
    private final Executor myExecutor;

    RemoteAgentServerConnector(RemoteAgentServerConfiguration configuration, RemoteAgentCredentials credentials, Executor executor) {
        myConfiguration = configuration;
        myCredentials = credentials;
        myExecutor = executor;
    }

    @Override
    public void connect(ConnectionCallback<DeploymentConfiguration> callback) {
        myCredentials.getTokenAsync(myConfiguration).whenCompleteAsync((token, error) -> {
            if (error != null) {
                LOG.warn("Cannot read the remote agent token", error);
            }
            connect(callback, error == null ? token : null);
        }, myExecutor);
    }

    private void connect(ConnectionCallback<DeploymentConfiguration> callback, @Nullable String token) {
        RemoteAgentConnection connection = new RemoteAgentConnection(
            myConfiguration.getHost(),
            myConfiguration.getPort(),
            myConfiguration.isUseTls(),
            myConfiguration.getCertificateFingerprint(),
            token,
            myExecutor
        );
        try {
            connection.connect();

            RemoteFileSystemProvider fileSystemProvider = new RemoteFileSystemProvider();
            RemoteNioFileSystem nioFileSystem = fileSystemProvider.newFileSystem(connection);

            RemotePlatform platform = new RemotePlatform(connection, nioFileSystem);
            for (Path root : nioFileSystem.getRootDirectories()) {
                VirtualFileManager.getInstance().findFileByNioPath(root);
            }

            callback.connected(new RemoteAgentServerRuntimeInstance(connection, platform, nioFileSystem));
        }
        catch (RemoteAgentException e) {
            connection.close();
            LOG.warn(e.getMessage(), e);
            callback.errorOccurred(LocalizeValue.localizeTODO(e.getMessage()));
        }
        catch (Throwable e) {
            connection.close();
            LOG.error(e);
            callback.errorOccurred(LocalizeValue.localizeTODO(String.valueOf(e.getMessage())));
        }
    }
}
