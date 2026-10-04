// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.util.collection.JBIterable;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface GridMutator<Row, Column> {

    boolean isUpdateSafe(ModelIndexSet<Row> rowIndices, ModelIndexSet<Column> columnIndices, @Nullable Object newValue);

    boolean hasPendingChanges();

    boolean hasUnparsedValues();

    boolean isUpdateImmediately();

    void mutate(GridRequestSource source,
                ModelIndexSet<Row> row,
                ModelIndexSet<Column> column,
                @Nullable Object newValue,
                boolean allowImmediateUpdate);

    void mutate(GridRequestSource source,
                List<CellMutation> mutations,
                boolean allowImmediateUpdate);

    interface RowsMutator<Row, Column> extends GridMutator<Row, Column> {

        void deleteRows(GridRequestSource source, ModelIndexSet<Row> rows);

        void insertRows(GridRequestSource source, int amount);

        /**
         * Inserts empty rows before a row, or after the last one.
         *
         * @param before the row the new rows go before, or {@code null} to append them. The default appends them in any case: a
         *               data source which can insert in place overrides it
         */
        default void insertRows(GridRequestSource source, @Nullable ModelIndex<Row> before, int amount) {
            insertRows(source, amount);
        }

        void cloneRow(GridRequestSource source, ModelIndex<Row> toClone);

        boolean isDeletedRow(ModelIndex<Row> row);

        boolean isDeletedRows(ModelIndexSet<Row> rows);

        boolean isInsertedRow(ModelIndex<Row> row);

        int getInsertedRowsCount();

        @Nullable
        ModelIndex<Row> getLastInsertedRow();

        ModelIndexSet<Row> getAffectedRows();

        JBIterable<ModelIndex<Row>> getInsertedRows();
    }

    interface DatabaseMutator<Row, Column> extends RowsMutator<Row, Column>, ColumnsMutator<Row, Column> {
        void submit(GridRequestSource source, boolean includeInserted);

        String getPendingChanges();

        @Nullable
        MutationType getMutationType(ModelIndex<Row> row);

        boolean hasUnparsedValues(ModelIndex<Row> row);

        @Nullable
        MutationData getMutation(ModelIndex<Row> row, ModelIndex<Column> column);

        @Nullable
        MutationType getMutationType(ModelIndex<Row> row, ModelIndex<Column> column);

        boolean isFailed();

        boolean hasMutatedRows(ModelIndexSet<Row> rows, ModelIndexSet<Column> columns);

        void revert(GridRequestSource source,
                    ModelIndexSet<Row> rows,
                    ModelIndexSet<Column> columns);
    }


    interface ColumnsMutator<Row, Column> extends GridMutator<Row, Column> {

        void deleteColumns(GridRequestSource source, ModelIndexSet<Column> columns);

        void insertColumn(GridRequestSource source, @Nullable String name);

        /**
         * Inserts an empty column before a column, or after the last one.
         *
         * @param before the column the new column goes before, or {@code null} to append it. The default appends it in any case: a
         *               data source which can insert in place overrides it
         * @param name   the name of the new column, or {@code null} for a generated one
         */
        default void insertColumn(GridRequestSource source, @Nullable ModelIndex<Column> before, @Nullable String name) {
            insertColumn(source, name);
        }

        /**
         * Moves the column at {@code from} to the index {@code to}, shifting the columns between by one. The default does nothing: a
         * data source which cannot reorder its columns keeps them.
         */
        default void moveColumn(
            GridRequestSource source,
            ModelIndex<Column> from,
            ModelIndex<Column> to
        ) {
        }

        void cloneColumn(GridRequestSource source, ModelIndex<Column> toClone);

        int getInsertedColumnsCount();

        JBIterable<ModelIndex<Column>> getInsertedColumns();

        boolean isInsertedColumn(ModelIndex<Column> idx);

        boolean isDeletedColumn(ModelIndex<Column> idx);

        @Nullable
        Column getInsertedColumn(ModelIndex<Column> idx);

        void renameColumn(GridRequestSource source, ModelIndex<Column> idx, String newName);
    }
}
