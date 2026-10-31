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

import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferHandlerAdapter;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferTarget;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.image.DesktopAWTCellImage;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.Length;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Table;
import consulo.ui.TableColumn;
import consulo.ui.TransferHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TableDoubleClickEvent;
import consulo.ui.event.TableSelectEvent;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.speedSearch.TableViewSpeedSearch;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.color.ColorValue;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.LazyFlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-09-15
 */
class DesktopTableImpl<Item> extends SwingComponentDelegate<TableView<Item>> implements Table<Item>, DesktopAWTTransferTarget<Item> {
    class MyTableView extends TableView<Item> implements FromSwingComponentWrapper {
        MyTableView(DesktopTableModel<Item> model) {
            super(model);
            // an editor whose focus moves out of the table commits - otherwise it stays open and its value is lost
            putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        }

        @Override
        public Component toUIComponent() {
            return DesktopTableImpl.this;
        }
    }

    private static final int MAX_ITEMS_FOR_SIZE_CALCULATION = 50;

    private final FlatDataModel<Item> myModel;
    private final List<DesktopTableColumnImpl<Item, ?>> myColumns = new ArrayList<>();

    private boolean myAllowMultipleSelect;
    private boolean myShowHeader = true;
    private @Nullable Function<Item, String> mySpeedSearchConverter;
    private @Nullable Function<Item, Length> myItemHeightGetter;
    private @Nullable Function<Item, ColorValue> myRowBackgroundGetter;
    private @Nullable TransferHandler<Item> myTransferHandler;
    private @Nullable DesktopAWTCellImage myHoveredCellImage;
    private long myHoveredCellKey = -1;

    public DesktopTableImpl(FlatDataModel<Item> model) {
        myModel = model;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected TableView<Item> createComponent() {
        DesktopTableModel<Item> tableModel = new DesktopTableModel<>(myModel);
        MyTableView tableView = new MyTableView(tableModel);

        applyColumns(tableView);
        applyMultipleSelect(tableView);
        applyHeader(tableView);
        applySpeedSearch(tableView);
        applyItemHeight(tableView);
        applyTransferHandler(tableView);
        if (myModel instanceof LazyFlatDataModel) {
            tableView.setMaxItemsForSizeCalculation(MAX_ITEMS_FOR_SIZE_CALCULATION);
        }

        tableView.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                getListenerDispatcher(TableSelectEvent.class)
                    .onEvent(new TableSelectEvent(this, getSelectedItems(), DesktopAWTInputDetails.currentEvent(tableView)));
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
                    .onEvent(new TableDoubleClickEvent(DesktopTableImpl.this, tableView.getRow(row), DesktopAWTInputDetails.convert(tableView, event)));
                return true;
            }
        }.installOn(tableView);

