// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

/**
 * Holder for the top level declarations of JetBrains {@code FileIndexingStamp.kt}.
 * {@code IndexingRequestIdAndFileModCount} is a {@code long}, {@code FileModCount} is an {@code int}.
 */
public final class FileIndexingStamps {
    static final long NULL_INDEXING_STAMP = 0;

    public static int toFileModCount(long indexingRequestIdAndFileModCount) {
        return (int)indexingRequestIdAndFileModCount;
    }

    public static long withIndexingRequestId(int fileModCount, int indexingRequestId) {
        // https://stackoverflow.com/a/12772968 FileModCount shouldn't be negative but let's combine numbers properly
        return ((long)indexingRequestId << 32) | (0xffffffffL & fileModCount);
    }

    static IsFileChangedResult isFileChanged(long i, long stamp) {
        if (i == NULL_INDEXING_STAMP) {
            return IsFileChangedResult.UNKNOWN;
        }
        if (i == stamp) {
            return IsFileChangedResult.NO;
        }
        return IsFileChangedResult.YES;
    }

    private FileIndexingStamps() {
    }
}
