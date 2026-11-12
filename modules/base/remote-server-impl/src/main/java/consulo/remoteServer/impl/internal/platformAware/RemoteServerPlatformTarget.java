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

import consulo.application.Application;
import consulo.remoteServer.platformAware.PlatformPredefinedBundles;
import consulo.application.platform.PlatformTarget;
import consulo.remoteServer.platformAware.PredefinedBundlesMode;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.platformAware.PlatformAwareServerConfiguration;
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

    /**
     * The id of the server - not its name, nor its address: both may change.
     */
    static String idOf(RemoteServer<?> server) {
        return server.getId();
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
        if (!platform.isDone() || platform.isCompletedExceptionally()) {
            return null;
        }
        // the server may have been connected outside of this target (the Services view) - its bundles are detected then
        Platform connected = platform.join();
        detectBundles(connected);
        return connected;
    }

    @Override
    public CompletableFuture<Platform> connect() {
        // the bundles of the platform are known before the connect completes - a project opened right after it finds its SDK
        return runtime(myConnectionManager.getOrCreateConnection(myServer), new CompletableFuture<>())
            .thenCompose(platform -> detectBundles(platform).handle((detected, error) -> platform));
    }

    /**
     * Detects the bundles of the platform as the server configuration asks - only once per session,
     * and for {@link PredefinedBundlesMode#ONCE} never again once detected.
     */
    private CompletableFuture<?> detectBundles(Platform platform) {
        if (!(myServer.getConfiguration() instanceof PlatformAwareServerConfiguration configuration)) {
            return CompletableFuture.completedFuture(null);
        }
        PredefinedBundlesMode mode = configuration.getPredefinedBundlesMode();
        return Application.get().getInstance(PlatformPredefinedBundles.class).targetConnected(this, platform, mode)
            .whenComplete((detected, error) -> {
                if (error == null && Boolean.TRUE.equals(detected) && mode == PredefinedBundlesMode.ONCE) {
                    configuration.setPredefinedBundlesMode(PredefinedBundlesMode.NEVER);
                }
            });
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
