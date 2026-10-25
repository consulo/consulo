// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

public interface CombinedCollapsibleDiffBlock<ID extends CombinedBlockId> extends CombinedSelectableDiffBlock<ID> {
    @RequiredUIAccess
    void setCollapsed(boolean collapsed);

    boolean isCollapsed();

    void addListener(CombinedDiffBlockListener listener, Disposable parentDisposable);
}
