// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

public interface GridColumn extends ColumnDescriptor {
    int getColumnNumber();

    default @Nullable Object getValue(GridRow row) {
        return row.getValue(getColumnNumber());
    }
}
