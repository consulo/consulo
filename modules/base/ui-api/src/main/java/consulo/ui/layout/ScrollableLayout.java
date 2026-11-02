/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * @author VISTALL
 * @since 2018-07-03
 */
public interface ScrollableLayout extends Layout<LayoutConstraint> {
    static ScrollableLayout create(Component component) {
        return create(component, ScrollableLayoutOptions.builder().build());
    }

    static ScrollableLayout create(Component component, ScrollableLayoutOptions options) {
        return UIInternal.get()._ScrollLayout_create(component, options);
    }

    /**
     * How many children of a {@link VerticalLayout} content are on screen at once. The rest are still there, a scroll
     * away - this bounds how much room the layout asks for, not how much it holds. A content with fewer children asks
     * for all of them.
     *
     * @param count {@code 0} or less restores the default sizing, where the layout asks for the whole content
     */
    @RequiredUIAccess
    default void setVisibleRowCount(int count) {
    }
}
