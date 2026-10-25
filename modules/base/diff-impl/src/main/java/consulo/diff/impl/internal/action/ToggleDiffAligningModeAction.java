// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.FrameDiffTool;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;
import consulo.diff.impl.internal.viewer.simple.SimpleDiffViewer;
import consulo.diff.localize.DiffLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

@ActionImpl(id = "Vcs.Diff.ToggleDiffAligningMode")
public class ToggleDiffAligningModeAction extends ToggleAction implements DumbAware {
    public ToggleDiffAligningModeAction() {
        super(DiffLocalize.actionToggleDiffAligningModeText(), DiffLocalize.actionToggleDiffAligningModeDescription(), null);
    }

    @Override
    public void update(AnActionEvent e) {
        FrameDiffTool.DiffViewer viewer = e.getData(DiffDataKeys.DIFF_VIEWER);
        Project project = e.getData(Project.KEY);

        boolean available = project != null
            && viewer instanceof SimpleDiffViewer simpleDiffViewer
            && simpleDiffViewer.getRequest().getUserData(DiffUserDataKeys.ALIGNED_TWO_SIDED_DIFF) == null
            && simpleDiffViewer.getContext().getUserData(DiffUserDataKeys.ALIGNED_TWO_SIDED_DIFF) == null;
        if (!available) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        super.update(e);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        DiffContext diffContext = e.getData(DiffDataKeys.DIFF_CONTEXT);
        return diffContext != null && TextDiffViewerUtil.getTextSettings(diffContext).isEnableAligningChangesMode();
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        DiffContext diffContext = e.getData(DiffDataKeys.DIFF_CONTEXT);
        if (diffContext == null) {
            return;
        }
        TextDiffViewerUtil.getTextSettings(diffContext).setEnableAligningChangesMode(state);
        if (e.getData(DiffDataKeys.DIFF_VIEWER) instanceof SimpleDiffViewer viewer) {
            viewer.rediff();
        }
    }
}
