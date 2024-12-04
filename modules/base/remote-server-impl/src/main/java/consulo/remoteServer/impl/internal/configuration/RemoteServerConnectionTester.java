// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.impl.internal.configuration;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.remoteServer.CloudBundle;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.deployment.ServerRuntimeInstance;
import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicReference;

public class RemoteServerConnectionTester {

    public interface Callback {
        void connectionTested(boolean wasConnected, @Nonnull String hadStatusText);
    }

    private final RemoteServer<?> myServer;

    public RemoteServerConnectionTester(@Nonnull RemoteServer<?> server) {
        myServer = server;
    }

    public void testConnection(@Nonnull Callback callback) {
        final ServerConnection connection = ServerConnectionManager.getInstance().createTemporaryConnection(myServer);
        final AtomicReference<Boolean> connectedRef = new AtomicReference<>(null);
        final Semaphore semaphore = new Semaphore();
        semaphore.down();

        //noinspection unchecked
        connection.connectIfNeeded(new ServerConnector.ConnectionCallback() {

            @Override
            public void connected(@Nonnull ServerRuntimeInstance serverRuntimeInstance) {
                connectedRef.set(true);
                semaphore.up();
                connection.disconnect();
            }

            @Override
            public void errorOccurred(@Nonnull String errorMessage) {
                connectedRef.set(false);
                semaphore.up();
            }
        });

        new Task.Backgroundable(null, CloudBundle.message("task.title.connecting"), true) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                while (!indicator.isCanceled()) {
                    if (semaphore.waitFor(500)) {
                        break;
                    }
                }
                final Boolean connected = connectedRef.get();
                if (connected == null) {
                    return;
                }
                callback.connectionTested(connected, connection.getStatusText());
            }
        }.queue();
    }
}
