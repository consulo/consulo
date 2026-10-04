// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public interface SelectionModel<Row, Column> {

    GridSelection<Row, Column> store();

    void restore(GridSelection<Row, Column> selection);

    GridSelection<Row, Column> fit(GridSelection<Row, Column> selection);

    GridSelectionTracker getTracker();

    void setSelection(ModelIndexSet<Row> rows, ModelIndexSet<Column> columns);

    void setSelection(ModelIndex<Row> row, ModelIndex<Column> column);

    void setRowSelection(ModelIndexSet<Row> selection, boolean selectAtLeastOneCell);

    void setRowSelection(ModelIndex<Row> selection, boolean selectAtLeastOneCell);

    void addRowSelection(ModelIndexSet<Row> selection);

    void setColumnSelection(ModelIndexSet<Column> selection, boolean selectAtLeastOneCell);

    void setColumnSelection(ModelIndex<Column> selection, boolean selectAtLeastOneCell);

    boolean isSelectionEmpty();

    boolean isSelected(ModelIndex<Row> row, ModelIndex<Column> column);

    boolean isSelected(ViewIndex<Row> row, ViewIndex<Column> column);

    boolean isSelectedColumn(ModelIndex<Column> column);

    boolean isSelectedRow(ModelIndex<Row> row);

    int getSelectedRowCount();

    int getSelectedColumnCount();

    void selectWholeRow();

    void selectWholeColumn();

    void clearSelection();

    ModelIndex<Row> getSelectedRow();

    default ModelIndex<Row> getLeadSelectionRow() {
        return getSelectedRow();
    }

    ModelIndexSet<Row> getSelectedRows();

    ModelIndex<Column> getSelectedColumn();

    default ModelIndex<Column> getLeadSelectionColumn() {
        return getSelectedColumn();
    }

    ModelIndexSet<Column> getSelectedColumns();
}
