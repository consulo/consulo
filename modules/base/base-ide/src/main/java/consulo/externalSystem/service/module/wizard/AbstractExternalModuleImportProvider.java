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
package consulo.externalSystem.service.module.wizard;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemResolveProjectTask;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.logging.Logger;
import consulo.moduleImport.ModuleImportProvider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.wizard.WizardStep;
import consulo.ui.wizard.WizardStepValidationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 30-Jan-17
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

  public AbstractExternalModuleImportProvider(@Nonnull ProjectDataManager projectDataManager, @Nonnull C control, @Nonnull ProjectSystemId externalSystemId) {
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

  @RequiredReadAction
  @Override
  public void process(@Nonnull ExternalModuleImportContext<C> context, @Nonnull final Project project, @Nonnull ModifiableModuleModel model, @Nonnull Consumer<Module> newModuleConsumer) {
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);
    final DataNode<ProjectData> externalProjectNode = getExternalProjectNode();
    if (externalProjectNode != null) {
      beforeCommit(externalProjectNode, project);
    }

    StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
      @SuppressWarnings("unchecked")
      @Override
      public void run() {
        AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(project, myExternalSystemId);
        final ExternalProjectSettings projectSettings = getCurrentExternalProjectSettings();
        Set<ExternalProjectSettings> projects = new HashSet<>(systemSettings.getLinkedProjectsSettings());
        // add current importing project settings to linked projects settings or replace if similar already exist
        projects.remove(projectSettings);
        projects.add(projectSettings);

        systemSettings.copyFrom(myControl.getSystemSettings());
        systemSettings.setLinkedProjectsSettings(projects);

        if (externalProjectNode != null) {
          ExternalSystemUtil.ensureToolWindowInitialized(project, myExternalSystemId);
          ExternalSystemApiUtil.executeProjectChangeAction(new DisposeAwareProjectChange(project) {
            @RequiredUIAccess
            @Override
            public void execute() {
              ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring(new Runnable() {
                @Override
                public void run() {
                  myProjectDataManager.importData(externalProjectNode.getKey(), Collections.singleton(externalProjectNode), project, true);
                  myExternalProjectNode = null;
                }
              });
            }
          });

          final Runnable resolveDependenciesTask = new Runnable() {
            @Override
            public void run() {
              String progressText = ExternalSystemBundle.message("progress.resolve.libraries", myExternalSystemId.getReadableName());
              ProgressManager.getInstance().run(new Task.Backgroundable(project, progressText, false) {
                @Override
                public void run(@Nonnull final ProgressIndicator indicator) {
                  if (project.isDisposed()) return;
                  ExternalSystemResolveProjectTask task = new ExternalSystemResolveProjectTask(myExternalSystemId, project, projectSettings.getExternalProjectPath(), false);
                  task.execute(indicator, ExternalSystemTaskNotificationListener.EP_NAME.getExtensions());
                  DataNode<ProjectData> projectWithResolvedLibraries = task.getExternalProject();
                  if (projectWithResolvedLibraries == null) {
                    return;
                  }

                  setupLibraries(projectWithResolvedLibraries, project);
                }
              });
            }
          };
          UIUtil.invokeLaterIfNeeded(resolveDependenciesTask);
        }
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
    final String externalSystemName = myExternalSystemId.getReadableName();
    File projectFile = getProjectFile();
    if (projectFile == null) {
      throw new WizardStepValidationException(ExternalSystemBundle.message("error.project.undefined"));
    }
    projectFile = getExternalProjectConfigToUse(projectFile);
    final Ref<WizardStepValidationException> error = new Ref<>();
    final ExternalProjectRefreshCallback callback = new ExternalProjectRefreshCallback() {
      @Override
      public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
        myExternalProjectNode = externalProject;
      }

      @Override
      public void onFailure(@Nonnull String errorMessage, @Nullable String errorDetails) {
        if (!StringUtil.isEmpty(errorDetails)) {
          LOG.warn(errorDetails);
        }
        error.set(new WizardStepValidationException(ExternalSystemBundle.message("error.resolve.with.reason", errorMessage)));
      }
    };

    final Project project = getContextOrDefaultProject(context);
    final File finalProjectFile = projectFile;
    final String externalProjectPath = FileUtil.toCanonicalPath(finalProjectFile.getAbsolutePath());
    final Ref<WizardStepValidationException> exRef = new Ref<>();
    executeAndRestoreDefaultProjectSettings(project, new Runnable() {
      @Override
      public void run() {
        try {
          ExternalSystemUtil.refreshProject(project, myExternalSystemId, externalProjectPath, callback, true, ProgressExecutionMode.MODAL_SYNC);
        }
        catch (IllegalArgumentException e) {
          exRef.set(new WizardStepValidationException(ExternalSystemBundle.message("error.cannot.parse.project", externalSystemName)));
        }
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
    if (systemSettings instanceof PersistentStateComponent) {
      systemStateToRestore = ((PersistentStateComponent)systemSettings).getState();
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
    final String linkedProjectPath = FileUtil.toCanonicalPath(externalProjectConfigFile.getPath());
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
  private void setupLibraries(@Nonnull final DataNode<ProjectData> projectWithResolvedLibraries, final Project project) {
    ExternalSystemApiUtil.executeProjectChangeAction(new DisposeAwareProjectChange(project) {
      @RequiredUIAccess
      @Override
      public void execute() {
        ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring(new Runnable() {
          @Override
          public void run() {
            if (ExternalSystemApiUtil.isNewProjectConstruction()) {
              // Clean existing libraries (if any).
              LibraryTable projectLibraryTable = ProjectLibraryTable.getInstance(project);
              if (projectLibraryTable == null) {
                LOG.warn("Can't resolve external dependencies of the target gradle project (" + project + "). Reason: project " + "library table is undefined");
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
          }
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
  public void buildSteps(@Nonnull Consumer<WizardStep<ExternalModuleImportContext<C>>> consumer, @Nonnull ExternalModuleImportContext<C> context) {
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
