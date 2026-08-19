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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.SelectionMode;
import consulo.ui.Table;
import consulo.ui.TableColumn;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TableDoubleClickEvent;
import consulo.ui.event.TableSelectEvent;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.speedSearch.TableViewSpeedSearch;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * @author VISTALL
 * @since 2020-09-15
 */
class DesktopTableImpl<Item> extends SwingComponentDelegate<TableView<Item>> implements Table<Item> {
    class MyTableView extends TableView<Item> implements FromSwingComponentWrapper {
        MyTableView(DesktopTableModel<Item> model) {
            super(model);
        }

        @Override
        public Component toUIComponent() {
            return DesktopTableImpl.this;
        }
    }

    private final FlatDataModel<Item> myModel;
    private final List<DesktopTableColumnImpl<Item, ?>> myColumns = new ArrayList<>();

    private SelectionMode mySelectionMode = SelectionMode.SINGLE;
    private boolean myShowHeader = true;
    private @Nullable Function<Item, String> mySpeedSearchConverter;
    private @Nullable ToIntFunction<Item> myItemHeightGetter;

    public DesktopTableImpl(FlatDataModel<Item> model) {
        myModel = model;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected TableView<Item> createComponent() {
        DesktopTableModel<Item> tableModel = new DesktopTableModel<>(myModel);
        MyTableView tableView = new MyTableView(tableModel);

        applyColumns(tableView);
        applySelectionMode(tableView);
        applyHeader(tableView);
        applySpeedSearch(tableView);
        applyItemHeight(tableView);

        tableView.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                getListenerDispatcher(TableSelectEvent.class)
                    .onEvent(new TableSelectEvent(this, getSelectedItems()));
            }
        });

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                int row = tableView.rowAtPoint(event.getPoint());
                if (row < 0) {
                    return false;
                }
                getListenerDispatcher(TableDoubleClickEvent.class)
                    .onEvent(new TableDoubleClickEvent(DesktopTableImpl.this, tableView.getRow(row)));
                return true;
            }
        }.installOn(tableView);

        return tableView;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyColumns(TableView<Item> tableView) {
        ColumnInfo[] array = myColumns.toArray(new ColumnInfo[0]);
        ((DesktopTableModel<Item>) tableView.getModel()).setColumnInfos(array);

        // a structure change drops the swing column state, so the per column spec is pushed back
        for (int i = 0; i < myColumns.size() && i < tableView.getColumnModel().getColumnCount(); i++) {
            tableView.getColumnModel().getColumn(i).setResizable(myColumns.get(i).isResizable());
        }
        tableView.updateColumnSizes();
    }

    private void applySelectionMode(TableView<Item> tableView) {
        switch (mySelectionMode) {
            case NONE -> tableView.setRowSelectionAllowed(false);
            case SINGLE -> {
                tableView.setRowSelectionAllowed(true);
                tableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            }
            case MULTIPLE -> {
                tableView.setRowSelectionAllowed(true);
                tableView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }
        }
    }

    private void applyHeader(TableView<Item> tableView) {
        tableView.setShowColumns(myShowHeader);
    }

    private void applySpeedSearch(TableView<Item> tableView) {
        if (mySpeedSearchConverter != null) {
            Function<Item, String> converter = mySpeedSearchConverter;
            new TableViewSpeedSearch<>(tableView) {
                @Override
                protected @Nullable String getItemText(Item element) {
                    return converter.apply(element);
                }
            };
        }
    }

    private void applyItemHeight(TableView<Item> tableView) {
        if (myItemHeightGetter == null) {
            return;
        }

        // JTable has no per row height hook, so each row is set outright - the height comes from the
        // caller, so there is no measure pass
        for (int row = 0; row < tableView.getRowCount(); row++) {
            tableView.setRowHeight(row, myItemHeightGetter.applyAsInt(tableView.getRow(row)));
        }
    }

    @Override
    public FlatDataModel<Item> getDataModel() {
        return myModel;
    }

    @Override
    public <Value> TableColumn<Item, Value> addColumn(LocalizeValue header, Function<Item, Value> valueProvider) {
        DesktopTableColumnImpl<Item, Value> column = new DesktopTableColumnImpl<>(header, valueProvider);
        myColumns.add(column);

        if (isInitialized()) {
            applyColumns(toAWTComponent());
        }
        return column;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<TableColumn<Item, ?>> getColumns() {
        return List.copyOf((List) myColumns);
    }

    @Override
    public void setSelectionMode(SelectionMode mode) {
        mySelectionMode = mode;
        if (isInitialized()) {
            applySelectionMode(toAWTComponent());
        }
    }

    @Override
    public @Nullable Item getSelectedItem() {
        return toAWTComponent().getSelectedObject();
    }

    @Override
    public List<Item> getSelectedItems() {
        return toAWTComponent().getSelectedObjects();
    }

    @RequiredUIAccess
    @Override
    public void select(Item item) {
        toAWTComponent().addSelection(item);
    }

    @RequiredUIAccess
    @Override
    public void deselectAll() {
        toAWTComponent().clearSelection();
    }

    @Override
    public void setShowHeader(boolean show) {
        myShowHeader = show;
        if (isInitialized()) {
            applyHeader(toAWTComponent());
        }
    }

    @RequiredUIAccess
    @Override
    public void scrollTo(Item item) {
        TableView<Item> tableView = toAWTComponent();
        int index = myModel.indexOf(item);
        if (index < 0) {
            return;
        }
        int row = tableView.convertRowIndexToView(index);
        tableView.scrollRectToVisible(tableView.getCellRect(row, 0, true));
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<Item, String> converter) {
        mySpeedSearchConverter = converter;
        if (isInitialized()) {
            applySpeedSearch(toAWTComponent());
        }
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        if (!isInitialized()) {
            return null;
        }
        SpeedSearchSupply supply = SpeedSearchSupply.getSupply(toAWTComponent());
        return supply == null ? null : supply.getEnteredPrefix();
    }

    @Override
    public void setItemHeightGetter(@Nullable ToIntFunction<Item> getter) {
        myItemHeightGetter = getter;
        if (isInitialized()) {
            applyItemHeight(toAWTComponent());
        }
    }
}
