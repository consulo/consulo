// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;

public sealed interface IndexingStampStorage
    extends Closeable
    permits IndexingStampStorageOverRegularAttributes, IndexingStampStorageOverFastAttributes {

    void writeTimestamps(int fileId, TimestampsImmutable timestamps) throws IOException;

    @Nullable TimestampsImmutable readTimestamps(int fileId);
}
