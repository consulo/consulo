// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class AbsoluteSymbolPosition {
    long pageNumber;
    int symbolOffsetInPage;

    AbsoluteSymbolPosition(long pageNumber, int symbolOffsetInPage) {
        this.pageNumber = pageNumber;
        this.symbolOffsetInPage = symbolOffsetInPage;
    }

    void set(AbsoluteSymbolPosition from) {
        set(from.pageNumber, from.symbolOffsetInPage);
    }

    void set(long pageNumber, int symbolOffsetInPage) {
        this.pageNumber = pageNumber;
        this.symbolOffsetInPage = symbolOffsetInPage;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this
            || obj instanceof AbsoluteSymbolPosition that && pageNumber == that.pageNumber && symbolOffsetInPage == that.symbolOffsetInPage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, symbolOffsetInPage);
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
    public String toString() {
        return "(" + pageNumber + "," + symbolOffsetInPage + ")";
    }
}
