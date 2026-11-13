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
package consulo.remoteServer.platformAware;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.remoteServer.runtime.ServerConnector;

/**
 * Connects a {@link PlatformAwareServerType} to a remote agent the plugin launches itself, giving the same
 * {@link PlatformAwareServerRuntimeInstance} (remote platform, file system) as a configured remote agent.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface RemoteAgentConnectorFactory {
    /**
     * @param platformId identifies the machine for good - the id of the server ({@code RemoteServer#getId()}), not its address
     * @param launcher   starts the agent on each connect; the launched agent is closed when the platform disconnects
     */
    ServerConnector<?> createConnector(String platformId, RemoteAgentLauncher launcher);
}
