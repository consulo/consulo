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
package consulo.content.platform;

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A machine run configurations and SDKs can live on - the local one or a remote one reached through a connection.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public interface PlatformTarget {
    /**
     * Stable across restarts, it is what run configurations and SDKs store.
     */
    String getId();

    LocalizeValue getDisplayName();

    @Nullable Image getIcon();

    /**
     * The platform while the target is reachable, null while it is not connected.
     */
    @Nullable Platform getPlatform();

    /**
     * Reaches the target, connecting first when needed.
     */
    CompletableFuture<Platform> connect();
}