        MouseAdapter cellImageMouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                onCellImageMouse(tableView, e, false);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    onCellImageMouse(tableView, e, true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (tableView.getMousePosition() == null) {
                    exitHoveredCellImage();
                }
            }
        };
        tableView.addMouseListener(cellImageMouse);
        tableView.addMouseMotionListener(cellImageMouse);

        // like the trees: the row under a popup trigger becomes the selection before the context menu event, which is
        // what a ContextMenuEvent listener reads its row from - added before that dispatch, so it runs first
        tableView.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectRowUnderPopupTrigger(tableView, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selectRowUnderPopupTrigger(tableView, e);
            }
        });

        return tableView;
    }

    private static void selectRowUnderPopupTrigger(JTable table, MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int row = table.rowAtPoint(e.getPoint());
        if (row >= 0 && !table.isRowSelected(row)) {
            table.setRowSelectionInterval(row, row);
        }
    }

    @RequiredUIAccess
    private void onCellImageMouse(TableView<Item> tableView, MouseEvent event, boolean click) {
        Point point = event.getPoint();
        int row = tableView.rowAtPoint(point);
        int column = tableView.columnAtPoint(point);

        DesktopAWTCellImage image = null;
        Rectangle imageRect = null;
        if (row >= 0 && column >= 0) {
            Rectangle cell = tableView.getCellRect(row, column, false);
            Object value = tableView.getValueAt(row, column);
            java.awt.Component rendered = tableView.getCellRenderer(row, column)
                .getTableCellRendererComponent(tableView, value, tableView.isRowSelected(row), false, row, column);
            if (rendered instanceof DesktopSuffixCellRendererPanel suffixPanel) {
                rendered = suffixPanel.getMainComponent();
            }
            if (rendered instanceof SimpleColoredComponent colored
                && !colored.isIconOnTheRight()
                && colored.getIcon() instanceof DesktopAWTCellImage cellImage) {
                Rectangle rect = new Rectangle(cell.x + colored.getIpad().left, cell.y, cellImage.getWidth(), cell.height);
                if (rect.contains(point)) {
                    image = cellImage;
                    imageRect = rect;
                }
            }
        }

        long key = image == null ? -1 : ((long) row << 32) | (column & 0xffffffffL);
        if (key != myHoveredCellKey) {
            exitHoveredCellImage();
        }
        if (image == null || imageRect == null) {
            return;
        }

        myHoveredCellKey = key;
        myHoveredCellImage = image;
        int x = point.x - imageRect.x;
        int y = point.y - imageRect.y;
        if (click) {
            image.mouseClicked(x, y, imageRect.height);
        }
        else {
            image.mouseMoved(x, y, imageRect.height);
        }
    }

    @RequiredUIAccess
    private void exitHoveredCellImage() {
        DesktopAWTCellImage hovered = myHoveredCellImage;
        myHoveredCellImage = null;
        myHoveredCellKey = -1;
        if (hovered != null) {
            hovered.mouseExited();
        }
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

    private void applyMultipleSelect(TableView<Item> tableView) {
        tableView.setSelectionMode(
            myAllowMultipleSelect ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION
        );
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
            tableView.setRowHeight(row, DesktopLength.toPixels(tableView, myItemHeightGetter.apply(tableView.getRow(row))));
        }
    }

    private void applyTransferHandler(TableView<Item> tableView) {
        TransferHandler<Item> handler = myTransferHandler;
        if (handler == null) {
            tableView.setTransferHandler(null);
            tableView.setDragEnabled(false);
            return;
        }

        DesktopAWTTransferHandlerAdapter<Item> adapter = new DesktopAWTTransferHandlerAdapter<>(this, handler, this);
        tableView.setTransferHandler(adapter);
        tableView.setDragEnabled(adapter.isDragAndDropSupported());
        if (adapter.isDragAndDropSupported()) {
            tableView.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        }
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<Item> handler) {
        myTransferHandler = handler;
        if (isInitialized()) {
            applyTransferHandler(toAWTComponent());
        }
    }

    @Override
    public @Nullable TransferHandler<Item> getTransferHandler() {
        return myTransferHandler;
    }

    @Override
    public List<Item> getTransferItems() {
        return toAWTComponent().getSelectedObjects();
    }

    /**
     * The toolkit states a drop between rows as the index it would be inserted at, so an insertion is turned back into the
     * row it lands next to - past the last row, below that row.
     */
    @Override
    public @Nullable Drop<Item> resolveDrop(javax.swing.TransferHandler.TransferSupport support) {
        if (!(support.getDropLocation() instanceof JTable.DropLocation location)) {
            return null;
        }

        TableView<Item> tableView = toAWTComponent();
        int rowCount = tableView.getRowCount();
        int row = location.getRow();
        if (rowCount == 0 || row < 0) {
            return null;
        }

        if (!location.isInsertRow()) {
            return row < rowCount ? new Drop<>(tableView.getRow(row), DragAndDropTransferHandler.DropPosition.INTO) : null;
        }

        if (row < rowCount) {
            return new Drop<>(tableView.getRow(row), DragAndDropTransferHandler.DropPosition.ABOVE);
        }
        return new Drop<>(tableView.getRow(rowCount - 1), DragAndDropTransferHandler.DropPosition.BELOW);
    }

    @Override
    public FlatDataModel<Item> getDataModel() {
        return myModel;
    }

    @Override
    public <Value> TableColumn<Item, Value> addColumn(LocalizeValue header, Function<Item, Value> valueProvider) {
        DesktopTableColumnImpl<Item, Value> column = new DesktopTableColumnImpl<>(this::getRowBackground, header, valueProvider);
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
    public void setAllowMultipleSelect(boolean allow) {
        myAllowMultipleSelect = allow;
        if (isInitialized()) {
            applyMultipleSelect(toAWTComponent());
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
        toAWTComponent().setSelection(List.of(item));
    }

    @RequiredUIAccess
    @Override
    public void deselectAll() {
        toAWTComponent().clearSelection();
    }

    @Override
    public void setRowBackgroundGetter(@Nullable Function<Item, ColorValue> getter) {
        myRowBackgroundGetter = getter;

        if (isInitialized()) {
            toAWTComponent().repaint();
        }
    }

    /**
     * Read by the columns while they paint - the band is one rule over the row, not a copy per column.
     */
    @Nullable
    ColorValue getRowBackground(Item item) {
        Function<Item, ColorValue> getter = myRowBackgroundGetter;
        return getter == null ? null : getter.apply(item);
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
    public void setItemHeightGetter(@Nullable Function<Item, Length> getter) {
        myItemHeightGetter = getter;
        if (isInitialized()) {
            applyItemHeight(toAWTComponent());
        }
    }
}
