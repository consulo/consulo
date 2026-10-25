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
package consulo.review.create;

import consulo.ui.Component;
import consulo.ui.Separator;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The components of the review creation screen one under another, each padded unless asked not to be, with a separator
 * line where one was added. The first component stretched in height takes the space left; a uni layout has no
 * weights, so any later stretched component keeps its height.
 * <p>
 * Use a static method to construct
 */
public final class CodeReviewCreateReviewLayoutBuilder {
    private static final Space BASE_GAP = Space.LARGE;

    private record Entry(Component component, boolean stretchY, boolean separatorAbove) {
    }

    private final List<Entry> myEntries = new ArrayList<>();
    private boolean myAddSeparator;

    private CodeReviewCreateReviewLayoutBuilder() {
    }

    public static CodeReviewCreateReviewLayoutBuilder create() {
        return new CodeReviewCreateReviewLayoutBuilder();
    }

    @RequiredUIAccess
    public CodeReviewCreateReviewLayoutBuilder addComponent(Component component) {
        return addComponent(component, false, false);
    }

    @RequiredUIAccess
    public CodeReviewCreateReviewLayoutBuilder addComponent(Component component, boolean stretchY, boolean withoutBorder) {
        if (!withoutBorder && component instanceof Layout<?> layout) {
            layout.paddingBuilder().allSet(BASE_GAP).apply();
        }
        myEntries.add(new Entry(component, stretchY, myAddSeparator));
        myAddSeparator = false;
        return this;
    }

    public CodeReviewCreateReviewLayoutBuilder addSeparator() {
        myAddSeparator = true;
        return this;
    }

    @RequiredUIAccess
    public Component build() {
        int stretched = -1;
        for (int i = 0; i < myEntries.size(); i++) {
            if (myEntries.get(i).stretchY()) {
                stretched = i;
                break;
            }
        }

        if (stretched < 0) {
            return stack(myEntries);
        }

        DockLayout root = DockLayout.create(Space.NONE);
        List<Entry> above = myEntries.subList(0, stretched);
        if (!above.isEmpty()) {
            root.top(stack(above));
        }

        Entry center = myEntries.get(stretched);
        if (center.separatorAbove()) {
            DockLayout withSeparator = DockLayout.create(Space.NONE);
            withSeparator.top(Separator.horizontal());
            withSeparator.center(center.component());
            root.center(withSeparator);
        }
        else {
            root.center(center.component());
        }

        List<Entry> below = myEntries.subList(stretched + 1, myEntries.size());
        if (!below.isEmpty()) {
            root.bottom(stack(below));
        }
        return root;
    }

    @RequiredUIAccess
    private static Component stack(List<Entry> entries) {
        VerticalLayout layout = VerticalLayout.create(Space.NONE);
        for (Entry entry : entries) {
            if (entry.separatorAbove()) {
                layout.add(Separator.horizontal());
            }
            layout.add(entry.component());
        }
        return layout;
    }
}
