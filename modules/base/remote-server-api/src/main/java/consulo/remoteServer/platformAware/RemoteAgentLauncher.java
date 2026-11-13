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

import java.util.concurrent.CompletableFuture;

/**
 * Starts a remote agent for one connect of a {@link RemoteAgentConnectorFactory} connector.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@FunctionalInterface
public interface RemoteAgentLauncher {
    /**
     * Called off the UI thread on every connect. The returned agent is owned by the connection from then on.
     */
    CompletableFuture<LaunchedRemoteAgent> launch();
}
