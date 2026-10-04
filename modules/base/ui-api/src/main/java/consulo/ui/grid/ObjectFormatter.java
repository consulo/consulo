// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

public interface ObjectFormatter {
    @Nullable
    String objectToString(@Nullable Object o, GridColumn column, ObjectFormatterConfig config);

    boolean isStringLiteral(@Nullable GridColumn column, @Nullable Object value, ObjectFormatterMode mode);

    String getStringLiteral(String value, GridColumn column, ObjectFormatterMode mode);
}
