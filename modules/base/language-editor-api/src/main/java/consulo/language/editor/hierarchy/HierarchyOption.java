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
package consulo.language.editor.hierarchy;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * A toggle a model offers on its own toolbar - hiding rows a hierarchy considers uninteresting, say. The
 * platform owns the toggle and its persistence; the model only reads the answer back through
 * {@link HierarchyRequest#isEnabled(HierarchyOption)} while it builds.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public final class HierarchyOption {
    private final String myId;
    private final LocalizeValue myText;
    private final @Nullable Image myIcon;
    private final boolean myDefaultValue;

    public HierarchyOption(String id, LocalizeValue text, @Nullable Image icon, boolean defaultValue) {
        myId = id;
        myText = text;
        myIcon = icon;
        myDefaultValue = defaultValue;
    }

    public String getId() {
        return myId;
    }

    public LocalizeValue getText() {
        return myText;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public boolean getDefaultValue() {
        return myDefaultValue;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HierarchyOption other && myId.equals(other.myId);
    }

    @Override
    public int hashCode() {
        return myId.hashCode();
    }

    @Override
    public String toString() {
        return myId;
    }
}
