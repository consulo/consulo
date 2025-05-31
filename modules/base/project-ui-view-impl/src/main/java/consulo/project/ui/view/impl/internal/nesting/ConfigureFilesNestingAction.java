// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.ui.view.impl.internal.nesting;


import consulo.annotation.component.ActionImpl;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.internal.ProjectViewSharedSettings;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ActionImpl(id = "ProjectView.FileNesting")
public class ConfigureFilesNestingAction extends DumbAwareAction {
    private final Provider<ProjectViewSharedSettings> myProjectViewSharedSettings;

    @Inject
    public ConfigureFilesNestingAction(Provider<ProjectViewSharedSettings> projectViewSharedSettings) {
        super(ProjectUIViewLocalize.actionProjectviewFilenestingText(), ProjectUIViewLocalize.actionProjectviewFilenestingDescription());
        myProjectViewSharedSettings = projectViewSharedSettings;
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        if (project == null) {
            return;
        }

        ProjectView view = ProjectView.getInstance(project);

        ProjectViewSharedSettings projectViewSharedSettings = myProjectViewSharedSettings.get();

        FileNestingInProjectViewDialog dialog = new FileNestingInProjectViewDialog(project);
        dialog.reset(projectViewSharedSettings.getViewOption(ShowNestedProjectViewPaneOptionProvider.SHOW_NESTED_FILES_KEY));

        if (dialog.showAndGet()) {
            dialog.apply(v -> {
                projectViewSharedSettings.setViewOption(
                    ShowNestedProjectViewPaneOptionProvider.SHOW_NESTED_FILES_KEY,
                    v
                );

                ProjectViewPane pane = view.getCurrentProjectViewPane();
                if (pane != null) {
                    pane.putUserData(ShowNestedProjectViewPaneOptionProvider.SHOW_NESTED_FILES_KEY, v);

                    pane.updateFromRoot(true);
                }
            });
        }
    }
}
