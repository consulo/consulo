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
package consulo.sandboxPlugin.ide.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.localHistory.ActivityPresentationProvider;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class SandActivityPresentationProvider implements ActivityPresentationProvider {
    public static final String ID = "Sand";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable Image getIcon(String kind) {
        return SandEditWithActivityAction.KIND.equals(kind) ? PlatformIconGroup.vcsMerge() : null;
    }
}
