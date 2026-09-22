// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.internal.largeFileEditor;

import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class SearchResult {
    public static final Key<SearchResult> KEY = new Key<>("lfe.SearchResult");

    public final Position startPosition;
    public final Position endPosition;
    public final String contextPrefix;
    public final String foundString;
    public final String contextPostfix;

    public SearchResult(
        long startPageNumber,
        int startOffsetInPage,
        long endPageNumber,
        int endOffsetInPage,
        @Nullable String contextPrefix,
        @Nullable String foundString,
        @Nullable String contextPostfix
    ) {
        startPosition = new Position(startPageNumber, startOffsetInPage);
        endPosition = new Position(endPageNumber, endOffsetInPage);
        this.contextPrefix = contextPrefix == null ? "" : contextPrefix;
        this.foundString = foundString == null ? "" : foundString;
        this.contextPostfix = contextPostfix == null ? "" : contextPostfix;
    }

    @Override
    public String toString() {
        return String.format("p%ds%d-p%ds%d: pref{%s},orig{%s},post{%s}",
            startPosition.pageNumber, startPosition.symbolOffsetInPage,
            endPosition.pageNumber, endPosition.symbolOffsetInPage,
            contextPrefix, foundString, contextPostfix
        );
    }

    @Override
    public boolean equals(@Nullable Object target) {
        if (this == target) {
            return true;
        }

        return target instanceof SearchResult that
            && startPosition.equals(that.startPosition)
            && endPosition.equals(that.endPosition)
            && contextPrefix.equals(that.contextPrefix)
            && foundString.equals(that.foundString)
            && contextPostfix.equals(that.contextPostfix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPosition, endPosition, contextPrefix, foundString, contextPostfix);
    }
}
