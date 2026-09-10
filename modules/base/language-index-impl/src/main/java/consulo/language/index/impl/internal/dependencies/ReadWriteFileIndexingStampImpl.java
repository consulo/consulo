// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import java.util.function.LongConsumer;

public record ReadWriteFileIndexingStampImpl(long stamp, boolean forceCheckingForOutdatedIndexesUsingFileModCount)
    implements FileIndexingStamp {

    public ReadWriteFileIndexingStampImpl(long stamp) {
        this(stamp, false);
    }

    public static FileIndexingStamp create(int requestId, int fileStamp, boolean expectIndexesWereNotRemoved) {
        return new ReadWriteFileIndexingStampImpl(FileIndexingStamps.withIndexingRequestId(fileStamp, requestId),
            expectIndexesWereNotRemoved);
    }

    @Override
    public void store(LongConsumer storage) {
        storage.accept(stamp);
    }

    @Override
    public boolean isSame(long i) {
        return i != FileIndexingStamps.NULL_INDEXING_STAMP && i == stamp;
    }

    @Override
    public IsFileChangedResult isFileChanged(long i) {
        return forceCheckingForOutdatedIndexesUsingFileModCount ? FileIndexingStamps.isFileChanged(i, stamp) : IsFileChangedResult.UNKNOWN;
    }
}
