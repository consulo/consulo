/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.model.task.ExternalSystemTaskType;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.model.task.TaskCallback;
import consulo.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.externalSystem.importing.ImportSpec;
import consulo.ide.impl.idea.openapi.externalSystem.importing.ImportSpecBuilder;
import consulo.ide.impl.idea.openapi.externalSystem.service.ImportCanceledException;
import consulo.ide.impl.idea.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import consulo.ide.impl.idea.openapi.externalSystem.service.internal.ExternalSystemResolveProjectTask;
import consulo.ide.impl.idea.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import consulo.externalSystem.service.notification.NotificationSource;
import consulo.ide.impl.idea.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import consulo.ide.impl.idea.openapi.externalSystem.service.project.ProjectStructureHelper;
import consulo.ide.impl.idea.openapi.externalSystem.service.project.manage.ModuleDataService;
import consulo.ide.impl.idea.openapi.externalSystem.service.project.manage.ProjectDataManager;
import consulo.externalSystem.service.setting.ExternalSystemConfigLocator;
import consulo.ide.impl.idea.openapi.roots.impl.libraries.ProjectLibraryTableImpl;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.wm.ex.ToolWindowManagerEx;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtilRt;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.rmi.RemoteUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Denis Zhdanov
 * @since 4/22/13 9:36 AM
 */
public class ExternalSystemUtil {

  private static final Logger LOG = Logger.getInstance(ExternalSystemUtil.class);

  private ExternalSystemUtil() {
  }

  @Nullable
  public static VirtualFile refreshAndFindFileByIoFile(@Nonnull final File file) {
    final Application app = Application.get();
    if (!app.isDispatchThread()) {
      assert !((ApplicationEx)app).holdsReadLock();
    }
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }

  @Nullable
  public static VirtualFile findLocalFileByPath(String path) {
    VirtualFile result = StandardFileSystems.local().findFileByPath(path);
    if (result != null) return result;

    return !Application.get().isReadAccessAllowed() ? findLocalFileByPathUnderWriteAction(path) : findLocalFileByPathUnderReadAction(path);
  }

  @Nullable
  private static VirtualFile findLocalFileByPathUnderWriteAction(final String path) {
    return ExternalSystemApiUtil.doWriteAction(() -> StandardFileSystems.local().refreshAndFindFileByPath(path));
  }

  @Nullable
  private static VirtualFile findLocalFileByPathUnderReadAction(final String path) {
    return ReadAction.compute(() -> StandardFileSystems.local().findFileByPath(path));
  }

