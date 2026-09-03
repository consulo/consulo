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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.TableDoubleClickEvent;
import consulo.ui.event.TableSelectEvent;
import consulo.ui.internal.UIInternal;
import consulo.ui.color.ColorValue;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Table without scrollable layout parent header cannot be painted.
 *
 * @author VISTALL
 * @since 2020-09-15
 */
public interface Table<Item> extends Component, HasSpeedSearch<Item>, HasItemSize<Item> {
    static <Item> Table<Item> create(FlatDataModel<Item> model) {
        return UIInternal.get()._Table_create(model);
    }

    FlatDataModel<Item> getDataModel();

    /**
     * Columns belong to the table, so a column is always an object the table itself made.
     */
    <Value> TableColumn<Item, Value> addColumn(LocalizeValue header, Function<Item, Value> valueProvider);

    List<TableColumn<Item, ?>> getColumns();

    void setSelectionMode(SelectionMode mode);

    @Nullable
    Item getSelectedItem();

    List<Item> getSelectedItems();

    @RequiredUIAccess
    void select(Item item);

    @RequiredUIAccess
    void deselectAll();

    void setShowHeader(boolean show);

    /**
     * The fill of a row as a whole, behind every column - the band an unregistered or otherwise set apart row
     * carries. {@code null}, and a {@code null} answer from the getter, leave the row on the host default.
     * A frontend with nowhere to put a row band ignores it rather than failing.
     */
    void setRowBackgroundGetter(@Nullable Function<Item, ColorValue> getter);

    @RequiredUIAccess
    void scrollTo(Item item);

    @SuppressWarnings({"unchecked", "rawtypes"})
    default Disposable addSelectListener(ComponentEventListener<Table<Item>, TableSelectEvent<Item>> listener) {
        return addListener((Class) TableSelectEvent.class, listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default Disposable addDoubleClickListener(ComponentEventListener<Table<Item>, TableDoubleClickEvent<Item>> listener) {
        return addListener((Class) TableDoubleClickEvent.class, listener);
    }
}
