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
package consulo.externalSystem.impl.internal.service.thrift;

import consulo.externalSystem.impl.internal.service.ExternalSystemCommunicationManager;
import consulo.externalSystem.impl.internal.service.RemoteExternalSystemFacade;
import consulo.externalSystem.impl.internal.service.remote.wrapper.ExternalSystemFacadeWrapper;
import consulo.externalSystem.model.ProjectSystemId;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Communication manager that connects to external system processes via Thrift RPC.
 * <p>
 * Each acquired facade represents a connection to an external JVM process
 * that runs a Thrift server implementing {@code ExternalSystemFacadeService}.
 *
 * @author VISTALL
 * @since 2026-03-20
 */
public class ThriftExternalSystemCommunicationManager implements ExternalSystemCommunicationManager {

    private final Map<String, ThriftExternalSystemFacadeImpl> myFacades = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public RemoteExternalSystemFacade acquire(String id, ProjectSystemId externalSystemId) throws Exception {
        ThriftExternalSystemFacadeImpl existing = myFacades.get(id);
        if (existing != null && existing.isConnected()) {
            return existing;
        }

        // TODO launch external JVM process with Thrift server,
        //  read port from process stdout, then connect.
        //  For now, this manager is a placeholder for the wiring.
        //  Actual process launching will be implemented per-external-system
        //  (e.g. Gradle plugin provides classpath and main class).
        throw new UnsupportedOperationException(
            "ThriftExternalSystemCommunicationManager: process launching not yet implemented for " + externalSystemId
        );
    }

    /**
     * Acquires a facade by connecting to an already-running Thrift server at the given host/port.
     * This is used when the external process has already been launched and its port is known.
     */
    public RemoteExternalSystemFacade acquire(String id, String host, int port) {
        ThriftExternalSystemFacadeImpl existing = myFacades.get(id);
        if (existing != null && existing.isConnected()) {
            return existing;
        }

        ThriftExternalSystemFacadeImpl facade = new ThriftExternalSystemFacadeImpl(host, port);
        myFacades.put(id, facade);
        return facade;
    }

    @Override
    public void release(String id, ProjectSystemId externalSystemId) throws Exception {
        ThriftExternalSystemFacadeImpl facade = myFacades.remove(id);
        if (facade != null) {
            facade.close();
        }
    }

    @Override
    public boolean isAlive(RemoteExternalSystemFacade facade) {
        RemoteExternalSystemFacade toCheck = facade;
        if (facade instanceof ExternalSystemFacadeWrapper) {
            toCheck = ((ExternalSystemFacadeWrapper) facade).getDelegate();
        }
        if (toCheck instanceof ThriftExternalSystemFacadeImpl) {
            return ((ThriftExternalSystemFacadeImpl) toCheck).isConnected();
        }
        return false;
    }

    @Override
    public void clear() {
        for (ThriftExternalSystemFacadeImpl facade : myFacades.values()) {
            facade.close();
        }
        myFacades.clear();
    }
}
