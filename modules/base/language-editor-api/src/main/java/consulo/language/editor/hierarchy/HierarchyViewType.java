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

import java.util.function.Function;

/**
 * One of the views a hierarchy can be shown in - supertypes, callers, and so on. The identity of a view
 * is its {@link #getId() id}, which is stable and never localized, so the maps keyed on a view survive a
 * change of display language while the session is open.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public final class HierarchyViewType {
    private final String myId;
    private final LocalizeValue myPresentableName;
    private final LocalizeValue myDescription;
    private final @Nullable Image myIcon;
    private final Function<String, LocalizeValue> myContentTitle;

    private HierarchyViewType(
        String id,
        LocalizeValue presentableName,
        LocalizeValue description,
        @Nullable Image icon,
        Function<String, LocalizeValue> contentTitle
    ) {
        myId = id;
        myPresentableName = presentableName;
        myDescription = description;
        myIcon = icon;
        myContentTitle = contentTitle;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return myId;
    }

    public LocalizeValue getPresentableName() {
        return myPresentableName;
    }

    public LocalizeValue getDescription() {
        return myDescription;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    /**
     * Title of the tab this view is shown in, for the named element the hierarchy was opened on.
     */
    public LocalizeValue contentTitle(String elementName) {
        return myContentTitle.apply(elementName);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HierarchyViewType other && myId.equals(other.myId);
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
        private LocalizeValue myPresentableName = LocalizeValue.empty();
        private LocalizeValue myDescription = LocalizeValue.empty();
        private @Nullable Image myIcon;
        private Function<String, LocalizeValue> myContentTitle = name -> LocalizeValue.of(name);

        private Builder(String id) {
            myId = id;
        }

        public Builder presentableName(LocalizeValue presentableName) {
            myPresentableName = presentableName;
            return this;
        }

        public Builder description(LocalizeValue description) {
            myDescription = description;
            return this;
        }

        public Builder icon(@Nullable Image icon) {
            myIcon = icon;
            return this;
        }

        public Builder contentTitle(Function<String, LocalizeValue> contentTitle) {
            myContentTitle = contentTitle;
            return this;
        }

        public HierarchyViewType build() {
            return new HierarchyViewType(myId, myPresentableName, myDescription, myIcon, myContentTitle);
        }
    }
}
