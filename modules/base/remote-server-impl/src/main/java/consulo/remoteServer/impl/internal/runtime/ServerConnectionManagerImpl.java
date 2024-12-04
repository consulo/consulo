// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServerListener;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServiceImpl
@Singleton
public class ServerConnectionManagerImpl extends ServerConnectionManager {

    private final Map<RemoteServer<?>, ServerConnection<?>> myConnections = new ConcurrentHashMap<>();
    private final ServerConnectionEventDispatcher myEventDispatcher = new ServerConnectionEventDispatcher();

    @Override
    public @Nonnull <C extends ServerConfiguration> ServerConnection<?> getOrCreateConnection(@Nonnull RemoteServer<C> server) {
        UIAccess.assertIsUIThread();
        ServerConnection<?> connection = myConnections.get(server);
        if (connection == null) {
            connection = doCreateConnection(server, this);
            myConnections.put(server, connection);
            myEventDispatcher.fireConnectionCreated(connection);
        }
        return connection;
    }

    @Override
    public @Nonnull <C extends ServerConfiguration> ServerConnection<?> createTemporaryConnection(@Nonnull RemoteServer<C> server) {
        return doCreateConnection(server, null);
    }

    private <C extends ServerConfiguration> ServerConnection<?> doCreateConnection(@Nonnull RemoteServer<C> server,
                                                                                   ServerConnectionManagerImpl manager) {
        ServerTaskExecutorImpl executor = new ServerTaskExecutorImpl();
        return new ServerConnectionImpl<>(server, server.getType().createConnector(server, executor), manager, getEventDispatcher());
    }

    @Override
    public @Nullable <C extends ServerConfiguration> ServerConnection<?> getConnection(@Nonnull RemoteServer<C> server) {
        return myConnections.get(server);
    }

    void removeConnection(RemoteServer<?> server) {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        myConnections.remove(server);
    }

    public ServerConnectionEventDispatcher getEventDispatcher() {
        return myEventDispatcher;
    }

    @Override
    public @Nonnull Collection<ServerConnection<?>> getConnections() {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        return Collections.unmodifiableCollection(myConnections.values());
    }

    public static class DisconnectFromRemovedServer implements RemoteServerListener {
        @Override
        public void serverRemoved(@Nonnull RemoteServer<?> server) {
            ServerConnectionManagerImpl impl = (ServerConnectionManagerImpl) ServerConnectionManager.getInstance();
            ServerConnection<?> connection = impl.getConnection(server);
            if (connection != null) {
                connection.disconnect();
            }
        }

        @Override
        public void serverAdded(@Nonnull RemoteServer<?> server) {
            //
        }
    }
}
