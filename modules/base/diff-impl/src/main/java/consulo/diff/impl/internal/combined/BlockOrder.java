// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import org.jspecify.annotations.Nullable;

public interface BlockOrder {
    Iterable<CombinedBlockId> iterateBlocks();

    int indexOf(CombinedBlockId blockId);

    @Nullable CombinedBlockId getOrNull(int index);

    int getBlocksCount();
}
