// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class ScanningIterators implements ScanningParameters {
    private final String myIndexingReason;
    private final @Nullable List<IndexableFilesIterator> myPredefinedIndexableFilesIterators;
    private final ScanningType myScanningType;

    public ScanningIterators(String indexingReason) {
        this(indexingReason, null);
    }

    public ScanningIterators(String indexingReason, @Nullable List<IndexableFilesIterator> predefinedIndexableFilesIterators) {
        this(
            indexingReason,
            predefinedIndexableFilesIterators,
            predefinedIndexableFilesIterators == null ? ScanningType.FULL : ScanningType.PARTIAL
        );
    }

    public ScanningIterators(
        String indexingReason,
        @Nullable List<IndexableFilesIterator> predefinedIndexableFilesIterators,
        ScanningType scanningType
    ) {
        UnindexedFilesScanner.LOG.assertTrue(predefinedIndexableFilesIterators == null || !predefinedIndexableFilesIterators.isEmpty());
        myIndexingReason = indexingReason;
        myPredefinedIndexableFilesIterators = predefinedIndexableFilesIterators;
        myScanningType = scanningType;
    }

    String getIndexingReason() {
        return myIndexingReason;
    }

    @Nullable List<IndexableFilesIterator> getPredefinedIndexableFilesIterators() {
        return myPredefinedIndexableFilesIterators;
    }

    ScanningType getScanningType() {
        return myScanningType;
    }

    boolean isFullIndexUpdate() {
        return myPredefinedIndexableFilesIterators == null;
    }

    @Override
    public String toString() {
        return "ScanningIterators[" + myIndexingReason + ", " + myScanningType +
            (myPredefinedIndexableFilesIterators == null ? "" : ", " + myPredefinedIndexableFilesIterators.size() + " iterators") + "]";
    }
}
