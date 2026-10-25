// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

/**
 * Represent global block action.
 * <p>
 * In contrast to editor caret handlers, these actions are not bound to particular {@link consulo.codeEditor.Editor} instance
 * and works even for collapsed blocks.
 */
public abstract class CombinedGlobalBlockNavigationAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    protected CombinedGlobalBlockNavigationAction(LocalizeValue text) {
        super(text);
    }

    @Override
    public final void update(AnActionEvent e) {
        CombinedDiffViewer combinedDiffViewer = e.getData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER);

        e.getPresentation().setEnabledAndVisible(combinedDiffViewer != null);
    }
}
