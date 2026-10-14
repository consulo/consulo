/*
 * Copyright 2013-2020 consulo.io
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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 2020-09-15
 */
public interface TableColumn<Item, Value> {
    TableColumn<Item, Value> setHeader(LocalizeValue header);

    @SuppressWarnings("deprecation")
    default TableColumn<Item, Value> setRenderer(TextItemRender<Value> renderer) {
        return setRender(renderer);
    }

    @Deprecated
    @DeprecationInfo("Use #setRenderer")
    TableColumn<Item, Value> setRender(TextItemRender<Value> render);

    @SuppressWarnings("deprecation")
    default TableColumn<Item, Value> setRenderer(ComponentItemRender<Value> renderer) {
        return setRender(renderer);
    }

    @Deprecated
    @DeprecationInfo("Use #setRenderer")
    TableColumn<Item, Value> setRender(ComponentItemRender<Value> render);

    /**
     * Renderer which is handed the row as well as the value, for a column whose look follows the kind of row
     * it sits in. Mutually exclusive with the other renders - the last one set wins.
     */
    @SuppressWarnings("deprecation")
    default TableColumn<Item, Value> setRenderer(TableItemRender<Item, Value> renderer) {
        return setRender(renderer);
    }

    @Deprecated
    @DeprecationInfo("Use #setRenderer")
    TableColumn<Item, Value> setRender(TableItemRender<Item, Value> render);

    TableColumn<Item, Value> setWidth(int pixels);

    TableColumn<Item, Value> setResizable(boolean resizable);

    TableColumn<Item, Value> setHorizontalAlignment(HorizontalAlignment alignment);

    /**
     * @param comparator over the column value, {@code null} makes the column unsortable
     */
    TableColumn<Item, Value> setSortable(@Nullable Comparator<Value> comparator);

    /**
     * @param editor {@code null} (the default) leaves the column read only
     */
    TableColumn<Item, Value> setEditor(@Nullable TableItemEditor<Item, Value> editor);
}
