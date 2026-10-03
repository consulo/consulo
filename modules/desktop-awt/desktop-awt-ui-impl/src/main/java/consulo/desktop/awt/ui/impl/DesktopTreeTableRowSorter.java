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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.tree.JBTreeTable;
import org.jspecify.annotations.Nullable;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopTreeTableRowSorter<E> extends RowSorter<TableModel> {
    private final JBTreeTable myTreeTable;
    private final DesktopTreeTableImpl<E> myOwner;

    private volatile @Nullable SortKey mySortKey;

    DesktopTreeTableRowSorter(JBTreeTable treeTable, DesktopTreeTableImpl<E> owner) {
        myTreeTable = treeTable;
        myOwner = owner;
    }

    @Nullable
    SortKey getSortKey() {
        return mySortKey;
    }

    @Override
    public TableModel getModel() {
        return myTreeTable.getTable().getModel();
    }

    @Override
    public void toggleSortOrder(int column) {
        if (!myOwner.isSortable(column)) {
            return;
        }

        SortKey key = mySortKey;
        if (key == null || key.getColumn() != column) {
            setSortKeys(List.of(new SortKey(column, SortOrder.ASCENDING)));
        }
        else if (key.getSortOrder() == SortOrder.ASCENDING) {
            setSortKeys(List.of(new SortKey(column, SortOrder.DESCENDING)));
        }
        else {
            setSortKeys(List.of());
        }
    }

    @Override
    public int convertRowIndexToModel(int index) {
        return index;
    }

    @Override
    public int convertRowIndexToView(int index) {
        return index;
    }

    @Override
    public List<? extends SortKey> getSortKeys() {
        SortKey key = mySortKey;
        return key == null ? List.of() : List.of(key);
    }

    @Override
    public void setSortKeys(@Nullable List<? extends SortKey> keys) {
        SortKey key = keys == null || keys.isEmpty() ? null : keys.get(0);
        if (key != null && (key.getSortOrder() == SortOrder.UNSORTED || !myOwner.isSortable(key.getColumn()))) {
            key = null;
        }

        if (Objects.equals(key, mySortKey)) {
            return;
        }

        mySortKey = key;
        fireSortOrderChanged();
        myOwner.sortChanged(key);
    }

    @Override
    public int getViewRowCount() {
        return myTreeTable.getTree().getRowCount();
    }

    @Override
    public int getModelRowCount() {
        return myTreeTable.getTree().getRowCount();
    }

    @Override
    public void modelStructureChanged() {
    }

    @Override
    public void allRowsChanged() {
    }

    @Override
    public void rowsInserted(int firstRow, int endRow) {
    }

    @Override
    public void rowsDeleted(int firstRow, int endRow) {
    }

    @Override
    public void rowsUpdated(int firstRow, int endRow) {
    }

    @Override
    public void rowsUpdated(int firstRow, int endRow, int column) {
    }
}
