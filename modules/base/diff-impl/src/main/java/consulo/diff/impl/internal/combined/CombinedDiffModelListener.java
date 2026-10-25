// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import consulo.diff.request.DiffRequest;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.EventListener;
import java.util.Map;

public interface CombinedDiffModelListener extends EventListener {
    @RequiredUIAccess
    void onModelReset();

    @RequiredUIAccess
    void onRequestsLoaded(CombinedBlockId blockId, DiffRequest request);

    @RequiredUIAccess
    void onRequestContentsUnloaded(Map<CombinedBlockId, DiffRequest> requests);
}
