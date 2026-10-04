// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public interface GridModelWithInjections<Row, Column> extends GridModel<Row, Column> {
    void injectValue(ModelIndex<GridRow> row, ModelIndex<GridColumn> columns, Object what);
}
