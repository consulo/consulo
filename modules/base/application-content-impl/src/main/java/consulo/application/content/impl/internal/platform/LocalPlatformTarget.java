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
package consulo.application.content.impl.internal.platform;

import consulo.content.platform.PlatformTarget;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.image.Image;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
final class LocalPlatformTarget implements PlatformTarget {
    static final String ID = "local";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectLocalize.platformTargetLocal();
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesDesktop();
    }

    @Override
    public Platform getPlatform() {
        return Platform.current();
    }

    @Override
    public CompletableFuture<Platform> connect() {
        return CompletableFuture.completedFuture(Platform.current());
    }
}
