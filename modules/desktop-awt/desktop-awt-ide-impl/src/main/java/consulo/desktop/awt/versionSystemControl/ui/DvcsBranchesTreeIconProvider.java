// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.desktop.awt.versionSystemControl.ui;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsRef;

/**
 * @author VISTALL
 */
public final class DvcsBranchesTreeIconProvider {
    private DvcsBranchesTreeIconProvider() {
    }

    public static Image forRef(DvcsRef ref, boolean current, boolean favorite, boolean selected, boolean favoriteToggleOnClick) {
        if (selected && !favorite && favoriteToggleOnClick) {
            return PlatformIconGroup.nodesNotfavoriteonhover();
        }
        if (favorite) {
            return PlatformIconGroup.nodesFavorite();
        }
        return PlatformIconGroup.vcsBranch();
    }

    public static Image forGroup() {
        return PlatformIconGroup.nodesFolder();
    }
}
