// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.action;

import consulo.application.dumb.DumbAware;
import consulo.execution.RunManager;
import consulo.execution.localize.ExecutionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public class RunCurrentFileAction extends AnAction implements DumbAware {

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        e.getPresentation().setTextValue(ExecutionLocalize.runConfigurationsComboRunCurrentFileItemInDropdown());
        e.getPresentation().setDescriptionValue(ExecutionLocalize.runConfigurationsComboRunCurrentFileDescription());
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    @Nonnull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        RunManager.getInstance(project).setSelectedConfiguration(null);
        RunConfigurationsComboBoxAction.updatePresentation(null, null, project, e.getPresentation(), e.getPlace());
    }
}