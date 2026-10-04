// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.util.collection.JBIterable;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public interface MutationsStorage {
    void set(ModelIndex<GridRow> row, ModelIndex<GridColumn> column, @Nullable CellMutation value);

    @Nullable
    MutationData get(ModelIndex<GridRow> row, ModelIndex<GridColumn> column);

    void deleteRow(ModelIndex<GridRow> rowIdx);

    boolean isModified(ModelIndex<GridRow> row);

    boolean isValid(@Nullable ModelIndex<GridRow> row, @Nullable ModelIndex<GridColumn> column);

    boolean hasUnparsedValues();

    boolean hasUnparsedValues(ModelIndex<GridRow> row);

    boolean isInsertedRow(ModelIndex<GridRow> row);

    boolean isInsertedColumn(ModelIndex<GridColumn> idx);

    int getInsertedRowsCount();

    int getInsertedColumnsCount();

    int getDeletedRowsCount();

    int getDeletedColumnsCount();

    boolean isDeletedRow(ModelIndex<GridRow> row);

    boolean isDeletedColumn(ModelIndex<GridColumn> column);

    boolean isDeletedRows(ModelIndexSet<GridRow> rows);

    @Nullable
    ModelIndex<GridRow> getLastInsertedRow();

    void insertColumn(ModelIndex<GridColumn> idx, GridColumn column);

    void renameColumn(ModelIndex<GridColumn> idx, String newName);

    void removeColumnFromDeleted(ModelIndex<GridColumn> index);

    void removeRowFromDeleted(ModelIndex<GridRow> index);

    @Nullable
    GridColumn getInsertedColumn(ModelIndex<GridColumn> idx);

    Set<ModelIndex<GridRow>> getModifiedRows();

    void deleteColumn(ModelIndex<GridColumn> columnIdx);

    boolean hasChanges();

    int getModifiedRowsCount();

    JBIterable<ModelIndex<GridRow>> getDeletedRows();

    JBIterable<ModelIndex<GridColumn>> getDeletedColumns();

    JBIterable<ModelIndex<GridRow>> getInsertedRows();

    JBIterable<ModelIndex<GridColumn>> getInsertedColumns();

    void insertRow(ModelIndex<GridRow> row);

    void clearRow(ModelIndex<GridRow> rowIdx);

    void clearColumns();
}
