// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedPathBlockId;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

public class CombinedSimpleDiffBlockFactory implements CombinedDiffBlockFactory<CombinedPathBlockId> {
    @RequiredUIAccess
    @Override
    public CombinedCollapsibleDiffBlock<CombinedPathBlockId> createBlock(Project project, CombinedDiffBlockContent content, boolean isCollapsed) {
        CombinedPathBlockId blockId = (CombinedPathBlockId) content.getBlockId();
        return new CombinedSimpleDiffBlock(
            project,
            blockId,
            content.getViewer().getComponent(),
            content.getViewer() instanceof CombinedDiffLoadingBlock,
            isCollapsed
        );
    }
}