  public static void ensureToolWindowInitialized(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    ToolWindowManager manager = ToolWindowManager.getInstance(project);
    if (!(manager instanceof ToolWindowManagerEx)) {
      return;
    }
    ToolWindowManagerEx managerEx = (ToolWindowManagerEx)manager;
    String id = externalSystemId.getReadableName();
    ToolWindow window = manager.getToolWindow(id);
    if (window != null) {
      return;
    }

    for (ToolWindowFactory toolWindowFactory : project.getApplication().getExtensionPoint(ToolWindowFactory.class).getExtensionList()) {
      if (id.equals(toolWindowFactory.getId())) {
        managerEx.initToolWindow(toolWindowFactory);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T getToolWindowElement(@Nonnull Class<T> clazz, @Nonnull Project project, @Nonnull Key<T> key, @Nonnull ProjectSystemId externalSystemId) {
    return ExternalSystemApiUtil.getToolWindowElement(clazz, project, key, externalSystemId);
  }

  @Nullable
  public static ToolWindow ensureToolWindowContentInitialized(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    return ExternalSystemApiUtil.ensureToolWindowContentInitialized(project, externalSystemId);
  }

  /**
   * Asks to refresh all external projects of the target external system linked to the given ide project.
   * <p>
   * 'Refresh' here means 'obtain the most up-to-date version and apply it to the ide'.
   *
   * @param project          target ide project
   * @param externalSystemId target external system which projects should be refreshed
   * @param force            flag which defines if external project refresh should be performed if it's config is up-to-date
   * @deprecated use {@link  ExternalSystemUtil#refreshProjects(consulo.ide.impl.idea.openapi.externalSystem.importing.ImportSpecBuilder)}
   */
  @Deprecated
  public static void refreshProjects(@Nonnull final Project project, @Nonnull final ProjectSystemId externalSystemId, boolean force) {
    refreshProjects(project, externalSystemId, force, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
  }

  /**
   * Asks to refresh all external projects of the target external system linked to the given ide project.
   * <p>
   * 'Refresh' here means 'obtain the most up-to-date version and apply it to the ide'.
   *
   * @param project          target ide project
   * @param externalSystemId target external system which projects should be refreshed
   * @param force            flag which defines if external project refresh should be performed if it's config is up-to-date
   * @deprecated use {@link  ExternalSystemUtil#refreshProjects(consulo.ide.impl.idea.openapi.externalSystem.importing.ImportSpecBuilder)}
   */
  @Deprecated
  public static void refreshProjects(@Nonnull final Project project, @Nonnull final ProjectSystemId externalSystemId, boolean force, @Nonnull final ProgressExecutionMode progressExecutionMode) {
    refreshProjects(new ImportSpecBuilder(project, externalSystemId).forceWhenUptodate(force).use(progressExecutionMode));
  }

  /**
   * Asks to refresh all external projects of the target external system linked to the given ide project based on provided spec
   *
   * @param specBuilder import specification builder
   */
  public static void refreshProjects(@Nonnull final ImportSpecBuilder specBuilder) {
    ImportSpec spec = specBuilder.build();

    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(spec.getExternalSystemId());
    if (manager == null) {
      return;
    }
    AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().apply(spec.getProject());
    final Collection<? extends ExternalProjectSettings> projectsSettings = settings.getLinkedProjectsSettings();
    if (projectsSettings.isEmpty()) {
      return;
    }

    final ProjectDataManager projectDataManager = ServiceManager.getService(ProjectDataManager.class);
    final int[] counter = new int[1];

    ExternalProjectRefreshCallback callback = new MyMultiExternalProjectRefreshCallback(spec.getProject(), projectDataManager, counter, spec.getExternalSystemId());

    Map<String, Long> modificationStamps = manager.getLocalSettingsProvider().apply(spec.getProject()).getExternalConfigModificationStamps();
    Set<String> toRefresh = ContainerUtilRt.newHashSet();
    for (ExternalProjectSettings setting : projectsSettings) {

      // don't refresh project when auto-import is disabled if such behavior needed (e.g. on project opening when auto-import is disabled)
      if (!setting.isUseAutoImport() && spec.isWhenAutoImportEnabled()) continue;

      if (spec.isForceWhenUptodate()) {
        toRefresh.add(setting.getExternalProjectPath());
      }
      else {
        Long oldModificationStamp = modificationStamps.get(setting.getExternalProjectPath());
        long currentModificationStamp = getTimeStamp(setting, spec.getExternalSystemId());
        if (oldModificationStamp == null || oldModificationStamp < currentModificationStamp) {
          toRefresh.add(setting.getExternalProjectPath());
        }
      }
    }

    if (!toRefresh.isEmpty()) {
      ExternalSystemNotificationManager.getInstance(spec.getProject()).clearNotifications(null, NotificationSource.PROJECT_SYNC, spec.getExternalSystemId());

      counter[0] = toRefresh.size();
      for (String path : toRefresh) {
        refreshProject(spec.getProject(), spec.getExternalSystemId(), path, callback, false, spec.getProgressExecutionMode());
      }
    }
  }

  private static long getTimeStamp(@Nonnull ExternalProjectSettings externalProjectSettings, @Nonnull ProjectSystemId externalSystemId) {
    long timeStamp = 0;
    for (ExternalSystemConfigLocator locator : ExternalSystemConfigLocator.EP_NAME.getExtensionList()) {
      if (!externalSystemId.equals(locator.getTargetExternalSystemId())) {
        continue;
      }
      for (VirtualFile virtualFile : locator.findAll(externalProjectSettings)) {
        timeStamp += virtualFile.getTimeStamp();
      }
    }
    return timeStamp;
  }

  /**
   * There is a possible case that an external module has been un-linked from ide project. There are two ways to process
   * ide modules which correspond to that external project:
   * <pre>
   * <ol>
   *   <li>Remove them from ide project as well;</li>
   *   <li>Keep them at ide project as well;</li>
   * </ol>
   * </pre>
   * This method handles that situation, i.e. it asks a user what should be done and acts accordingly.
   *
   * @param orphanModules    modules which correspond to the un-linked external project
   * @param project          current ide project
   * @param externalSystemId id of the external system which project has been un-linked from ide project
   */
  public static void ruleOrphanModules(@Nonnull final List<Module> orphanModules, @Nonnull final Project project, @Nonnull final ProjectSystemId externalSystemId) {
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {

        final JPanel content = new JPanel(new GridBagLayout());
        content.add(new JLabel(ExternalSystemBundle.message("orphan.modules.text", externalSystemId.getReadableName())), ExternalSystemUiUtil.getFillLineConstraints(0));

        final CheckBoxList<Module> orphanModulesList = new CheckBoxList<Module>();
        orphanModulesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        orphanModulesList.setItems(orphanModules, module -> module.getName());
        for (Module module : orphanModules) {
          orphanModulesList.setItemSelected(module, true);
        }
        orphanModulesList.setBorder(IdeBorderFactory.createEmptyBorder(8));
        content.add(orphanModulesList, ExternalSystemUiUtil.getFillLineConstraints(0));
        content.setBorder(IdeBorderFactory.createEmptyBorder(0, 0, 8, 0));

        DialogWrapper dialog = new DialogWrapper(project) {

          {
            setTitle(ExternalSystemBundle.message("import.title", externalSystemId.getReadableName()));
            init();
          }

          @Nullable
          @Override
          protected JComponent createCenterPanel() {
            return new JBScrollPane(content);
          }
        };
        boolean ok = dialog.showAndGet();
        if (!ok) {
          return;
        }

        List<Module> toRemove = ContainerUtilRt.newArrayList();
        for (int i = 0; i < orphanModules.size(); i++) {
          Module module = orphanModules.get(i);
          if (orphanModulesList.isItemSelected(i)) {
            toRemove.add(module);
          }
          else {
            ModuleDataService.unlinkModuleFromExternalSystem(module);
          }
        }

        if (!toRemove.isEmpty()) {
          ServiceManager.getService(ProjectDataManager.class).removeData(ProjectKeys.MODULE, toRemove, project, true);
        }
      }
    });
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Nullable
  private static String extractDetails(@Nonnull Throwable e) {
    final Throwable unwrapped = RemoteUtil.unwrap(e);
    if (unwrapped instanceof ExternalSystemException) {
      return ((ExternalSystemException)unwrapped).getOriginalReason();
    }
    return null;
  }

  /**
   * TODO[Vlad]: refactor the method to use {@link consulo.ide.impl.idea.openapi.externalSystem.importing.ImportSpecBuilder}
   * <p>
   * Queries slave gradle process to refresh target gradle project.
   *
   * @param project             target intellij project to use
   * @param externalProjectPath path of the target gradle project's file
   * @param callback            callback to be notified on refresh result
   * @param isPreviewMode       flag that identifies whether gradle libraries should be resolved during the refresh
   * @return the most up-to-date gradle project (if any)
   */
  public static void refreshProject(@Nonnull final Project project,
                                    @Nonnull final ProjectSystemId externalSystemId,
                                    @Nonnull final String externalProjectPath,
                                    @Nonnull final ExternalProjectRefreshCallback callback,
                                    final boolean isPreviewMode,
                                    @Nonnull final ProgressExecutionMode progressExecutionMode) {
    refreshProject(project, externalSystemId, externalProjectPath, callback, isPreviewMode, progressExecutionMode, true);
  }

  /**
   * TODO[Vlad]: refactor the method to use {@link consulo.ide.impl.idea.openapi.externalSystem.importing.ImportSpecBuilder}
   * <p>
   * Queries slave gradle process to refresh target gradle project.
   *
   * @param project             target intellij project to use
   * @param externalProjectPath path of the target gradle project's file
   * @param callback            callback to be notified on refresh result
   * @param isPreviewMode       flag that identifies whether gradle libraries should be resolved during the refresh
   * @param reportRefreshError  prevent to show annoying error notification, e.g. if auto-import mode used
   * @return the most up-to-date gradle project (if any)
   */
  public static void refreshProject(@Nonnull final Project project,
                                    @Nonnull final ProjectSystemId externalSystemId,
                                    @Nonnull final String externalProjectPath,
                                    @Nonnull final ExternalProjectRefreshCallback callback,
                                    final boolean isPreviewMode,
                                    @Nonnull final ProgressExecutionMode progressExecutionMode,
                                    final boolean reportRefreshError) {
    File projectFile = new File(externalProjectPath);
    final String projectName;
    if (projectFile.isFile()) {
      projectName = projectFile.getParentFile().getName();
    }
    else {
      projectName = projectFile.getName();
    }
    final ExternalSystemApiUtil.TaskUnderProgress refreshProjectStructureTask = new ExternalSystemApiUtil.TaskUnderProgress() {
      @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "IOResourceOpenedButNotSafelyClosed"})
      @Override
      public void execute(@Nonnull ProgressIndicator indicator) {
        if (project.isDisposed()) return;

        ExternalSystemProcessingManager processingManager = ServiceManager.getService(ExternalSystemProcessingManager.class);
        if (processingManager.findTask(ExternalSystemTaskType.RESOLVE_PROJECT, externalSystemId, externalProjectPath) != null) {
          callback.onFailure(ExternalSystemBundle.message("error.resolve.already.running", externalProjectPath), null);
          return;
        }

        if (!(callback instanceof MyMultiExternalProjectRefreshCallback)) {
          ExternalSystemNotificationManager.getInstance(project).clearNotifications(null, NotificationSource.PROJECT_SYNC, externalSystemId);
        }

        ExternalSystemResolveProjectTask task = new ExternalSystemResolveProjectTask(externalSystemId, project, externalProjectPath, isPreviewMode);

        task.execute(indicator, ExternalSystemTaskNotificationListener.EP_NAME.getExtensions());
        if (project.isDisposed()) return;

        final Throwable error = task.getError();
        if (error == null) {
          ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
          assert manager != null;
          DataNode<ProjectData> externalProject = task.getExternalProject();

          if (externalProject != null) {
            Set<String> externalModulePaths = ContainerUtil.newHashSet();
            Collection<DataNode<ModuleData>> moduleNodes = ExternalSystemApiUtil.findAll(externalProject, ProjectKeys.MODULE);
            for (DataNode<ModuleData> node : moduleNodes) {
              externalModulePaths.add(node.getData().getLinkedExternalProjectPath());
            }

            String projectPath = externalProject.getData().getLinkedExternalProjectPath();
            ExternalProjectSettings linkedProjectSettings = manager.getSettingsProvider().apply(project).getLinkedProjectSettings(projectPath);
            if (linkedProjectSettings != null) {
              linkedProjectSettings.setModules(externalModulePaths);

              long stamp = getTimeStamp(linkedProjectSettings, externalSystemId);
              if (stamp > 0) {
                manager.getLocalSettingsProvider().apply(project).getExternalConfigModificationStamps().put(externalProjectPath, stamp);
              }
            }
          }

          callback.onSuccess(externalProject);
          return;
        }
        if (error instanceof ImportCanceledException) {
          // stop refresh task
          return;
        }
        String message = ExternalSystemApiUtil.buildErrorMessage(error);
        if (StringUtil.isEmpty(message)) {
          message = String.format("Can't resolve %s project at '%s'. Reason: %s", externalSystemId.getReadableName(), externalProjectPath, message);
        }

        callback.onFailure(message, extractDetails(error));

        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        if (manager == null) {
          return;
        }
        AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().apply(project);
        ExternalProjectSettings projectSettings = settings.getLinkedProjectSettings(externalProjectPath);
        if (projectSettings == null || !reportRefreshError) {
          return;
        }

        ExternalSystemNotificationManager.getInstance(project).processExternalProjectRefreshError(error, projectName, externalSystemId);
      }
    };

    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        final String title;
        switch (progressExecutionMode) {
          case MODAL_SYNC:
            title = ExternalSystemBundle.message("progress.import.text", projectName, externalSystemId.getReadableName());
            new Task.Modal(project, title, true) {
              @Override
              public void run(@Nonnull ProgressIndicator indicator) {
                refreshProjectStructureTask.execute(indicator);
              }
            }.queue();
            break;
          case IN_BACKGROUND_ASYNC:
            title = ExternalSystemBundle.message("progress.refresh.text", projectName, externalSystemId.getReadableName());
            new Task.Backgroundable(project, title) {
              @Override
              public void run(@Nonnull ProgressIndicator indicator) {
                refreshProjectStructureTask.execute(indicator);
              }
            }.queue();
            break;
          case START_IN_FOREGROUND_ASYNC:
            title = ExternalSystemBundle.message("progress.refresh.text", projectName, externalSystemId.getReadableName());
            new Task.Backgroundable(project, title, true, PerformInBackgroundOption.DEAF) {
              @Override
              public void run(@Nonnull ProgressIndicator indicator) {
                refreshProjectStructureTask.execute(indicator);
              }
            }.queue();
        }
      }
    });
  }

  public static void runTask(@Nonnull ExternalSystemTaskExecutionSettings taskSettings, @Nonnull String executorId, @Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    runTask(taskSettings, executorId, project, externalSystemId, null, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
  }

  public static void runTask(@Nonnull final ExternalSystemTaskExecutionSettings taskSettings,
                             @Nonnull final String executorId,
                             @Nonnull final Project project,
                             @Nonnull final ProjectSystemId externalSystemId,
                             @Nullable final TaskCallback callback,
                             @Nonnull final ProgressExecutionMode progressExecutionMode) {
    ExternalSystemApiUtil.runTask(taskSettings, executorId, project, externalSystemId, callback, progressExecutionMode);
  }

  @Nullable
  public static Pair<ProgramRunner, ExecutionEnvironment> createRunner(@Nonnull ExternalSystemTaskExecutionSettings taskSettings,
                                                                       @Nonnull String executorId,
                                                                       @Nonnull Project project,
                                                                       @Nonnull ProjectSystemId externalSystemId) {

    return ExternalSystemApiUtil.createRunner(taskSettings, executorId, project, externalSystemId);
  }

  @Nullable
  public static AbstractExternalSystemTaskConfigurationType findConfigurationType(@Nonnull ProjectSystemId externalSystemId) {
    return ExternalSystemApiUtil.findConfigurationType(externalSystemId);
  }

  /**
   * Is expected to be called when given task info is about to be executed.
   * <p>
   * Basically, this method updates recent tasks list at the corresponding external system tool window and
   * persists new recent tasks state.
   *
   * @param taskInfo task which is about to be executed
   * @param project  target project
   */
  public static void updateRecentTasks(@Nonnull ExternalTaskExecutionInfo taskInfo, @Nonnull Project project) {
    ExternalSystemApiUtil.updateRecentTasks(taskInfo, project);
  }

  @Nullable
  public static String getRunnerId(@Nonnull String executorId) {
    return ExternalSystemApiUtil.getRunnerId(executorId);
  }

  /**
   * Allows to answer if given ide project has 1-1 mapping with the given external project, i.e. the ide project has been
   * imported from external system and no other external projects have been added.
   * <p>
   * This might be necessary in a situation when project-level setting is changed (e.g. project name). We don't want to rename
   * ide project if it doesn't completely corresponds to the given ide project then.
   *
   * @param ideProject      target ide project
   * @param externalProject target external project
   * @return <code>true</code> if given ide project has 1-1 mapping to the given external project;
   * <code>false</code> otherwise
   */
  public static boolean isOneToOneMapping(@Nonnull Project ideProject, @Nonnull DataNode<ProjectData> externalProject) {
    String linkedExternalProjectPath = null;
    for (ExternalSystemManager<?, ?, ?, ?, ?> manager : ExternalSystemApiUtil.getAllManagers()) {
      ProjectSystemId externalSystemId = manager.getSystemId();
      AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(ideProject, externalSystemId);
      Collection projectsSettings = systemSettings.getLinkedProjectsSettings();
      int linkedProjectsNumber = projectsSettings.size();
      if (linkedProjectsNumber > 1) {
        // More than one external project of the same external system type is linked to the given ide project.
        return false;
      }
      else if (linkedProjectsNumber == 1) {
        if (linkedExternalProjectPath == null) {
          // More than one external project of different external system types is linked to the current ide project.
          linkedExternalProjectPath = ((ExternalProjectSettings)projectsSettings.iterator().next()).getExternalProjectPath();
        }
        else {
          return false;
        }
      }
    }

    ProjectData projectData = externalProject.getData();
    if (linkedExternalProjectPath != null && !linkedExternalProjectPath.equals(projectData.getLinkedExternalProjectPath())) {
      // New external project is being linked.
      return false;
    }

    Set<String> externalModulePaths = ContainerUtilRt.newHashSet();
    for (DataNode<ModuleData> moduleNode : ExternalSystemApiUtil.findAll(externalProject, ProjectKeys.MODULE)) {
      externalModulePaths.add(moduleNode.getData().getLinkedExternalProjectPath());
    }
    externalModulePaths.remove(linkedExternalProjectPath);

    for (Module module : ModuleManager.getInstance(ideProject).getModules()) {
      String path = ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
      if (!StringUtil.isEmpty(path) && !externalModulePaths.remove(path)) {
        return false;
      }
    }
    return externalModulePaths.isEmpty();
  }

  /**
   * Tries to obtain external project info implied by the given settings and link that external project to the given ide project.
   *
   * @param externalSystemId        target external system
   * @param projectSettings         settings of the external project to link
   * @param project                 target ide project to link external project to
   * @param executionResultCallback it might take a while to resolve external project info, that's why it's possible to provide
   *                                a callback to be notified on processing result. It receives <code>true</code> if an external
   *                                project has been successfully linked to the given ide project;
   *                                <code>false</code> otherwise (note that corresponding notification with error details is expected
   *                                to be shown to the end-user then)
   * @param isPreviewMode           flag which identifies if missing external project binaries should be downloaded
   * @param progressExecutionMode   identifies how progress bar will be represented for the current processing
   */
  @SuppressWarnings("UnusedDeclaration")
  public static void linkExternalProject(@Nonnull final ProjectSystemId externalSystemId,
                                         @Nonnull final ExternalProjectSettings projectSettings,
                                         @Nonnull final Project project,
                                         @Nullable final Consumer<Boolean> executionResultCallback,
                                         boolean isPreviewMode,
                                         @Nonnull final ProgressExecutionMode progressExecutionMode) {
    ExternalProjectRefreshCallback callback = new ExternalProjectRefreshCallback() {
      @SuppressWarnings("unchecked")
      @Override
      public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
        if (externalProject == null) {
          if (executionResultCallback != null) {
            executionResultCallback.accept(false);
          }
          return;
        }
        AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(project, externalSystemId);
        Set<ExternalProjectSettings> projects = ContainerUtilRt.<ExternalProjectSettings>newHashSet(systemSettings.getLinkedProjectsSettings());
        projects.add(projectSettings);
        systemSettings.setLinkedProjectsSettings(projects);
        ensureToolWindowInitialized(project, externalSystemId);
        ExternalSystemApiUtil.executeProjectChangeAction(new DisposeAwareProjectChange(project) {
          @RequiredUIAccess
          @Override
          public void execute() {
            ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring(new Runnable() {
              @Override
              public void run() {
                ProjectDataManager dataManager = ServiceManager.getService(ProjectDataManager.class);
                dataManager.importData(externalProject.getKey(), Collections.singleton(externalProject), project, true);
              }
            });
          }
        });
        if (executionResultCallback != null) {
          executionResultCallback.accept(true);
        }
      }

      @Override
      public void onFailure(@Nonnull String errorMessage, @Nullable String errorDetails) {
        if (executionResultCallback != null) {
          executionResultCallback.accept(false);
        }
      }
    };
    refreshProject(project, externalSystemId, projectSettings.getExternalProjectPath(), callback, isPreviewMode, progressExecutionMode);
  }

  @Nullable
  public static VirtualFile waitForTheFile(@Nullable final String path) {
    if (path == null) return null;

    final VirtualFile[] file = new VirtualFile[1];
    final Application app = ApplicationManager.getApplication();
    Runnable action = new Runnable() {
      public void run() {
        app.runWriteAction(new Runnable() {
          public void run() {
            file[0] = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
          }
        });
      }
    };
    if (app.isDispatchThread()) {
      action.run();
    }
    else {
      app.invokeAndWait(action, IdeaModalityState.defaultModalityState());
    }
    return file[0];
  }

  private static class MyMultiExternalProjectRefreshCallback implements ExternalProjectRefreshCallback {

    @Nonnull
    private final Set<String> myExternalModulePaths;
    private final Project myProject;
    private final ProjectDataManager myProjectDataManager;
    private final int[] myCounter;
    private final ProjectSystemId myExternalSystemId;

    public MyMultiExternalProjectRefreshCallback(Project project, ProjectDataManager projectDataManager, int[] counter, ProjectSystemId externalSystemId) {
      myProject = project;
      myProjectDataManager = projectDataManager;
      myCounter = counter;
      myExternalSystemId = externalSystemId;
      myExternalModulePaths = ContainerUtilRt.newHashSet();
    }

    @Override
    public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
      if (externalProject == null) {
        return;
      }
      Collection<DataNode<ModuleData>> moduleNodes = ExternalSystemApiUtil.findAll(externalProject, ProjectKeys.MODULE);
      for (DataNode<ModuleData> node : moduleNodes) {
        myExternalModulePaths.add(node.getData().getLinkedExternalProjectPath());
      }
      ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(myProject) {
        @RequiredUIAccess
        @Override
        public void execute() {
          ProjectRootManagerEx.getInstanceEx(myProject).mergeRootsChangesDuring(new Runnable() {
            @Override
            public void run() {
              myProjectDataManager.importData(externalProject.getKey(), Collections.singleton(externalProject), myProject, true);
            }
          });

          processOrphanProjectLibraries();
        }
      });
      if (--myCounter[0] <= 0) {
        processOrphanModules();
      }
    }

    @Override
    public void onFailure(@Nonnull String errorMessage, @Nullable String errorDetails) {
      myCounter[0] = Integer.MAX_VALUE; // Don't process orphan modules if there was an error on refresh.
    }

    private void processOrphanModules() {
      if (myProject.isDisposed()) return;
      if (ExternalSystemDebugEnvironment.DEBUG_ORPHAN_MODULES_PROCESSING) {
        LOG.info(String.format("Checking for orphan modules. External paths returned by external system: '%s'", myExternalModulePaths));
      }
      List<Module> orphanIdeModules = ContainerUtilRt.newArrayList();
      String externalSystemIdAsString = myExternalSystemId.toString();

      for (Module module : ModuleManager.getInstance(myProject).getModules()) {
        String s = ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY);
        String p = ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
        if (ExternalSystemDebugEnvironment.DEBUG_ORPHAN_MODULES_PROCESSING) {
          LOG.info(String.format("IDE module: EXTERNAL_SYSTEM_ID_KEY - '%s', LINKED_PROJECT_PATH_KEY - '%s'.", s, p));
        }
        if (externalSystemIdAsString.equals(s) && !myExternalModulePaths.contains(p)) {
          orphanIdeModules.add(module);
          if (ExternalSystemDebugEnvironment.DEBUG_ORPHAN_MODULES_PROCESSING) {
            LOG.info(String.format("External paths doesn't contain IDE module LINKED_PROJECT_PATH_KEY anymore => add to orphan IDE modules."));
          }
        }
      }

      if (!orphanIdeModules.isEmpty()) {
        ruleOrphanModules(orphanIdeModules, myProject, myExternalSystemId);
      }
    }

    private void processOrphanProjectLibraries() {
      List<Library> orphanIdeLibraries = ContainerUtilRt.newArrayList();

      LibraryTable projectLibraryTable = ProjectLibraryTableImpl.getInstance(myProject);
      for (Library library : projectLibraryTable.getLibraries()) {
        if (!ExternalSystemApiUtil.isExternalSystemLibrary(library, myExternalSystemId)) continue;
        if (ProjectStructureHelper.isOrphanProjectLibrary(library, ModuleManager.getInstance(myProject).getModules())) {
          orphanIdeLibraries.add(library);
        }
      }
      for (Library orphanIdeLibrary : orphanIdeLibraries) {
        projectLibraryTable.removeLibrary(orphanIdeLibrary);
      }
    }
  }
}
