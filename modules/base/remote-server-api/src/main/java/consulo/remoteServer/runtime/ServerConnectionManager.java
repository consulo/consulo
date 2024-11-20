// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.runtime;

import consulo.application.Application;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class ServerConnectionManager {
    @NotNull
    public static ServerConnectionManager getInstance() {
        return Application.get().getInstance(ServerConnectionManager.class);
    }

    @NotNull
    public abstract <C extends ServerConfiguration> ServerConnection<?> getOrCreateConnection(@NotNull RemoteServer<C> server);

    @Nullable
    public abstract <C extends ServerConfiguration> ServerConnection<?> getConnection(@NotNull RemoteServer<C> server);

    @NotNull
    public abstract Collection<ServerConnection<?>> getConnections();

    @NotNull
    public <C extends ServerConfiguration> ServerConnection<?> createTemporaryConnection(@NotNull RemoteServer<C> server) {
        throw new UnsupportedOperationException();
    }
}
