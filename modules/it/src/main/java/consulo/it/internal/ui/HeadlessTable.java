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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Length;
import consulo.ui.Table;
import consulo.ui.TableColumn;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.TableDoubleClickEvent;
import consulo.ui.event.TableSelectEvent;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class HeadlessTable<Item> extends HeadlessComponentBase implements Table<Item> {
    private final FlatDataModel<Item> myModel;
    private final List<TableColumn<Item, ?>> myColumns = new ArrayList<>();
    private final List<Item> mySelected = new ArrayList<>();

    private boolean myAllowMultipleSelect;
    private boolean myShowHeader = true;
    private @Nullable Function<Item, ColorValue> myRowBackgroundGetter;
    private @Nullable Function<Item, String> mySpeedSearchConverter;
    private @Nullable Function<Item, Length> myItemHeightGetter;
    private int myVisibleRowCount;

    public HeadlessTable(FlatDataModel<Item> model) {
        myModel = model;

        model.addListener(event -> dropMissingFromSelection());
    }

    @Override
    public FlatDataModel<Item> getDataModel() {
        return myModel;
    }

    @Override
    public <Value> TableColumn<Item, Value> addColumn(LocalizeValue header, Function<Item, Value> valueProvider) {
        HeadlessTableColumn<Item, Value> column = new HeadlessTableColumn<>(header, valueProvider);
        myColumns.add(column);
        return column;
    }

    @Override
    public List<TableColumn<Item, ?>> getColumns() {
        return Collections.unmodifiableList(new ArrayList<>(myColumns));
    }

    @Override
    public void setAllowMultipleSelect(boolean allow) {
        myAllowMultipleSelect = allow;

        if (!allow && mySelected.size() > 1) {
            Item first = mySelected.get(0);
            mySelected.clear();
            mySelected.add(first);
            fireSelectionChanged();
        }
    }

    public boolean isAllowMultipleSelect() {
        return myAllowMultipleSelect;
    }

    @Override
    public @Nullable Item getSelectedItem() {
        return mySelected.isEmpty() ? null : mySelected.get(0);
    }

    @Override
    public List<Item> getSelectedItems() {
        return new ArrayList<>(mySelected);
    }

    @Override
    @RequiredUIAccess
    public void select(Item item) {
        if (myModel.indexOf(item) < 0) {
            return;
        }

        if (myAllowMultipleSelect) {
            if (mySelected.contains(item)) {
                return;
            }
            mySelected.add(item);
        }
        else {
            if (mySelected.size() == 1 && mySelected.get(0).equals(item)) {
                return;
            }
            mySelected.clear();
            mySelected.add(item);
        }

        fireSelectionChanged();
    }

    @Override
    @RequiredUIAccess
    public void deselectAll() {
        if (mySelected.isEmpty()) {
            return;
        }

        mySelected.clear();
        fireSelectionChanged();
    }

    @RequiredUIAccess
    public void doubleClick(Item item) {
        getListenerDispatcher(TableDoubleClickEvent.class).onEvent(new TableDoubleClickEvent(this, item));
    }

    @Override
    public void setShowHeader(boolean show) {
        myShowHeader = show;
    }

    public boolean isShowHeader() {
        return myShowHeader;
    }

    @Override
    public void setRowBackgroundGetter(@Nullable Function<Item, ColorValue> getter) {
        myRowBackgroundGetter = getter;
    }

    public @Nullable Function<Item, ColorValue> getRowBackgroundGetter() {
        return myRowBackgroundGetter;
    }

    @Override
    @RequiredUIAccess
    public void scrollTo(Item item) {
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<Item, String> converter) {
        mySpeedSearchConverter = converter;
    }

    public @Nullable Function<Item, String> getSpeedSearchConverter() {
        return mySpeedSearchConverter;
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<Item, Length> getter) {
        myItemHeightGetter = getter;
    }

    public @Nullable Function<Item, Length> getItemHeightGetter() {
        return myItemHeightGetter;
    }

    @Override
    public void setVisibleRowCount(int count) {
        myVisibleRowCount = count;
    }

    public int getVisibleRowCount() {
        return myVisibleRowCount;
    }

    private void dropMissingFromSelection() {
        if (mySelected.removeIf(item -> myModel.indexOf(item) < 0)) {
            fireSelectionChanged();
        }
    }

    private void fireSelectionChanged() {
        getListenerDispatcher(TableSelectEvent.class).onEvent(new TableSelectEvent(this, new ArrayList<>(mySelected)));
    }
}
