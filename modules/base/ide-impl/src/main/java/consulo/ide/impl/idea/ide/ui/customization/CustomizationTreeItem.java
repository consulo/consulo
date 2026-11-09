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
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One child of a {@link KeymapGroupImpl}, as a value of the customization tree. The same action id or a separator can
 * appear several times in one group, so a child is told apart from its equal siblings by how many of them precede it.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
public final class CustomizationTreeItem {
    private final KeymapGroupImpl myParent;
    private final Object myComponent;
    private final int myOccurrence;

    private CustomizationTreeItem(KeymapGroupImpl parent, Object component, int occurrence) {
        myParent = parent;
        myComponent = component;
        myOccurrence = occurrence;
    }

    public static List<CustomizationTreeItem> children(KeymapGroupImpl group) {
        List<Object> children = group.getChildren();
        List<CustomizationTreeItem> result = new ArrayList<>(children.size());
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            int occurrence = 0;
            for (int j = 0; j < i; j++) {
                if (isSame(children.get(j), child)) {
                    occurrence++;
                }
            }
            result.add(new CustomizationTreeItem(group, child, occurrence));
        }
        return result;
    }

    public static @Nullable CustomizationTreeItem at(KeymapGroupImpl group, int index) {
        List<CustomizationTreeItem> children = children(group);
        return index >= 0 && index < children.size() ? children.get(index) : null;
    }

    public KeymapGroupImpl getParent() {
        return myParent;
    }

    public Object getComponent() {
        return myComponent;
    }

    public @Nullable KeymapGroupImpl getGroup() {
        return myComponent instanceof KeymapGroupImpl group ? group : null;
    }

    public @Nullable String getActionId() {
        if (myComponent instanceof String actionId) {
            return actionId;
        }
        if (myComponent instanceof KeymapGroupImpl group) {
            return group.getId();
        }
        return null;
    }

    public boolean isTopLevel() {
        return myParent.getParent() == null;
    }

    public int getIndex() {
        List<Object> children = myParent.getChildren();
        int occurrence = 0;
        for (int i = 0; i < children.size(); i++) {
            if (isSame(children.get(i), myComponent)) {
                if (occurrence == myOccurrence) {
                    return i;
                }
                occurrence++;
            }
        }
        return -1;
    }

    public List<String> getGroupPath() {
        return CustomizationUtil.getGroupPath(myParent);
    }

    public boolean isUnder(KeymapGroupImpl group) {
        KeymapGroupImpl current = myParent;
        while (current != null) {
            if (current == group) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean isSame(Object left, Object right) {
        if (left instanceof KeymapGroupImpl || right instanceof KeymapGroupImpl) {
            return left == right;
        }
        return left.equals(right);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof CustomizationTreeItem that
            && myParent == that.myParent
            && myOccurrence == that.myOccurrence
            && isSame(myComponent, that.myComponent);
    }

    @Override
    public int hashCode() {
        int componentHash = myComponent instanceof KeymapGroupImpl ? System.identityHashCode(myComponent) : myComponent.hashCode();
        return Objects.hash(System.identityHashCode(myParent), componentHash, myOccurrence);
    }

    @Override
    public String toString() {
        return String.valueOf(myComponent);
    }
}
