// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.ex.grid;

import consulo.ui.grid.CellMutation;
import consulo.ui.grid.DataConsumer;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridModel;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ModelIndexSet;
import consulo.ui.grid.MutationData;
import consulo.ui.grid.MutationsStorage;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.TypedValue;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Notes:
 * <ul>
 * <li>the storage may be created for an empty model - with 0 rows and columns - and grows from there: growing the arrays by half of
 * their size alone would leave an empty array empty;</li>
 * <li>a renamed inserted column is a copy made by {@link DataConsumer.Column#copy}: the grid model has no JDBC type name and class name
 * of the column to pass on.</li>
 * </ul>
 */
public class MutationsStorageImpl implements MutationsStorage {
    private static final double MULTIPLIER = 1.5;

    private final double myMultiplier;
    private final GridModel<GridRow, GridColumn> myModel;
    private final RowsCounter myModifiedRowsCounter;
    private final RowsCounter myRowsWithUnparsedValuesCounter;
    private final Queue<ModelIndex<GridRow>> myInsertedRows;
    private final Map<ModelIndex<GridColumn>, GridColumn> myInsertedColumns;
    private final Set<ModelIndex<GridRow>> myDeletedRows;
    private final Set<ModelIndex<GridColumn>> myDeletedColumns;

    private @Nullable MutationData[] @Nullable [] myValues;

    private int myMaxRows;
    private int myRows;
    private int myMaxColumns;
    private int myColumns;

    public MutationsStorageImpl(GridModel<GridRow, GridColumn> model, int rows, int columns) {
        this(model, rows, columns, MULTIPLIER);
    }

    private MutationsStorageImpl(GridModel<GridRow, GridColumn> model, int rows, int columns, double multiplier) {
        myMultiplier = multiplier;
        myRows = rows;
        myColumns = columns;
        myMaxRows = getIncreasedValue(rows);
        myMaxColumns = getIncreasedValue(columns);
        myModel = model;
        myValues = new @Nullable MutationData[myMaxRows] @Nullable [];
        myModifiedRowsCounter = new ModifiedRowsCounter();
        myRowsWithUnparsedValuesCounter = new RowsWithUnparsedValuesCounter();
        myInsertedRows = new PriorityQueue<>((o1, o2) -> Integer.compare(o2.asInteger(), o1.asInteger()));
        myDeletedRows = new HashSet<>();
        myDeletedColumns = new HashSet<>();
        myInsertedColumns = new HashMap<>();
    }

    @Override
    public void set(ModelIndex<GridRow> row, ModelIndex<GridColumn> column, @Nullable CellMutation value) {
        if (!isValid(row, column)) {
            return;
        }
        allocateSpace(row, column);
        countModifications(value, row, column);
        getAllocatedRow(row.asInteger())[column.asInteger()] =
            value == null ? null : new MutationData(value.getValue(), value.getMetadata());
    }

    protected void allocateSpace(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        int rowIdx = row.asInteger();
        int colIdx = column.asInteger();
        if (rowIdx >= myMaxRows || colIdx >= myMaxColumns) {
            reallocate(Math.max(rowIdx, myMaxRows), Math.max(colIdx, myMaxColumns));
        }
        increase(rowIdx, colIdx);
        if (myValues[rowIdx] == null) {
            myValues[rowIdx] = new MutationData[myMaxColumns];
        }
    }

    /**
     * The row of the values {@link #allocateSpace} allocated.
     */
    private @Nullable MutationData[] getAllocatedRow(int rowIdx) {
        @Nullable MutationData[] values = myValues[rowIdx];
        if (values == null) {
            throw new IllegalStateException("Row " + rowIdx + " is not allocated");
        }
        return values;
    }

    private void shiftUpColumn(ModelIndex<GridColumn> column) {
        myModifiedRowsCounter.deleteColumn(column);
        myRowsWithUnparsedValuesCounter.deleteColumn(column);
        int idx = column.asInteger();
        if (idx >= myColumns) {
            // no value was ever set in this column or after it - nothing to shift
            return;
        }
        for (@Nullable MutationData[] values : myValues) {
            if (values == null) {
                continue;
            }
            System.arraycopy(values, idx + 1, values, idx, myColumns - 1 - idx);
            values[myColumns - 1] = null;
        }
    }

    private void shiftUpRow(ModelIndex<GridRow> row) {
        int idx = row.asInteger();
        // no value was ever set in this row or after it - nothing to shift
        if (idx < myRows) {
            System.arraycopy(myValues, idx + 1, myValues, idx, myRows - 1 - idx);
            myValues[myRows - 1] = null;
        }
        myModifiedRowsCounter.shiftUp(row);
        myRowsWithUnparsedValuesCounter.shiftUp(row);
    }

    @Override
    public Set<ModelIndex<GridRow>> getModifiedRows() {
        Set<ModelIndex<GridRow>> modifiedRows = new HashSet<>();
        for (int i : myModifiedRowsCounter.get()) {
            modifiedRows.add(ModelIndex.forRow(myModel, i));
        }
        return modifiedRows;
    }

    @Override
    public void deleteColumn(ModelIndex<GridColumn> columnIdx) {
        if (!isValidColumn(columnIdx)) {
            return;
        }
        boolean isInserted = isInsertedColumn(columnIdx);
        clearColumn(columnIdx);
        if (isInserted) {
            insertedColumnRemoved(columnIdx);
        }
        else {
            myDeletedColumns.add(columnIdx);
        }
    }

    @Override
    public void deleteRow(ModelIndex<GridRow> rowIdx) {
        if (!isValidRow(rowIdx)) {
            return;
        }
        boolean isInserted = isInsertedRow(rowIdx);
        clearRow(rowIdx);
        if (isInserted) {
            insertedRowRemoved(rowIdx);
        }
        else {
            myDeletedRows.add(rowIdx);
        }
    }

    private void insertedColumnRemoved(ModelIndex<GridColumn> columnIdx) {
        List<ModelIndex<GridColumn>> indexesToShift = findBiggerIndices(myInsertedColumns.keySet(), columnIdx);
        Map<ModelIndex<GridColumn>, GridColumn> columnsToShift = new HashMap<>();
        for (ModelIndex<GridColumn> idx : indexesToShift) {
            GridColumn column = myInsertedColumns.remove(idx);
            if (column != null) {
                columnsToShift.put(idx, column);
            }
        }
        JBIterable.from(columnsToShift.entrySet())
            .map(keyValue -> Pair.create(ModelIndex.forColumn(myModel, keyValue.getKey().asInteger() - 1), keyValue.getValue()))
            .filter(keyValue -> keyValue.getFirst().asInteger() >= 0)
            .forEach(keyValue -> myInsertedColumns.put(keyValue.getFirst(), keyValue.getSecond()));
        shiftUpColumn(columnIdx);
    }

    private void insertedRowRemoved(ModelIndex<GridRow> rowIdx) {
        List<ModelIndex<GridRow>> indexesToShift = findBiggerIndices(myInsertedRows, rowIdx);
        myInsertedRows.removeAll(indexesToShift);
        JBIterable.from(indexesToShift)
            .map(idx -> ModelIndex.forRow(myModel, idx.asInteger() - 1))
            .filter(idx -> idx.asInteger() >= 0)
            .forEach(myInsertedRows::add);
        shiftUpRow(rowIdx);
    }

    private static <T> List<ModelIndex<T>> findBiggerIndices(Collection<ModelIndex<T>> indexes, ModelIndex<T> idx) {
        return ContainerUtil.filter(indexes, i -> i.asInteger() > idx.asInteger());
    }

    @Override
    public boolean isModified(ModelIndex<GridRow> row) {
        return isValidRow(row) && myModifiedRowsCounter.contains(row.asInteger());
    }

    @Override
    public @Nullable MutationData get(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        return getInner(row, column);
    }

    private @Nullable MutationData getInner(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        int rowIdx = row.asInteger();
        int colIdx = column.asInteger();
        if (!isValid(row, column) || rowIdx >= myRows || colIdx >= myColumns) {
            return null;
        }
        @Nullable MutationData[] values = myValues[rowIdx];
        return values == null ? null : values[colIdx];
    }

    @Override
    public boolean hasChanges() {
        return !myModifiedRowsCounter.isEmpty() || !myInsertedRows.isEmpty() || !myDeletedRows.isEmpty() || !myDeletedColumns.isEmpty();
    }

    @Override
    public int getModifiedRowsCount() {
        return myModifiedRowsCounter.myRowsSet.size();
    }

    private boolean isValidRow(@Nullable ModelIndex<GridRow> row) {
        return isValid(row, null);
    }

    private boolean isValidColumn(@Nullable ModelIndex<GridColumn> column) {
        return isValid(null, column);
    }

    @Override
    public boolean isValid(@Nullable ModelIndex<GridRow> row, @Nullable ModelIndex<GridColumn> column) {
        return (row == null || row.isValid(myModel) || isInsertedRow(row)) &&
            (column == null || column.isValid(myModel) || isInsertedColumn(column));
    }

    private void increase(int rows, int columns) {
        myRows = rows >= myRows ? rows + 1 : myRows;
        myColumns = columns >= myColumns ? columns + 1 : myColumns;
    }

    private void countModifications(@Nullable CellMutation value, ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        @Nullable MutationData[] values = myValues[row.asInteger()];
        MutationData oldData = values == null ? null : values[column.asInteger()];
        myModifiedRowsCounter.countModifications(value, oldData, row, column);
        myRowsWithUnparsedValuesCounter.countModifications(value, oldData, row, column);
    }

    private void reallocate(int rows, int columns) {
        myMaxRows = rows >= myMaxRows ? getIncreasedValue(rows) : myMaxRows;
        myMaxColumns = columns >= myMaxColumns ? getIncreasedValue(columns) : myMaxColumns;
        increase(rows, columns);
        myValues = copy(myValues, myMaxRows, myMaxColumns);
        myModifiedRowsCounter.reallocate();
        myRowsWithUnparsedValuesCounter.reallocate();
    }

    @Override
    public boolean hasUnparsedValues() {
        return !myRowsWithUnparsedValuesCounter.isEmpty();
    }

    @Override
    public boolean hasUnparsedValues(ModelIndex<GridRow> row) {
        return myRowsWithUnparsedValuesCounter.contains(row.asInteger());
    }

    @Override
    public boolean isInsertedRow(ModelIndex<GridRow> row) {
        return myInsertedRows.contains(row);
    }

    @Override
    public boolean isInsertedColumn(ModelIndex<GridColumn> idx) {
        return myInsertedColumns.containsKey(idx);
    }

    @Override
    public int getInsertedRowsCount() {
        return myInsertedRows.size();
    }

    @Override
    public int getInsertedColumnsCount() {
        return myInsertedColumns.size();
    }

    @Override
    public int getDeletedRowsCount() {
        return myDeletedRows.size();
    }

    @Override
    public int getDeletedColumnsCount() {
        return myDeletedColumns.size();
    }

    @Override
    public boolean isDeletedRow(ModelIndex<GridRow> row) {
        return myDeletedRows.contains(row);
    }

    @Override
    public boolean isDeletedColumn(ModelIndex<GridColumn> column) {
        return myDeletedColumns.contains(column);
    }

    @Override
    public boolean isDeletedRows(ModelIndexSet<GridRow> rows) {
        return myDeletedRows.containsAll(rows.asList());
    }

    @Override
    public @Nullable ModelIndex<GridRow> getLastInsertedRow() {
        return myInsertedRows.peek();
    }

    @Override
    public JBIterable<ModelIndex<GridRow>> getDeletedRows() {
        return JBIterable.from(myDeletedRows);
    }

    @Override
    public JBIterable<ModelIndex<GridColumn>> getDeletedColumns() {
        return JBIterable.from(myDeletedColumns);
    }

    @Override
    public JBIterable<ModelIndex<GridRow>> getInsertedRows() {
        return JBIterable.from(myInsertedRows);
    }

    @Override
    public JBIterable<ModelIndex<GridColumn>> getInsertedColumns() {
        return JBIterable.from(myInsertedColumns.keySet());
    }

    @Override
    public void insertRow(ModelIndex<GridRow> row) {
        myInsertedRows.add(row);
    }

    @Override
    public void insertColumn(ModelIndex<GridColumn> idx, GridColumn column) {
        myInsertedColumns.put(idx, column);
    }

    @Override
    public void renameColumn(ModelIndex<GridColumn> idx, String newName) {
        GridColumn column = myInsertedColumns.get(idx);
        if (column == null) {
            return;
        }
        myInsertedColumns.put(idx, DataConsumer.Column.copy(column, idx.asInteger(), newName, column.getType()));
    }

    @Override
    public void removeColumnFromDeleted(ModelIndex<GridColumn> index) {
        myDeletedColumns.remove(index);
    }

    private void clearColumn(ModelIndex<GridColumn> column) {
        myInsertedColumns.remove(column);
        myDeletedColumns.remove(column);
        myModifiedRowsCounter.deleteColumn(column);
        myRowsWithUnparsedValuesCounter.deleteColumn(column);
        int colIdx = column.asInteger();
        for (int i = 0; i < myMaxRows; i++) {
            @Nullable MutationData[] values = myValues[i];
            // a column after the allocated ones has no values
            if (values == null || colIdx >= values.length) {
                continue;
            }
            values[colIdx] = null;
        }
    }

    @Override
    public void removeRowFromDeleted(ModelIndex<GridRow> index) {
        myDeletedRows.remove(index);
    }

    @Override
    public void clearRow(ModelIndex<GridRow> rowIdx) {
        myInsertedRows.remove(rowIdx);
        myDeletedRows.remove(rowIdx);
        if (rowIdx.asInteger() < myRows) {
            myValues[rowIdx.asInteger()] = null;
        }
        myModifiedRowsCounter.deleteRow(rowIdx);
        myRowsWithUnparsedValuesCounter.deleteRow(rowIdx);
    }

    @Override
    public void clearColumns() {
        myInsertedColumns.clear();
        myDeletedColumns.clear();
    }

    @Override
    public @Nullable GridColumn getInsertedColumn(ModelIndex<GridColumn> idx) {
        return myInsertedColumns.get(idx);
    }

    private static @Nullable MutationData[] @Nullable [] copy(@Nullable MutationData[] @Nullable [] from, int rows, int columns) {
        @Nullable MutationData[] @Nullable [] newArray = new @Nullable MutationData[rows] @Nullable [];
        for (int i = 0; i < from.length; i++) {
            @Nullable MutationData[] values = from[i];
            if (values == null) {
                continue;
            }
            newArray[i] = new MutationData[columns];
            System.arraycopy(values, 0, newArray[i], 0, values.length);
        }
        return newArray;
    }

    private int getIncreasedValue(int value) {
        // at least one more, so an empty storage grows too
        return Math.max(value + 1, (int) Math.round(value * myMultiplier));
    }

    private abstract class RowsCounter {
        private final IntSet myRowsSet;
        private int[] myCount;

        RowsCounter() {
            myCount = new int[myMaxRows];
            myRowsSet = new IntOpenHashSet(myMaxRows);
        }

        void shiftUp(ModelIndex<GridRow> row) {
            int idx = row.asInteger();
            // no row at or after this one was ever counted - nothing to shift
            if (idx < myRows) {
                System.arraycopy(myCount, idx + 1, myCount, idx, myRows - 1 - idx);
                myCount[myRows - 1] = 0;
            }
            myRowsSet.remove(idx);
            IntSet newRowsSet = new IntOpenHashSet();
            for (int oldRowIdx : myRowsSet) {
                if (oldRowIdx > idx) {
                    newRowsSet.add(oldRowIdx - 1);
                    continue;
                }
                newRowsSet.add(oldRowIdx);
            }
            myRowsSet.clear();
            myRowsSet.addAll(newRowsSet);
        }

        void deleteColumn(ModelIndex<GridColumn> idx) {
            int colIdx = idx.asInteger();
            for (int i = 0; i < myMaxRows; i++) {
                @Nullable MutationData[] values = myValues[i];
                // a column after the allocated ones has no values
                MutationData oldValue = values == null || colIdx >= values.length ? null : values[colIdx];
                countModifications(null, oldValue, ModelIndex.forRow(myModel, i), idx);
            }
        }

        void deleteRow(ModelIndex<GridRow> idx) {
            int i = idx.asInteger();
            if (i < myRows) {
                myCount[i] = 0;
            }
            myRowsSet.remove(i);
        }

        public boolean isEmpty() {
            return myRowsSet.isEmpty();
        }

        public void clear() {
            myCount = new int[myMaxRows];
            myRowsSet.clear();
        }

        public boolean contains(int row) {
            return myRowsSet.contains(row);
        }

        public void countModifications(@Nullable CellMutation value,
                                       @Nullable MutationData oldValue,
                                       ModelIndex<GridRow> row,
                                       ModelIndex<GridColumn> column) {
            myCount[row.asInteger()] += countModificationsInner(value, oldValue, row, column);
            if (myCount[row.asInteger()] > 0) {
                myRowsSet.add(row.asInteger());
                return;
            }
            myRowsSet.remove(row.asInteger());
        }

        protected abstract int countModificationsInner(@Nullable CellMutation value,
                                                       @Nullable MutationData oldValue,
                                                       ModelIndex<GridRow> row,
                                                       ModelIndex<GridColumn> column);

        public void reallocate() {
            int[] myNewCount = new int[myMaxRows];
            System.arraycopy(myCount, 0, myNewCount, 0, myCount.length);
            myCount = myNewCount;
        }

        public IntSet get() {
            return myRowsSet;
        }
    }

    private class ModifiedRowsCounter extends RowsCounter {
        @Override
        protected int countModificationsInner(@Nullable CellMutation value,
                                              @Nullable MutationData oldValue,
                                              ModelIndex<GridRow> row,
                                              ModelIndex<GridColumn> column) {
            boolean newValueEqualToDatabase = value == null || equalToDatabase(value.getValue(), row, column);
            boolean oldValueEqualToDatabase = oldValue == null || equalToDatabase(oldValue.getValue(), row, column);
            return !newValueEqualToDatabase && oldValueEqualToDatabase ? 1 :
                !oldValueEqualToDatabase && newValueEqualToDatabase ? -1 :
                    0;
        }

        boolean equalToDatabase(@Nullable Object value, ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
            if (!isValid(row, column) || isInsertedRow(row) || isInsertedColumn(column)) {
                return TypedValue.unwrap(value) == ReservedCellValue.UNSET;
            }
            Object databaseValue = myModel.getValueAt(row, column);
            return ObjectUtil.notNull(databaseValue, ReservedCellValue.NULL) ==
                ObjectUtil.notNull(TypedValue.unwrap(value), ReservedCellValue.NULL);
        }
    }

    private class RowsWithUnparsedValuesCounter extends RowsCounter {
        @Override
        protected int countModificationsInner(@Nullable CellMutation value,
                                              @Nullable MutationData oldValue,
                                              ModelIndex<GridRow> row,
                                              ModelIndex<GridColumn> column) {
            return isUnparsed(value) && !isUnparsed(oldValue) ? 1 :
                isUnparsed(oldValue) && !isUnparsed(value) ? -1 :
                    0;
        }

        private static boolean isUnparsed(@Nullable CellMutation value) {
            return value != null && value.getValue() instanceof UnparsedValue;
        }

        private static boolean isUnparsed(@Nullable MutationData value) {
            return value != null && value.getValue() instanceof UnparsedValue;
        }
    }
}
