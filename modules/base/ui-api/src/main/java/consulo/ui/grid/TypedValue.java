// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

public class TypedValue<T> {
    private final ReservedCellValue myValue;

    private final T myType;

    public TypedValue(ReservedCellValue value, T type) {
        myValue = value;
        myType = type;
    }

    public ReservedCellValue getValue() {
        return myValue;
    }

    public T getType() {
        return myType;
    }

    public static @Nullable Object unwrap(@Nullable Object value) {
        return value instanceof TypedValue ? ((TypedValue<?>) value).myValue : value;
    }
}
