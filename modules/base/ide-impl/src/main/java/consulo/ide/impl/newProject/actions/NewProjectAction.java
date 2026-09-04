/*
 * Copyright 2013-2016 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.newProject.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.ide.impl.module.creation.NewProjectDialog;
import consulo.ide.impl.module.creation.NewProjectWizardData;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.creation.NewModuleWizardContext;
import consulo.module.creation.NewOrImportModuleUtil;
import consulo.module.creation.scratch.NewModuleBuilderProcessor;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectOpenService;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 */
@ActionImpl(id = "NewProject")
public class NewProjectAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(NewProjectAction.class);

    @Inject
    public NewProjectAction() {
        super(ActionLocalize.actionNewprojectText(), ActionLocalize.actionNewprojectDescription());
    }

    public NewProjectAction(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        NewProjectDialog dialog = new NewProjectDialog(project, null);

        if (dialog.showAndGet()) {
            generateProject(project, dialog.getProjectPanel());
        }
    }

    @RequiredUIAccess
    protected static void generateProject(Project project, NewProjectWizardData projectPanel) {
        NewModuleWizardContext context = projectPanel.getWizardContext();
        NewModuleBuilderProcessor<NewModuleWizardContext> processor = projectPanel.getProcessor();
        if (processor == null || context == null) {
            LOG.error("Impossible situation. Calling generate project with null data: " + processor + "/" + context);
            return;
        }

        generateProjectAsync(projectPanel);
    }

    @RequiredUIAccess
    private static void generateProjectAsync(NewProjectWizardData panel) {
        // leave current step
        panel.finish();

        NewModuleWizardContext context = panel.getWizardContext();

        File location = new File(context.getPath());
        int childCount = location.exists() ? location.list().length : 0;
        if (!location.exists() && !location.mkdirs()) {
            MessageBoxes.okError(LocalizeValue.localizeTODO("Cannot create directory '" + location + "'")).showAsync();
            return;
        }

        WriteAction.run(() -> LocalFileSystem.getInstance().refreshIoFiles(List.of(location), false, true, null));

        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByIoFile(location);
        if (baseDir == null) {
            MessageBoxes.okError(LocalizeValue.localizeTODO("Directory '" + location + "' is not resolved.")).showAsync();
            return;
        }

        baseDir.refresh(false, true);

        if (childCount > 0) {
            MessageBoxes.yesNo()
                .text(LocalizeValue.localizeTODO("The directory '" + location + "' is not empty. Continue?"))
                .showAsync()
                .whenComplete((confirmed, error) -> {
                    if (Boolean.TRUE.equals(confirmed)) {
                        openProject(panel, location, baseDir);
                    }
                });
            return;
        }

        openProject(panel, location, baseDir);
    }

    @RequiredUIAccess
    private static void openProject(NewProjectWizardData panel, File location, VirtualFile baseDir) {
        RecentProjectsManager.getInstance().setLastProjectCreationLocation(location.getParent());

        UIAccess uiAccess = UIAccess.current();
        ProjectOpenContext openContext = new ProjectOpenContext();
        openContext.putUserData(ProjectOpenContext.FORCE_OPEN_IN_NEW_FRAME, true);
        Application.get().getInstance(ProjectOpenService.class)
            .openProjectAsync(baseDir.toNioPath(), uiAccess, openContext)
            .whenComplete((openedProject, error) -> {
                if (error == null && openedProject != null) {
                    uiAccess.give(() -> NewOrImportModuleUtil.doCreate(
                        panel.getProcessor(),
                        panel.getWizardContext(),
                        openedProject,
                        baseDir
                    ));
                }
            });
    }
}
