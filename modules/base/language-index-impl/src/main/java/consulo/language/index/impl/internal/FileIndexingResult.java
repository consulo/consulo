// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

/**
 * Carries the results of indexing given file by all applicable indexers.
 */
public final class FileIndexingResult {
    /**
     * How to apply changes to the indexes.
     * Currently, there are either apply-in-parallel (or erase if needed), or apply-in-same-thread outside read lock.
     */
    public enum ApplicationMode {
        SameThreadOutsideReadLock,
        AnotherThread
    }

    private FileIndexingResult() {
    }
}
