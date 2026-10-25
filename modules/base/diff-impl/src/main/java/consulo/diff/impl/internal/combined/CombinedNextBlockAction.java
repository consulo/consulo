// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.DiffContext;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.util.ActionUtil;
import org.jspecify.annotations.Nullable;

class CombinedNextBlockAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private final DiffContext myContext;

    CombinedNextBlockAction(DiffContext context) {
        myContext = context;
        ActionUtil.copyFrom(this, "Diff.NextChange");
    }

    @Override
    public void update(AnActionEvent e) {
        if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
            e.getPresentation().setEnabledAndVisible(true);
            return;
        }

        CombinedDiffNavigation navigation = getCombinedDiffNavigation();
        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(navigation != null && navigation.canGoNextBlock());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        CombinedDiffNavigation navigation = getCombinedDiffNavigation();
        if (navigation == null || !navigation.canGoNextBlock()) {
            return;
        }

        navigation.goNextBlock();
    }

    private @Nullable CombinedDiffNavigation getCombinedDiffNavigation() {
        return myContext.getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY);
    }
}
