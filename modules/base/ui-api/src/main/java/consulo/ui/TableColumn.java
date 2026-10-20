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

import consulo.localize.LocalizeValue;
import consulo.ui.font.Font;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.UnaryOperator;

/**
 * @author VISTALL
 * @since 2020-09-15
 */
public interface TableColumn<Item, Value> {
    TableColumn<Item, Value> setHeader(LocalizeValue header);

    TableColumn<Item, Value> setRenderer(TextItemRenderer<Value> renderer);

    TableColumn<Item, Value> setRenderer(ComponentItemRenderer<Value> renderer);

    /**
     * Renderer which is handed the row as well as the value, for a column whose look follows the kind of row
     * it sits in. Mutually exclusive with the other renders - the last one set wins.
     */
    TableColumn<Item, Value> setRenderer(TableItemRenderer<Item, Value> renderer);

    TableColumn<Item, Value> setWidth(int pixels);

    TableColumn<Item, Value> setResizable(boolean resizable);

    TableColumn<Item, Value> setHorizontalAlignment(HorizontalAlignment alignment);

    /**
     * Derives the font of the cells of this column from the font the table draws with - tabular numbers for a
     * column of dates, for instance. The result is kept and the builder asked again only once the table font
     * changes, on a theme or scale switch. A font set by the renderer itself still wins.
     *
     * @param builder {@code null} (the default) leaves the cells on the table font
     */
    TableColumn<Item, Value> setFontBuilder(@Nullable UnaryOperator<Font> builder);

    /**
     * @param comparator over the column value, {@code null} makes the column unsortable
     */
    TableColumn<Item, Value> setSortable(@Nullable Comparator<Value> comparator);

    /**
     * @param editor {@code null} (the default) leaves the column read only
     */
    TableColumn<Item, Value> setEditor(@Nullable TableItemEditor<Item, Value> editor);
}
