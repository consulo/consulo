/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.externalSystem.importing;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.persist.PersistentStateComponent;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.externalSystem.internal.ExternalSystemInternalHelper;
import consulo.externalSystem.service.ExternalSystemResolveProjectTask;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.service.ExternalSystemResolveProjectTaskFactory;
import consulo.externalSystem.service.project.ExternalProjectRefreshCallback;
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ProjectDataManager;
import consulo.externalSystem.service.setting.AbstractImportFromExternalSystemControl;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.ide.moduleImport.ModuleImportProvider;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.project.startup.StartupManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.ex.wizard.WizardStepValidationException;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2017-01-30
 */
public abstract class AbstractExternalModuleImportProvider<C extends AbstractImportFromExternalSystemControl> implements ModuleImportProvider<ExternalModuleImportContext<C>> {
    private static final Logger LOG = Logger.getInstance(AbstractExternalModuleImportProvider.class);

    @Nonnull
    private final ProjectDataManager myProjectDataManager;
    @Nonnull
    private final C myControl;
    @Nonnull
    private final ProjectSystemId myExternalSystemId;

    private DataNode<ProjectData> myExternalProjectNode;

    public AbstractExternalModuleImportProvider(
        @Nonnull ProjectDataManager projectDataManager,
        @Nonnull C control,
        @Nonnull ProjectSystemId externalSystemId
    ) {
        myProjectDataManager = projectDataManager;
        myControl = control;
        myExternalSystemId = externalSystemId;
    }

    @Nonnull
    public ProjectSystemId getExternalSystemId() {
        return myExternalSystemId;
    }

    protected abstract void doPrepare(@Nonnull ExternalModuleImportContext<C> context);

    protected abstract void beforeCommit(@Nonnull DataNode<ProjectData> dataNode, @Nonnull Project project);

    /**
     * Allows to adjust external project config file to use on the basis of the given value.
     * <p>
     * Example: a user might choose a directory which contains target config file and particular implementation expands
     * that to a particular file under the directory.
     *
     * @param file base external project config file
     * @return external project config file to use
     */
    @Nonnull
    protected abstract File getExternalProjectConfigToUse(@Nonnull File file);

    protected abstract void applyExtraSettings(@Nonnull ExternalModuleImportContext<C> context);

    @Nonnull
    public C getControl() {
        return myControl;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return myExternalSystemId.getDisplayName();
    }

    @Override
    @RequiredReadAction
    public void process(
        @Nonnull ExternalModuleImportContext<C> context,
        @Nonnull Project project,
        @Nonnull ModifiableModuleModel model,
        @Nonnull Consumer<Module> newModuleConsumer
    ) {
        project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);
        DataNode<ProjectData> externalProjectNode = getExternalProjectNode();
        if (externalProjectNode != null) {
            beforeCommit(externalProjectNode, project);
        }

        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
            AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(project, myExternalSystemId);
            ExternalProjectSettings projectSettings = getCurrentExternalProjectSettings();
            Set<ExternalProjectSettings> projects = new HashSet<>(systemSettings.getLinkedProjectsSettings());
            // add current importing project settings to linked projects settings or replace if similar already exist
            projects.remove(projectSettings);
            projects.add(projectSettings);

            systemSettings.copyFrom(myControl.getSystemSettings());
            systemSettings.setLinkedProjectsSettings(projects);

            Application application = Application.get();
            ExternalSystemInternalHelper internalHelper = application.getInstance(ExternalSystemInternalHelper.class);
            ExternalSystemResolveProjectTaskFactory taskFactory = application.getInstance(ExternalSystemResolveProjectTaskFactory.class);

