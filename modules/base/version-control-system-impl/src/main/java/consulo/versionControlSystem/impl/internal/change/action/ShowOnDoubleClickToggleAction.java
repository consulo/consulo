// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.commit.CommitModeManager;

public abstract class ShowOnDoubleClickToggleAction extends DumbAwareToggleAction {
    private final boolean myEditorPreview;

    protected ShowOnDoubleClickToggleAction(LocalizeValue text, LocalizeValue description, boolean editorPreview) {
        super(text, description, null);
        myEditorPreview = editorPreview;
    }

    public static boolean isShowDiffOnDoubleClick(Project project) {
        VcsApplicationSettings settings = VcsApplicationSettings.getInstance();
        if (CommitModeManager.getInstance(project).isCommitToolWindowEnabled()) {
            return settings.SHOW_EDITOR_PREVIEW_ON_DOUBLE_CLICK;
        }
        else {
            return settings.SHOW_DIFF_ON_DOUBLE_CLICK;
        }
    }

    @Override
    public void update(AnActionEvent e) {
        boolean enabled = e.getData(Project.KEY) != null;
        e.getPresentation().setEnabledAndVisible(enabled);
        if (enabled) {
            super.update(e);
        }
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        return project != null && isShowDiffOnDoubleClick(project) == myEditorPreview;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        boolean newState = e.isFromContextMenu() || state ? myEditorPreview : !myEditorPreview;

        VcsApplicationSettings settings = VcsApplicationSettings.getInstance();
        if (CommitModeManager.getInstance(project).isCommitToolWindowEnabled()) {
            settings.SHOW_EDITOR_PREVIEW_ON_DOUBLE_CLICK = newState;
        }
        else {
            settings.SHOW_DIFF_ON_DOUBLE_CLICK = newState;
        }
    }
}
