// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.runtime;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ServerConnectionManager {
    
    public static ServerConnectionManager getInstance() {
        return Application.get().getInstance(ServerConnectionManager.class);
    }

    
    public abstract <C extends ServerConfiguration> ServerConnection<?> getOrCreateConnection(RemoteServer<C> server);

    public abstract @Nullable <C extends ServerConfiguration> ServerConnection<?> getConnection(RemoteServer<C> server);

    
    public abstract Collection<ServerConnection<?>> getConnections();

    
    public <C extends ServerConfiguration> ServerConnection<?> createTemporaryConnection(RemoteServer<C> server) {
        throw new UnsupportedOperationException();
    }
}
