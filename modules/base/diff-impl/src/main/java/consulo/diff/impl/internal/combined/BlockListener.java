// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;

import java.util.Collection;
import java.util.EventListener;

public interface BlockListener extends EventListener {
    void blocksHidden(Collection<CombinedBlockId> blockIds);

    void blocksVisible(Collection<CombinedBlockId> blockIds);
}
