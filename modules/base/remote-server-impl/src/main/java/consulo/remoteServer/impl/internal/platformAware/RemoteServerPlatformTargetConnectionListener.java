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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.platform.PlatformTargetListener;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.platformAware.PlatformAwareServerType;
import consulo.remoteServer.runtime.ConnectionStatus;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionListener;
import consulo.remoteServer.runtime.ServerConnectionManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Tells the {@link PlatformTargetListener}s when the connection of a platform aware server comes and goes.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@TopicImpl(ComponentScope.APPLICATION)
public class RemoteServerPlatformTargetConnectionListener implements ServerConnectionListener {
    private final Application myApplication;
    private final Provider<ServerConnectionManager> myConnectionManager;

    @Inject
    public RemoteServerPlatformTargetConnectionListener(Application application, Provider<ServerConnectionManager> connectionManager) {
        myApplication = application;
        myConnectionManager = connectionManager;
    }

    @Override
    public void onConnectionCreated(ServerConnection<?> connection) {
    }

    @Override
    public void onConnectionStatusChanged(ServerConnection<?> connection) {
        RemoteServer<?> server = connection.getServer();
        if (!(server.getType() instanceof PlatformAwareServerType<?>)) {
            return;
        }
        RemoteServerPlatformTarget target = new RemoteServerPlatformTarget(server, myConnectionManager.get());
        PlatformTargetListener publisher = myApplication.getMessageBus().syncPublisher(PlatformTargetListener.class);
        if (connection.getStatus() == ConnectionStatus.CONNECTED) {
            publisher.targetConnected(target);
        }
        else {
            publisher.targetDisconnected(target);
        }
    }

    @Override
    public void onDeploymentsChanged(ServerConnection<?> connection) {
    }
}
