// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.FrameDiffTool;
import consulo.diff.combined.CombinedBlockId;

public final class CombinedDiffBlockContent {
    private final FrameDiffTool.DiffViewer myViewer;
    private final CombinedBlockId myBlockId;

    public CombinedDiffBlockContent(FrameDiffTool.DiffViewer viewer, CombinedBlockId blockId) {
        myViewer = viewer;
        myBlockId = blockId;
    }

    public FrameDiffTool.DiffViewer getViewer() {
        return myViewer;
    }

    public CombinedBlockId getBlockId() {
        return myBlockId;
    }
}