            if (externalProjectNode != null) {
                internalHelper.ensureToolWindowInitialized(project, myExternalSystemId);
                ExternalSystemApiUtil.executeProjectChangeAction(new DisposeAwareProjectChange(project) {
                    @Override
                    @RequiredUIAccess
                    public void execute() {
                        ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring(() -> {
                            myProjectDataManager.importData(
                                externalProjectNode.getKey(),
                                Collections.singleton(externalProjectNode),
                                project,
                                true
                            );
                            myExternalProjectNode = null;
                        });
                    }
                });

                Runnable resolveDependenciesTask = () -> {
                    LocalizeValue progressText = ExternalSystemLocalize.progressResolveLibraries(myExternalSystemId.getDisplayName());
                    ProgressManager.getInstance().run(new Task.Backgroundable(project, progressText.get(), false) {
                        @Override
                        public void run(@Nonnull ProgressIndicator indicator) {
                            if (project.isDisposed()) {
                                return;
                            }
                            ExternalSystemResolveProjectTask task = taskFactory.createResolveProjectTask(
                                myExternalSystemId,
                                project,
                                projectSettings.getExternalProjectPath(),
                                false
                            );
                            task.execute(indicator, ExternalSystemTaskNotificationListener.EP_NAME.getExtensions());
                            DataNode<ProjectData> projectWithResolvedLibraries = task.getExternalProject();
                            if (projectWithResolvedLibraries == null) {
                                return;
                            }

                            setupLibraries(projectWithResolvedLibraries, project);
                        }
                    });
                };
                UIUtil.invokeLaterIfNeeded(resolveDependenciesTask);
            }
        });
    }

    @Nullable
    public DataNode<ProjectData> getExternalProjectNode() {
        return myExternalProjectNode;
    }

    /**
     * Asks current builder to ensure that target external project is defined.
     *
     * @param context current wizard context
     * @throws WizardStepValidationException if gradle project is not defined and can't be constructed
     */
    @SuppressWarnings("unchecked")
    public void ensureProjectIsDefined(@Nonnull ExternalModuleImportContext<C> context) throws WizardStepValidationException {
        String externalSystemName = myExternalSystemId.getReadableName().get();
        File projectFile = getProjectFile();
        if (projectFile == null) {
            throw new WizardStepValidationException(ExternalSystemLocalize.errorProjectUndefined().get());
        }
        projectFile = getExternalProjectConfigToUse(projectFile);
        SimpleReference<WizardStepValidationException> error = new SimpleReference<>();
        ExternalProjectRefreshCallback callback = new ExternalProjectRefreshCallback() {
            @Override
            public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
                myExternalProjectNode = externalProject;
            }

            @Override
            public void onFailure(@Nonnull String errorMessage, @Nullable String errorDetails) {
                if (!StringUtil.isEmpty(errorDetails)) {
                    LOG.warn(errorDetails);
                }
                error.set(new WizardStepValidationException(ExternalSystemLocalize.errorResolveWithReason(errorMessage).get()));
            }
        };

        Project project = getContextOrDefaultProject(context);
        File finalProjectFile = projectFile;
        String externalProjectPath = FileUtil.toCanonicalPath(finalProjectFile.getAbsolutePath());
        SimpleReference<WizardStepValidationException> exRef = new SimpleReference<>();
        executeAndRestoreDefaultProjectSettings(project, () -> {
            try {
                ExternalSystemProjectRefresher refresher = ExternalSystemProjectRefresher.getInstance();

                refresher.refreshProject(
                    project,
                    myExternalSystemId,
                    externalProjectPath,
                    callback,
                    true,
                    ProgressExecutionMode.MODAL_SYNC
                );
            }
            catch (IllegalArgumentException e) {
                exRef.set(new WizardStepValidationException(ExternalSystemLocalize.errorCannotParseProject(externalSystemName).get()));
            }
        });
        WizardStepValidationException ex = exRef.get();
        if (ex != null) {
            throw ex;
        }
        if (myExternalProjectNode == null) {
            WizardStepValidationException exception = error.get();
            if (exception != null) {
                throw exception;
            }
        }
        else {
            applyProjectSettings(context);
        }
    }

    /**
     * Applies external system-specific settings like project files location etc to the given context.
     *
     * @param context storage for the project/module settings.
     */
    public void applyProjectSettings(@Nonnull ExternalModuleImportContext<C> context) {
        if (myExternalProjectNode == null) {
            assert false;
            return;
        }
        context.setName(myExternalProjectNode.getData().getInternalName());
        context.setPath(myExternalProjectNode.getData().getIdeProjectFileDirectoryPath());
        applyExtraSettings(context);
    }

    @Nullable
    private File getProjectFile() {
        String path = myControl.getProjectSettings().getExternalProjectPath();
        return path == null ? null : new File(path);
    }

    @SuppressWarnings("unchecked")
    private void executeAndRestoreDefaultProjectSettings(@Nonnull Project project, @Nonnull Runnable task) {
        if (!project.isDefault()) {
            task.run();
            return;
        }

        AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(project, myExternalSystemId);
        Object systemStateToRestore = null;
        if (systemSettings instanceof PersistentStateComponent persistentStateComponent) {
            systemStateToRestore = persistentStateComponent.getState();
        }
        systemSettings.copyFrom(myControl.getSystemSettings());
        Collection projectSettingsToRestore = systemSettings.getLinkedProjectsSettings();
        systemSettings.setLinkedProjectsSettings(Collections.singleton(getCurrentExternalProjectSettings()));
        try {
            task.run();
        }
        finally {
            if (systemStateToRestore != null) {
                ((PersistentStateComponent)systemSettings).loadState(systemStateToRestore);
            }
            else {
                systemSettings.setLinkedProjectsSettings(projectSettingsToRestore);
            }
        }
    }

    @Nonnull
    private ExternalProjectSettings getCurrentExternalProjectSettings() {
        ExternalProjectSettings result = myControl.getProjectSettings().clone();
        File externalProjectConfigFile = getExternalProjectConfigToUse(new File(result.getExternalProjectPath()));
        String linkedProjectPath = FileUtil.toCanonicalPath(externalProjectConfigFile.getPath());
        assert linkedProjectPath != null;
        result.setExternalProjectPath(linkedProjectPath);
        return result;
    }

    /**
     * The whole import sequence looks like below:
     * <p>
     * <pre>
     * <ol>
     *   <li>Get project view from the gradle tooling api without resolving dependencies (downloading libraries);</li>
     *   <li>Allow to adjust project settings before importing;</li>
     *   <li>Create IJ project and modules;</li>
     *   <li>Ask gradle tooling api to resolve library dependencies (download the if necessary);</li>
     *   <li>Configure libraries used by the gradle project at intellij;</li>
     *   <li>Configure library dependencies;</li>
     * </ol>
     * </pre>
     * <p>
     *
     * @param projectWithResolvedLibraries gradle project with resolved libraries (libraries have already been downloaded and
     *                                     are available at file system under gradle service directory)
     * @param project                      current intellij project which should be configured by libraries and module library
     *                                     dependencies information available at the given gradle project
     */
    private void setupLibraries(@Nonnull DataNode<ProjectData> projectWithResolvedLibraries, Project project) {
        ExternalSystemApiUtil.executeProjectChangeAction(new DisposeAwareProjectChange(project) {
            @Override
            @RequiredUIAccess
            public void execute() {
                ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring(() -> {
                    if (ExternalSystemApiUtil.isNewProjectConstruction()) {
                        // Clean existing libraries (if any).
                        LibraryTable projectLibraryTable = ProjectLibraryTable.getInstance(project);
                        if (projectLibraryTable == null) {
                            LOG.warn(
                                "Can't resolve external dependencies of the target gradle project (" + project + ")." +
                                    " Reason: project library table is undefined"
                            );
                            return;
                        }
                        LibraryTable.ModifiableModel model = projectLibraryTable.getModifiableModel();
                        try {
                            for (Library library : model.getLibraries()) {
                                model.removeLibrary(library);
                            }
                        }
                        finally {
                            model.commit();
                        }
                    }

                    // Register libraries.
                    myProjectDataManager.importData(Collections.<DataNode<?>>singletonList(projectWithResolvedLibraries), project, false);
                });
            }
        });
    }

    /**
     * Allows to get {@link Project} instance to use. Basically, there are two alternatives -
     * {@link ExternalModuleImportContext#getProject() project from the current wizard context} and
     * {@link ProjectManager#getDefaultProject() default project}.
     *
     * @param context current wizard context
     * @return {@link Project} instance to use
     */
    @Nonnull
    public Project getContextOrDefaultProject(@Nonnull ExternalModuleImportContext<C> context) {
        Project result = context.getProject();
        if (result == null) {
            result = ProjectManager.getInstance().getDefaultProject();
        }
        return result;
    }

    @Override
    public void buildSteps(
        @Nonnull Consumer<WizardStep<ExternalModuleImportContext<C>>> consumer,
        @Nonnull ExternalModuleImportContext<C> context
    ) {
        consumer.accept(new SelectExternalProjectStep<>());
    }

    @Nonnull
    @Override
    public ExternalModuleImportContext<C> createContext(@Nullable Project project) {
        return new ExternalModuleImportContext<>(project, this);
    }

    @Override
    public String getPathToBeImported(@Nonnull VirtualFile file) {
        return file.getPath();
    }
}
