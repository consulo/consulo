// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffViewerVirtualFile;
import consulo.diff.combined.CombinedDiffKeys;
import consulo.diff.combined.CombinedDiffToggle;
import consulo.diff.impl.internal.editor.DiffRequestProcessorEditor;
import consulo.diff.localize.DiffLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.ToggleAction;
import org.jspecify.annotations.Nullable;

@ActionImpl(id = "Vcs.Diff.EnableDisableCombinedDiff", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_TAB_POPUP)))
public class CombinedDiffToggleAction extends ToggleAction implements DumbAware {
    public CombinedDiffToggleAction() {
        super(DiffLocalize.combinedDiffEnableAction(), LocalizeValue.empty(), null);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        CombinedDiffToggle diffModeToggle = getDiffModeToggle(e);
        e.getPresentation().setEnabledAndVisible(diffModeToggle != null);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        CombinedDiffToggle diffModeToggle = getDiffModeToggle(e);
        return diffModeToggle != null && diffModeToggle.isCombinedDiffEnabled();
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        CombinedDiffToggle diffModeToggle = getDiffModeToggle(e);
        if (diffModeToggle == null) {
            return;
        }
        diffModeToggle.setCombinedDiffEnabled(state);

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            DiffRequestProcessorEditor.reloadDiffEditorsForFiles(project, file -> file instanceof DiffViewerVirtualFile);
        }
    }

    private static @Nullable CombinedDiffToggle getDiffModeToggle(AnActionEvent e) {
        DiffContext context = e.getData(DiffDataKeys.DIFF_CONTEXT);
        if (context == null) {
            return null;
        }
        return context.getUserData(CombinedDiffKeys.COMBINED_DIFF_TOGGLE);
    }
}
