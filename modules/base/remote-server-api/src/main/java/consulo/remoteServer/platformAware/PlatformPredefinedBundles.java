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
import consulo.application.platform.PlatformTarget;
import consulo.platform.Platform;

import java.util.concurrent.CompletableFuture;

/**
 * Detects and adds the bundles (SDKs) of a platform target once it is connected.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PlatformPredefinedBundles {
    /**
     * Called when {@code target} got connected. Runs in the background - detecting bundles runs their tools on the platform.
     *
     * @return completes when done, with true when bundles were detected (for {@link PredefinedBundlesMode#ONCE} the
     *         caller switches the mode off then)
     */
    CompletableFuture<Boolean> targetConnected(PlatformTarget target, Platform platform, PredefinedBundlesMode mode);
}
