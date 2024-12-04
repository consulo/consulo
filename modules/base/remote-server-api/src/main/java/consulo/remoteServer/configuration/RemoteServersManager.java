// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.remoteServer.ServerType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RemoteServersManager {
    public static RemoteServersManager getInstance() {
        return ApplicationManager.getApplication().getInstance(RemoteServersManager.class);
    }

    public abstract List<RemoteServer<?>> getServers();

    public abstract <C extends ServerConfiguration> List<RemoteServer<C>> getServers(@Nonnull ServerType<C> type);

    @Nullable
    public abstract <C extends ServerConfiguration> RemoteServer<C> findByName(@Nonnull String name, @Nonnull ServerType<C> type);

    @Nonnull
    public abstract <C extends ServerConfiguration> RemoteServer<C> createServer(@Nonnull ServerType<C> type, @Nonnull String name);

    /**
     * Creates new server with unique name derived from {@link ServerType#getPresentableName()}
     */
    @Nonnull
    public abstract <C extends ServerConfiguration> RemoteServer<C> createServer(@Nonnull ServerType<C> type);

    public abstract void addServer(RemoteServer<?> server);

    public abstract void removeServer(RemoteServer<?> server);
}
