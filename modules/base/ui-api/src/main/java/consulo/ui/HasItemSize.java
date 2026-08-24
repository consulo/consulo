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
package consulo.ui;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Row sizing for the collection components. Sizing sits on the component rather than the render,
 * because a row has a height whichever kind of render draws it.
 * <p/>
 * Height only - the column, the popup or the tree indent decides the width, so a width here would
 * be silently dropped.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public interface HasItemSize<Item> extends Component {
    /**
     * @param getter {@code null} restores platform sizing. A constant makes every row the same
     *               height; a per item computation gives variable rows, which the backend then gets
     *               told outright instead of having to measure each row.
     */
    void setItemHeightGetter(@Nullable Function<Item, Length> getter);

    /**
     * How many rows are on screen at once. The rest of the items are still there, a scroll away - this bounds how
     * much room the component asks for, not how much it holds.
     *
     * @param count {@code 0} or less restores platform sizing, where the component asks for every row it holds
     */
    default void setVisibleRowCount(int count) {
        // unwarranted action
    }
}
