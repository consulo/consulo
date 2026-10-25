// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.annotation.component.ActionImpl;
import consulo.diff.localize.DiffLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

@ActionImpl(id = "Vcs.CombinedDiff.CaretToNextBlock")
public class CombinedCaretToNextBlockAction extends CombinedGlobalBlockNavigationAction {
    public CombinedCaretToNextBlockAction() {
        super(DiffLocalize.combinedDiffNextFile());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        CombinedDiffViewer viewer = e.getRequiredData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER);
        if (viewer.canGoNextBlock()) {
            viewer.moveCaretToNextBlock();
        }
    }
}
