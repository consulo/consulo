// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public interface GridSelection<Row, Column> {
    void addSelectedColumns(CoreGrid<Row, Column> grid, ModelIndexSet<Column> additionalColumns);

    ModelIndexSet<Row> getSelectedRows();

    ModelIndexSet<Column> getSelectedColumns();
}
