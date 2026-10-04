// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import java.util.List;

public interface GridDataSupport {
    void revert(ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns);

    boolean isDeletedRows(ModelIndexSet<GridRow> rows);

    boolean isModified(ModelIndex<GridRow> row, ModelIndex<GridColumn> column);

    boolean hasPendingChanges();

    boolean hasUnparsedValues();

    boolean hasMutator();

    boolean hasRowMutator();

    boolean canRevert();

    boolean isSubmitImmediately();

    void finishBuildingAndApply(List<CellMutation.Builder> mutations);

    boolean isDeletedColumn(ModelIndex<GridColumn> column);

    boolean isInsertedColumn(ModelIndex<GridColumn> column);

    int getInsertedColumnsCount();
}
