// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor;

import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

public final class AbsoluteSymbolPosition {
    long pageNumber;
    int symbolOffsetInPage;

    AbsoluteSymbolPosition(long pageNumber, int symbolOffsetInPage) {
        this.pageNumber = pageNumber;
        this.symbolOffsetInPage = symbolOffsetInPage;
    }

    void set(@Nonnull AbsoluteSymbolPosition from) {
        set(from.pageNumber, from.symbolOffsetInPage);
    }

    void set(long pageNumber, int symbolOffsetInPage) {
        this.pageNumber = pageNumber;
        this.symbolOffsetInPage = symbolOffsetInPage;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbsoluteSymbolPosition other) {
            if (pageNumber == other.pageNumber && symbolOffsetInPage == other.symbolOffsetInPage) {
                return true;
            }
        }
        return false;
    }

    boolean isLessOrEqualsThen(AbsoluteSymbolPosition other) {
        return !isMoreThen(other);
    }

    boolean isMoreOrEqualsThen(AbsoluteSymbolPosition other) {
        return !isLessThen(other);
    }

    boolean isLessThen(AbsoluteSymbolPosition other) {
        if (other == null) return false;

        return pageNumber < other.pageNumber
            || pageNumber == other.pageNumber
            && symbolOffsetInPage < other.symbolOffsetInPage;
    }

    boolean isMoreThen(AbsoluteSymbolPosition other) {
        if (other == null) return false;

        return pageNumber > other.pageNumber
            || pageNumber == other.pageNumber
            && symbolOffsetInPage > other.symbolOffsetInPage;
    }

    @Override
    public @NonNls String toString() {
        return "(" + pageNumber + "," + symbolOffsetInPage + ")";
    }
}
