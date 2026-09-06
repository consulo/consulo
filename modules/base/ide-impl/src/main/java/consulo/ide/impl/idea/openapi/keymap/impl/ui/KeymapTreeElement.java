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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A single position in the keymap tree. The same action id may appear under several groups, so a position rather
 * than the payload itself identifies a node.
 *
 * @author VISTALL
 */
public final class KeymapTreeElement {
    private final @Nullable KeymapTreeElement myParent;
    private final Object myValue;
    private final int myIndex;

    public KeymapTreeElement(@Nullable KeymapTreeElement parent, Object value, int index) {
        myParent = parent;
        myValue = value;
        myIndex = index;
    }

    public @Nullable KeymapTreeElement getParent() {
        return myParent;
    }

    public Object getValue() {
        return myValue;
    }

    public boolean isGroup() {
        return myValue instanceof KeymapGroupImpl;
    }

    public @Nullable KeymapGroupImpl getGroup() {
        return myValue instanceof KeymapGroupImpl keymapGroup ? keymapGroup : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof KeymapTreeElement element
            && myIndex == element.myIndex
            && Objects.equals(myValue, element.myValue)
            && Objects.equals(myParent, element.myParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myParent, myValue, myIndex);
    }

    @Override
    public String toString() {
        return String.valueOf(myValue);
    }
}
