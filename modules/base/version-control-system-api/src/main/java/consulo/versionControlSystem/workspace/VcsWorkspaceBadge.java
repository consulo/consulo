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
package consulo.versionControlSystem.workspace;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * An icon and/or a text shown next to a workspace's name or revision, e.g. a lock or an open review.
 */
public final class VcsWorkspaceBadge {
    private final @Nullable Image myIcon;
    private final LocalizeValue myText;

    public VcsWorkspaceBadge(@Nullable Image icon, LocalizeValue text) {
        myIcon = icon;
        myText = text;
    }

    public static VcsWorkspaceBadge icon(Image icon) {
        return new VcsWorkspaceBadge(icon, LocalizeValue.empty());
    }

    public static VcsWorkspaceBadge text(LocalizeValue text) {
        return new VcsWorkspaceBadge(null, text);
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public LocalizeValue getText() {
        return myText;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof VcsWorkspaceBadge badge && Objects.equals(myIcon, badge.myIcon) && myText.equals(badge.myText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myIcon, myText);
    }
}
