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
import consulo.module.creation.impl.internal.NewProjectDialogDescriptor;
import consulo.module.creation.impl.internal.NewProjectWizardData;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.creation.NewModuleWizardContext;
import consulo.module.creation.NewOrImportModuleUtil;
import consulo.module.creation.scratch.NewModuleBuilderProcessor;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectOpenService;
import consulo.project.RecentProjectsManager;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.image.Image;
import consulo.platform.Platform;
import consulo.ui.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.stream.Stream;

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

        NewProjectDialogDescriptor descriptor = new NewProjectDialogDescriptor(null);

        DialogService dialogService = Application.get().getInstance(DialogService.class);
        Dialog dialog = project != null ? dialogService.build(project, descriptor) : dialogService.build(descriptor);

        dialog.showAsync().whenComplete((value, error) -> {
            if (error != null) {
                return;
            }

            generateProject(project, descriptor.getWizardData());
        });
    }

    @RequiredUIAccess
    public static void generateProject(Project project, NewProjectWizardData projectPanel) {
        NewModuleWizardContext context = projectPanel.getWizardContext();
        NewModuleBuilderProcessor<NewModuleWizardContext> processor = projectPanel.getProcessor();
        if (processor == null || context == null) {
            LOG.error("Impossible situation. Calling generate project with null data: " + processor + "/" + context);
            return;
        }

        generateProjectAsync(projectPanel);
    }

    /**
     * The location of the project, created on its machine.
     *
     * @param baseDir  the directory, null when it cannot be created or found
     * @param notEmpty the directory had files before
     */
    private record PreparedLocation(Path location, @Nullable VirtualFile baseDir, boolean notEmpty, @Nullable String error) {
    }

    @RequiredUIAccess
    private static void generateProjectAsync(NewProjectWizardData panel) {
        // leave current step
        panel.finish();

        NewModuleWizardContext context = panel.getWizardContext();
        Platform platform = context.getPlatform();

        Path location;
        try {
            // a path of the machine of the project - the local one, or a remote one
            location = platform.fs().getPath(context.getPath());
        }
        catch (InvalidPathException e) {
            MessageBoxes.okError(LocalizeValue.localizeTODO("Invalid project location '" + context.getPath() + "'")).showAsync();
            return;
        }

        CoroutineScope scope = CoroutineScope.of(Application.get().coroutineContext());
        scope.putCopyableUserData(UIAccess.KEY, UIAccess.current());

        Coroutine
            .first(CodeExecution.<Object, PreparedLocation>apply(ignored -> prepareLocation(location)))
            .then(UIAction.<PreparedLocation, PreparedLocation>apply(prepared -> {
                VirtualFile baseDir = prepared.baseDir();
                if (baseDir == null) {
                    MessageBoxes.okError(LocalizeValue.localizeTODO(prepared.error())).showAsync();
                }
                else if (prepared.notEmpty()) {
                    MessageBoxes.yesNo()
                        .text(LocalizeValue.localizeTODO("The directory '" + location + "' is not empty. Continue?"))
                        .showAsync()
                        .whenComplete((confirmed, error) -> {
                            if (Boolean.TRUE.equals(confirmed)) {
                                openProject(panel, platform, location, baseDir);
                            }
                        });
                }
                else {
                    openProject(panel, platform, location, baseDir);
                }
                return prepared;
            }))
            .runAsync(scope, ObjectUtil.NULL);
    }

    /**
     * Creates the directory where it is - not in the UI thread, as a remote machine answers by network.
     */
    private static PreparedLocation prepareLocation(Path location) {
        boolean notEmpty = false;
        try {
            if (Files.isDirectory(location)) {
                try (Stream<Path> children = Files.list(location)) {
                    notEmpty = children.findAny().isPresent();
                }
            }
            else {
                Files.createDirectories(location);
            }
        }
        catch (IOException e) {
            LOG.info(e);
            return new PreparedLocation(location, null, false, "Cannot create directory '" + location + "'");
        }

        VirtualFile baseDir = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(location);
        if (baseDir == null) {
            return new PreparedLocation(location, null, false, "Directory '" + location + "' is not resolved.");
        }
        baseDir.refresh(false, true);
        return new PreparedLocation(location, baseDir, notEmpty, null);
    }

    @RequiredUIAccess
    private static void openProject(NewProjectWizardData panel, Platform platform, Path location, VirtualFile baseDir) {
        Path parent = location.getParent();
        if (platform == Platform.current() && parent != null) {
            // the default location of the next new project - of this machine
            RecentProjectsManager.getInstance().setLastProjectCreationLocation(parent.toString());
        }

        UIAccess uiAccess = UIAccess.current();
        ProjectOpenContext openContext = new ProjectOpenContext();
        openContext.putUserData(ProjectOpenContext.FORCE_OPEN_IN_NEW_FRAME, true);
        Application.get().getInstance(ProjectOpenService.class)
            .openProjectAsync(location, uiAccess, openContext)
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
