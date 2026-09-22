// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.internal.largeFileEditor;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class Position {
    public long pageNumber;
    public int symbolOffsetInPage;

    public Position() {
        this.pageNumber = 0;
        this.symbolOffsetInPage = 0;
    }

    public Position(long pageNumber, int symbolOffsetInPage) {
        this.pageNumber = pageNumber;
        this.symbolOffsetInPage = symbolOffsetInPage;
    }

    public void reset(long pageNumber, int symbolNumber) {
        this.pageNumber = pageNumber;
        this.symbolOffsetInPage = symbolNumber;
    }

    @Override
    public boolean equals(@Nullable Object target) {
        return this == target
            || target instanceof Position that && pageNumber == that.pageNumber && symbolOffsetInPage == that.symbolOffsetInPage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, symbolOffsetInPage);
    }
}
