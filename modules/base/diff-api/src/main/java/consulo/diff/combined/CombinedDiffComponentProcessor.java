// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.combined;

import consulo.diff.DiffEditorViewer;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface CombinedDiffComponentProcessor extends DiffEditorViewer {
    List<CombinedBlockProducer> getBlocks();

    /**
     * Updates current model with the new requests
     */
    @RequiredUIAccess
    void setBlocks(List<CombinedBlockProducer> requests);

    void cleanBlocks();

    @Nullable CombinedBlockProducer getCurrentBlock();
}
