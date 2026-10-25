// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.DiffContext;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

final class CombinedDiffContext extends DiffContext {
    private final Project myProject;

    CombinedDiffContext(Project project) {
        myProject = project;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean isFocused() {
        CombinedDiffMainUI mainUi = getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_MAIN_UI);
        return mainUi != null && mainUi.isFocusedInWindow();
    }

    @Override
    public boolean isWindowFocused() {
        CombinedDiffMainUI mainUi = getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_MAIN_UI);
        return mainUi != null && mainUi.isWindowFocused();
    }

    @RequiredUIAccess
    @Override
    public void requestFocus() {
        CombinedDiffMainUI mainUi = getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_MAIN_UI);
        if (mainUi != null) {
            mainUi.requestFocusInWindow();
        }
    }
}
