// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import java.util.function.LongConsumer;

final class NullIndexingStamp implements FileIndexingStamp {
    static final NullIndexingStamp INSTANCE = new NullIndexingStamp();

    private NullIndexingStamp() {
    }

    @Override
    public void store(LongConsumer storage) {
        storage.accept(FileIndexingStamps.NULL_INDEXING_STAMP);
    }

    @Override
    public boolean isSame(long i) {
        return false;
    }

    @Override
    public IsFileChangedResult isFileChanged(long i) {
        return IsFileChangedResult.UNKNOWN;
    }
}
