// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import java.util.function.LongConsumer;

public interface FileIndexingStamp {
    /**
     * Number representing IndexingStamp. Use {@link #isFileChanged} to compare this number to any other stamps.
     * Signature made complicated intentionally to make it harder to obtain int and compare it with some other int
     * obtained from another FileIndexingStamp. Comparison should onlu be made via {@link #isFileChanged}
     */
    void store(LongConsumer storage);

    /**
     * Compares this stamp to Long value (request id, file mod count) obtained via {@link #store}
     */
    boolean isSame(long i);

    /**
     * Compares this stamp to Long value (request id, file mod count) obtained via {@link #store}
     */
    IsFileChangedResult isFileChanged(long i);
}
