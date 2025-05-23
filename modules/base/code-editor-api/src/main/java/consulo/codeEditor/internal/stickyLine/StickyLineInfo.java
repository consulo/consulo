// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.internal.stickyLine;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents information about sticky line collected by {@link StickyLinesCollector}.
 * <p>
 * See also {@link StickyLine}
 */
public record StickyLineInfo(int textOffset, int endOffset, @Nullable String debugText) {

    public StickyLineInfo(@Nonnull TextRange textRange) {
        this(textRange.getStartOffset(), textRange.getEndOffset(), null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StickyLineInfo info)) return false;
        return endOffset == info.endOffset && textOffset == info.textOffset;
    }

    @Override
    public int hashCode() {
        return textOffset + 31 * endOffset;
    }
}
