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

import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.platformAware.LaunchedRemoteAgent;
import consulo.remoteServer.platformAware.RemoteAgentLauncher;
import consulo.remoteServer.runtime.ServerConnector;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Launches the agent through the plugin, then connects like a configured agent. The launched agent is the lease of the runtime.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
class LaunchedRemoteAgentServerConnector extends ServerConnector<DeploymentConfiguration> {
    private static final Logger LOG = Logger.getInstance(LaunchedRemoteAgentServerConnector.class);

    private final String myPlatformId;
    private final RemoteAgentLauncher myLauncher;
    private final Executor myExecutor;

    LaunchedRemoteAgentServerConnector(String platformId, RemoteAgentLauncher launcher, Executor executor) {
        myPlatformId = platformId;
        myLauncher = launcher;
        myExecutor = executor;
    }

    @Override
    public void connect(ConnectionCallback<DeploymentConfiguration> callback) {
        CompletableFuture<LaunchedRemoteAgent> launch;
        try {
            launch = myLauncher.launch();
        }
        catch (Throwable e) {
            launch = CompletableFuture.failedFuture(e);
        }
        launch.whenCompleteAsync((agent, error) -> {
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                LOG.warn("Cannot launch the remote agent", cause);
                callback.errorOccurred(LocalizeValue.localizeTODO(String.valueOf(cause.getMessage())));
                return;
            }
            RemoteAgentServerConnector.connect(myPlatformId,
                agent.getHost(),
                agent.getPort(),
                agent.isUseTls(),
                agent.getCertificateFingerprint(),
                agent.getToken(),
                agent,
                myExecutor,
                callback);
        }, myExecutor);
    }
}
