// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.runtime;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ServerConnectionManager {
    @Nonnull
    public static ServerConnectionManager getInstance() {
        return Application.get().getInstance(ServerConnectionManager.class);
    }

    @Nonnull
    public abstract <C extends ServerConfiguration> ServerConnection<?> getOrCreateConnection(@Nonnull RemoteServer<C> server);

    @Nullable
    public abstract <C extends ServerConfiguration> ServerConnection<?> getConnection(@Nonnull RemoteServer<C> server);

    @Nonnull
    public abstract Collection<ServerConnection<?>> getConnections();

    @Nonnull
    public <C extends ServerConfiguration> ServerConnection<?> createTemporaryConnection(@Nonnull RemoteServer<C> server) {
        throw new UnsupportedOperationException();
    }
}
