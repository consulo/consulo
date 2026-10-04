// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

/**
 * A row whose row header shows a name instead of its number.
 */
public final class NamedRow extends DataConsumer.Row implements GridRow {
    public final String name;

    private NamedRow(int rowNum, String name, @Nullable Object[] values) {
        super(rowNum, values);
        this.name = name;
    }

    public static NamedRow create(int realIdx, String name, @Nullable Object[] values) {
        return new NamedRow(realIdx + 1, name, values);
    }
}
