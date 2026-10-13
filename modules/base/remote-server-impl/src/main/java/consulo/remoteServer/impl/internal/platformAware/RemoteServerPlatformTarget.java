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
package consulo.remoteServer.impl.internal.platformAware;

import consulo.content.platform.PlatformTarget;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.platformAware.PlatformAwareServerRuntimeInstance;
import consulo.remoteServer.runtime.ConnectionStatus;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.deployment.ServerRuntimeInstance;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A remote server whose runtime exposes a {@link Platform}.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
final class RemoteServerPlatformTarget implements PlatformTarget {
    private final RemoteServer<?> myServer;
    private final ServerConnectionManager myConnectionManager;

    RemoteServerPlatformTarget(RemoteServer<?> server, ServerConnectionManager connectionManager) {
        myServer = server;
        myConnectionManager = connectionManager;
    }

    static String idOf(RemoteServer<?> server) {
        return "remote-server:" + server.getType().getId() + ":" + server.getName();
    }

    @Override
    public String getId() {
        return idOf(myServer);
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(myServer.getName());
    }

    @Override
    public Image getIcon() {
        return myServer.getType().getIcon();
    }

    @Override
    public @Nullable Platform getPlatform() {
        ServerConnection<?> connection = myConnectionManager.getConnection(myServer);
        if (connection == null || connection.getStatus() != ConnectionStatus.CONNECTED) {
            return null;
        }
        CompletableFuture<Platform> platform = runtime(connection, new CompletableFuture<>());
        return platform.isDone() && !platform.isCompletedExceptionally() ? platform.join() : null;
    }

    @Override
    public CompletableFuture<Platform> connect() {
        return runtime(myConnectionManager.getOrCreateConnection(myServer), new CompletableFuture<>());
    }

    private static <D extends DeploymentConfiguration> CompletableFuture<Platform> runtime(ServerConnection<D> connection,
                                                                                           CompletableFuture<Platform> result) {
        connection.connectIfNeeded(new ServerConnector.ConnectionCallback<>() {
            @Override
            public void connected(ServerRuntimeInstance<D> instance) {
                if (instance instanceof PlatformAwareServerRuntimeInstance<D> platformAware) {
                    result.complete(platformAware.getPlatform());
                }
                else {
                    result.completeExceptionally(new IllegalStateException(connection.getServer().getName() + " has no platform"));
                }
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
                result.completeExceptionally(new IllegalStateException(errorMessage.get()));
            }
        });
        return result;
    }
}
