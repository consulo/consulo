// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import consulo.ui.annotation.RequiredUIAccess;

public interface CombinedSelectableDiffBlock<ID extends CombinedBlockId> extends CombinedDiffBlock<ID> {
    @RequiredUIAccess
    void setSelected(boolean selected);
}
