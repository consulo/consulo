// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.EventListener;

public interface CombinedDiffBlockListener extends EventListener {
    @RequiredUIAccess
    void onCollapseStateChanged(CombinedBlockId id, boolean collapseState);
}
