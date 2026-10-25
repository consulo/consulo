// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

public interface CombinedDiffBlockFactory<ID extends CombinedBlockId> {
    @RequiredUIAccess
    CombinedDiffBlock<ID> createBlock(Project project, CombinedDiffBlockContent content, boolean isCollapsed);
}
