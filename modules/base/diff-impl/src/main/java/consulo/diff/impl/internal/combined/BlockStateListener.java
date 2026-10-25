// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;

import java.util.EventListener;

@FunctionalInterface
public interface BlockStateListener extends EventListener {
    void onCurrentChanged(CombinedBlockId oldBlockId, CombinedBlockId newBlockId);
}
