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

import consulo.remoteServer.platformAware.PredefinedBundlesMode;

/**
 * The configuration of a server whose connection is a platform - which bundles (SDKs) of that platform are detected.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public interface PlatformAwareServerConfiguration {
    PredefinedBundlesMode getPredefinedBundlesMode();

    void setPredefinedBundlesMode(PredefinedBundlesMode mode);

    /**
     * Whether a lost connection is connected again on its own, with growing pauses between the attempts - for a machine
     * which should always be online. A connection closed by the user is not connected again.
     */
    default boolean isReconnectAutomatically() {
        return false;
    }

    default void setReconnectAutomatically(boolean reconnectAutomatically) {
    }
}
