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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-07-01
 */
public interface SwipeLayout extends Layout<LayoutConstraint> {
    static SwipeLayout create() {
        return UIInternal.get()._Layouts_swipe();
    }
    default SwipeLayout register(String id, Layout layout) {
        return register(id, () -> layout);
    }
    SwipeLayout register(String id, @RequiredUIAccess Supplier<Layout> layoutSupplier);

    /**
     * @param id of child
     * @return child layout which will be showed
     */
    Layout swipeLeftTo(String id);

    /**
     * @param id of child
     * @return child layout which will be showed
     */
    Layout swipeRightTo(String id);
}
