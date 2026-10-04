// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

import java.util.Collections;


public class CellMutation extends Mutation {
    private final ModelIndex<GridColumn> myColumn;
    private final @Nullable Object myNewValue;
    private @Nullable Object myMetadata;

    public CellMutation(ModelIndex<GridRow> row,
                        ModelIndex<GridColumn> column,
                        @Nullable Object newValue) {
        super(row);
        myColumn = column;
        myNewValue = newValue;
    }

    public @Nullable Object getMetadata() {
        return myMetadata;
    }

    public CellMutation withMetadata(@Nullable Object metadata) {
        myMetadata = metadata;
        return this;
    }

    public boolean canMergeByRowWith(CellMutation mutation) {
        return mutation.getRow().equals(getRow());
    }

    public @Nullable Object getValue() {
        return myNewValue;
    }

    public ModelIndex<GridColumn> getColumn() {
        return myColumn;
    }

    public @Nullable RowMutation createRowMutation(GridModel<GridRow, GridColumn> model) {
        GridColumn column = model.getColumn(myColumn);
        GridRow row = model.getRow(getRow());
        return row == null || column == null ?
            null :
            new RowMutation(row, Collections.singletonList(new ColumnQueryData(column, myNewValue)));
    }

    public static class Builder {
        private @Nullable ModelIndex<GridRow> myRow;
        private @Nullable ModelIndex<GridColumn> myColumn;
        private @Nullable Object myValue;

        public Builder row(ModelIndex<GridRow> row) {
            myRow = row;
            return this;
        }

        public Builder column(ModelIndex<GridColumn> column) {
            myColumn = column;
            return this;
        }

        public Builder value(@Nullable Object value) {
            myValue = value;
            return this;
        }

        public @Nullable ModelIndex<GridRow> getRow() {
            return myRow;
        }

        // it does not check that the row and the column were set
        @SuppressWarnings("NullAway")
        public CellMutation build() {
            return new CellMutation(myRow, myColumn, myValue);
        }
    }
}
