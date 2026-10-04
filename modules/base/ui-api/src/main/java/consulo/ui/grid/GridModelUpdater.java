// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import java.util.List;

public interface GridModelUpdater {
    void removeRows(int firstRowIndex, int rowCount);

    void setColumns(List<? extends GridColumn> columns);

    void setRows(int firstRowIndex, List<? extends GridRow> rows, GridRequestSource source);

    void addRows(List<? extends GridRow> rows);

    void afterLastRowAdded();
}
