// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.diff.DiffTool;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ComboBoxAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class DiffToolChooser extends ComboBoxAction implements DumbAware {
    private final @Nullable Project myProject;

    public DiffToolChooser(@Nullable Project project) {
        myProject = project;
        setEnabledInModalContext(true);
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        DiffTool activeTool = getActiveTool();
        presentation.setText(activeTool.getName());

        if (getForcedDiffTool() != null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        for (DiffTool tool : getTools()) {
            if (tool != activeTool) {
                presentation.setEnabledAndVisible(true);
                return;
            }
        }

        presentation.setEnabledAndVisible(false);
    }

    @Override
    protected DefaultActionGroup createPopupActionGroup(DataContext context) {
        DefaultActionGroup group = new DefaultActionGroup();
        for (DiffTool tool : getTools()) {
            group.add(createSelectAction(tool));
        }
        return group;
    }

    public AnAction createSelectAction(DiffTool tool) {
        return new SelectToolAction(tool);
    }

    @RequiredUIAccess
    public abstract void onSelected(@Nullable Project project, DiffTool diffTool);

    public abstract List<? extends DiffTool> getTools();

    public abstract DiffTool getActiveTool();

    public abstract @Nullable DiffTool getForcedDiffTool();

    private class SelectToolAction extends AnAction implements DumbAware {
        private final DiffTool myDiffTool;

        private SelectToolAction(DiffTool tool) {
            super(tool.getName());
            setEnabledInModalContext(true);
            myDiffTool = tool;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            onSelected(myProject, myDiffTool);
        }
    }
}
