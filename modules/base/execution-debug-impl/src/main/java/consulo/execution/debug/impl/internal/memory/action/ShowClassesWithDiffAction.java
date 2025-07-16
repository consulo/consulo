// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.memory.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.debug.memory.MemoryViewManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "MemoryView.ShowOnlyWithDiff")
public class ShowClassesWithDiffAction extends ToggleAction implements DumbAware {
    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return MemoryViewManager.getInstance().isNeedShowDiffOnly();
    }

    @Override
    @Nonnull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        Project project = e.getData(Project.KEY);
        if (project != null) {
            MemoryViewManager.getInstance().setShowDiffOnly(state);
        }
    }
}
