// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class AbsoluteEditorPosition {
    long pageNumber;
    int verticalScrollOffset;

    public AbsoluteEditorPosition(long pageNumber, int verticalScrollOffset) {
        this.pageNumber = pageNumber;
        this.verticalScrollOffset = verticalScrollOffset;
    }

    void set(long newPageNumber, int newVerticalScrollBarOffset) {
        this.pageNumber = newPageNumber;
        this.verticalScrollOffset = newVerticalScrollBarOffset;
    }

    void copyFrom(AbsoluteEditorPosition from) {
        pageNumber = from.pageNumber;
        verticalScrollOffset = from.verticalScrollOffset;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        return obj instanceof AbsoluteEditorPosition that
            && pageNumber == that.pageNumber
            && verticalScrollOffset == that.verticalScrollOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, verticalScrollOffset);
    }
}
