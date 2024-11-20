// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime;

import consulo.application.ApplicationManager;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServerListener;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConnectionManagerImpl extends ServerConnectionManager {

    private final Map<RemoteServer<?>, ServerConnection<?>> myConnections = new ConcurrentHashMap<>();
    private final ServerConnectionEventDispatcher myEventDispatcher = new ServerConnectionEventDispatcher();

    @Override
    public @NotNull <C extends ServerConfiguration> ServerConnection<?> getOrCreateConnection(@NotNull RemoteServer<C> server) {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        ServerConnection<?> connection = myConnections.get(server);
        if (connection == null) {
            connection = doCreateConnection(server, this);
            myConnections.put(server, connection);
            myEventDispatcher.fireConnectionCreated(connection);
        }
        return connection;
    }

    @Override
    public @NotNull <C extends ServerConfiguration> ServerConnection<?> createTemporaryConnection(@NotNull RemoteServer<C> server) {
        return doCreateConnection(server, null);
    }

    private <C extends ServerConfiguration> ServerConnection<?> doCreateConnection(@NotNull RemoteServer<C> server,
                                                                                   ServerConnectionManagerImpl manager) {
        ServerTaskExecutorImpl executor = new ServerTaskExecutorImpl();
        return new ServerConnectionImpl<>(server, server.getType().createConnector(server, executor), manager, getEventDispatcher());
    }

    @Override
    public @Nullable <C extends ServerConfiguration> ServerConnection<?> getConnection(@NotNull RemoteServer<C> server) {
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
    public @NotNull Collection<ServerConnection<?>> getConnections() {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        return Collections.unmodifiableCollection(myConnections.values());
    }

    public static class DisconnectFromRemovedServer implements RemoteServerListener {
        @Override
        public void serverRemoved(@NotNull RemoteServer<?> server) {
            ServerConnectionManagerImpl impl = (ServerConnectionManagerImpl) ServerConnectionManager.getInstance();
            ServerConnection<?> connection = impl.getConnection(server);
            if (connection != null) {
                connection.disconnect();
            }
        }

        @Override
        public void serverAdded(@NotNull RemoteServer<?> server) {
            //
        }
    }
}
