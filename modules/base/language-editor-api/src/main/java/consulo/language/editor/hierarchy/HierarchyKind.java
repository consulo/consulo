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
import org.jspecify.annotations.Nullable;

/**
 * A family of hierarchy - types, callers, and whatever else a language needs. A kind carries what the
 * platform needs to wire a session up: where its actions are placed, which group its context menu draws
 * from, and how its occurrence navigation is worded. A model states its kind and nothing else about
 * presentation.
 * <p>
 * The kinds the platform ships are in {@link StandardHierarchyKinds}. Nothing stops a language declaring
 * its own, so long as the action places and groups it names exist.
 * <p>
 * Identity is the {@link #getId() id}, which is stable and never localized.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public final class HierarchyKind {
    private final String myId;
    private final String myActionPlace;
    private final String myPopupPlace;
    private final String myPopupActionGroupId;
    private final @Nullable String myShortcutActionId;
    private final boolean myDragAndDropEnabled;
    private final LocalizeValue myNextOccurrenceText;
    private final LocalizeValue myPreviousOccurrenceText;

    private HierarchyKind(Builder builder) {
        myId = builder.myId;
        myActionPlace = builder.myActionPlace;
        myPopupPlace = builder.myPopupPlace;
        myPopupActionGroupId = builder.myPopupActionGroupId;
        myShortcutActionId = builder.myShortcutActionId;
        myDragAndDropEnabled = builder.myDragAndDropEnabled;
        myNextOccurrenceText = builder.myNextOccurrenceText;
        myPreviousOccurrenceText = builder.myPreviousOccurrenceText;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return myId;
    }

    public String getActionPlace() {
        return myActionPlace;
    }

    public String getPopupPlace() {
        return myPopupPlace;
    }

    public String getPopupActionGroupId() {
        return myPopupActionGroupId;
    }

    /**
     * Action whose shortcut also reaches this hierarchy from inside its own tree, or null where it has none.
     */
    public @Nullable String getShortcutActionId() {
        return myShortcutActionId;
    }

    public boolean isDragAndDropEnabled() {
        return myDragAndDropEnabled;
    }

    public LocalizeValue getNextOccurrenceText() {
        return myNextOccurrenceText;
    }

    public LocalizeValue getPreviousOccurrenceText() {
        return myPreviousOccurrenceText;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o
            || o instanceof HierarchyKind other && myId.equals(other.myId);
    }

    @Override
    public int hashCode() {
        return myId.hashCode();
    }

    @Override
    public String toString() {
        return myId;
    }

    public static final class Builder {
        private final String myId;
        private String myActionPlace = "";
        private String myPopupPlace = "";
        private String myPopupActionGroupId = "";
        private @Nullable String myShortcutActionId;
        private boolean myDragAndDropEnabled;
        private LocalizeValue myNextOccurrenceText = LocalizeValue.empty();
        private LocalizeValue myPreviousOccurrenceText = LocalizeValue.empty();

        private Builder(String id) {
            myId = id;
        }

        public Builder actionPlace(String actionPlace) {
            myActionPlace = actionPlace;
            return this;
        }

        public Builder popupPlace(String popupPlace) {
            myPopupPlace = popupPlace;
            return this;
        }

        public Builder popupActionGroupId(String popupActionGroupId) {
            myPopupActionGroupId = popupActionGroupId;
            return this;
        }

        public Builder shortcutActionId(@Nullable String shortcutActionId) {
            myShortcutActionId = shortcutActionId;
            return this;
        }

        public Builder dragAndDrop(boolean dragAndDropEnabled) {
            myDragAndDropEnabled = dragAndDropEnabled;
            return this;
        }

        public Builder occurrenceText(LocalizeValue next, LocalizeValue previous) {
            myNextOccurrenceText = next;
            myPreviousOccurrenceText = previous;
            return this;
        }

        public HierarchyKind build() {
            return new HierarchyKind(this);
        }
    }
}
