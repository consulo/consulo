// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.localize.DiffLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;

@ActionImpl(id = "Vcs.CombinedDiff.ToggleCollapseAllBlock", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_TAB_POPUP)))
public class CombinedToggleBlockCollapseAllAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public CombinedToggleBlockCollapseAllAction() {
        super(DiffLocalize.combinedDiffCollapseAllFiles());
    }

    @Override
    public void update(AnActionEvent e) {
        DiffContext context = e.getData(DiffDataKeys.DIFF_CONTEXT);
        CombinedDiffViewer combinedDiffViewer = context != null ? context.getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY) : null;
        e.getPresentation().setEnabledAndVisible(combinedDiffViewer != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DiffContext context = e.getData(DiffDataKeys.DIFF_CONTEXT);
        CombinedDiffViewer viewer = context != null ? context.getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY) : null;
        if (viewer == null) {
            return;
        }

        viewer.collapseAllBlocks();
    }
}
