// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

public class MutationRow extends DataConsumer.Row {
    private final GridMutator.DatabaseMutator<GridRow, GridColumn> myMutator;
    private final GridModel<GridRow, GridColumn> myModel;


    public MutationRow(ModelIndex<GridRow> rowIdx,
                       @Nullable Object[] initialData,
                       GridMutator.DatabaseMutator<GridRow, GridColumn> mutator,
                       GridModel<GridRow, GridColumn> databaseModel) {
        super(rowIdx.asInteger() + 1, initialData);
        myMutator = mutator;
        myModel = databaseModel;
    }

    @Override
    public @Nullable Object getValue(int columnNum) {
        ModelIndex<GridRow> row = ModelIndex.forRow(myModel, GridRow.toRealIdx(this));
        ModelIndex<GridColumn> column = ModelIndex.forColumn(myModel, columnNum);
        return GridMutationModel.getValueAt(row, column, myMutator, myModel);
    }
}
