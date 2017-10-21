/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.platform;

import com.intellij.ide.GeneralSettings;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.intellij.util.Consumer;
import consulo.annotations.RequiredDispatchThread;
import consulo.platform.Platform;
import consulo.project.ProjectOpenProcessors;
import consulo.ui.UIAccess;
import consulo.util.SandboxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author max
 */
public class PlatformProjectOpenProcessor extends ProjectOpenProcessor {
  private static final Logger LOGGER = Logger.getInstance(PlatformProjectOpenProcessor.class);
  private static final PlatformProjectOpenProcessor INSTANCE = new PlatformProjectOpenProcessor();

  public static PlatformProjectOpenProcessor getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean canOpenProject(@NotNull File file) {
    return file.isDirectory() && new File(file, Project.DIRECTORY_STORE_FOLDER).exists();
  }

  @RequiredDispatchThread
  @Override
  @Nullable
  public Project doOpenProject(@NotNull final VirtualFile virtualFile, @Nullable final Project projectToClose, final boolean forceOpenInNewFrame) {
    return doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame, -1, null);
  }

  @Nullable
  public static Project doOpenProject(@NotNull final VirtualFile virtualFile, Project projectToClose, final boolean forceOpenInNewFrame, final int line, @Nullable Consumer<Project> callback) {
    VirtualFile baseDir = virtualFile;
    if (!baseDir.isDirectory()) {
      baseDir = virtualFile.getParent();
      while (baseDir != null) {
        if (new File(FileUtil.toSystemDependentName(baseDir.getPath()), Project.DIRECTORY_STORE_FOLDER).exists()) {
          break;
        }
        baseDir = baseDir.getParent();
      }
      if (baseDir == null) {
        baseDir = virtualFile.getParent();
      }
    }

    final File projectDir = new File(FileUtil.toSystemDependentName(baseDir.getPath()), Project.DIRECTORY_STORE_FOLDER);

    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    if (!forceOpenInNewFrame && openProjects.length > 0) {
      if (projectToClose == null) {
        projectToClose = openProjects[openProjects.length - 1];
      }

      int exitCode = ProjectUtil.confirmOpenNewProject(false);
      if (exitCode == GeneralSettings.OPEN_PROJECT_SAME_WINDOW) {
        if (!ProjectUtil.closeAndDispose(projectToClose)) return null;
      }
      else if (exitCode != GeneralSettings.OPEN_PROJECT_NEW_WINDOW) { // not in a new window
        return null;
      }
    }

    boolean runConfigurators = true;
    final ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
    Project project = null;
    if (projectDir.exists()) {
      try {
        for (ProjectOpenProcessor processor : ProjectOpenProcessors.getInstance().getProcessors()) {
          processor.refreshProjectFiles(projectDir);
        }

        project = projectManager.convertAndLoadProject(baseDir.getPath());
        if (project == null) {
          WelcomeFrame.showIfNoProjectOpened();
          return null;
        }
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length > 0) {
          runConfigurators = false;
        }
      }
      catch (Exception e) {
        LOGGER.error(e);
      }
    }
    else {
      projectDir.mkdirs();
    }

    if (project == null) {
      project = projectManager.newProject(projectDir.getParentFile().getName(), projectDir.getParent(), true, false);
    }

    if (project == null) return null;
    openProjectToolWindow(project);
    openFileFromCommandLine(project, virtualFile, line);
    if (!projectManager.openProject(project)) {
      WelcomeFrame.showIfNoProjectOpened();
      final Project finalProject = project;
      ApplicationManager.getApplication().runWriteAction(() -> Disposer.dispose(finalProject));
      return project;
    }

    if (callback != null && runConfigurators) {
      callback.consume(project);
    }

    return project;
  }

  public static void openProjectToolWindow(final Project project) {
    Platform.hacky(() -> desktopOpenProjectToolWindow(project), () -> {
      // TODO [VISTALL] implement it!!!
    });
  }

  private static void desktopOpenProjectToolWindow(final Project project) {
    //noinspection RedundantCast
    StartupManager.getInstance(project).registerPostStartupActivity((DumbAwareRunnable)() -> {
      // ensure the dialog is shown after all startup activities are done
      SwingUtilities.invokeLater(() -> ApplicationManager.getApplication().invokeLater(() -> {
        if (project.isDisposed()) return;
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null && toolWindow.getType() != ToolWindowType.SLIDING) {
          toolWindow.activate(null);
        }
      }, ModalityState.NON_MODAL));
    });
  }

  private static void openFileFromCommandLine(final Project project, final VirtualFile virtualFile, final int line) {
    //noinspection RedundantCast
    StartupManager.getInstance(project).registerPostStartupActivity((DumbAwareRunnable)() -> {
      if (project.isDisposed()) {
        return;
      }
      ToolWindowManager.getInstance(project).invokeLater(() -> ToolWindowManager.getInstance(project).invokeLater(() -> {
        if (!virtualFile.isDirectory()) {
          if (line > 0) {
            new OpenFileDescriptor(project, virtualFile, line - 1, 0).navigate(true);
          }
          else {
            new OpenFileDescriptor(project, virtualFile).navigate(true);
          }
        }
      }));
    });
  }

  @Override
  @NotNull
  public Icon getIcon() {
    return SandboxUtil.getAppIcon();
  }

  @NotNull
  @Override
  public String getFileSample() {
    return "<b>Consulo</b> project";
  }

  @Override
  public String getName() {
    return "text editor";
  }

  //region Async staff
  @Override
  public void doOpenProjectAsync(@NotNull AsyncResult<Project> asyncResult,
                                 @NotNull VirtualFile virtualFile,
                                 @Nullable Project projectToClose,
                                 boolean forceOpenInNewFrame,
                                 @NotNull UIAccess uiAccess) {
    doOpenProjectAsync(asyncResult, virtualFile, projectToClose, forceOpenInNewFrame, -1, uiAccess, null);
  }

  public static void doOpenProjectAsync(@NotNull AsyncResult<Project> result,
                                        @NotNull VirtualFile virtualFile,
                                        Project projectToClose,
                                        boolean forceOpenInNewFrame,
                                        int line,
                                        @NotNull UIAccess uiAccess,
                                        @Nullable Consumer<Project> callback) {
    VirtualFile baseDir = virtualFile;
    if (!baseDir.isDirectory()) {
      baseDir = virtualFile.getParent();
      while (baseDir != null) {
        if (new File(FileUtil.toSystemDependentName(baseDir.getPath()), Project.DIRECTORY_STORE_FOLDER).exists()) {
          break;
        }
        baseDir = baseDir.getParent();
      }
      if (baseDir == null) {
        baseDir = virtualFile.getParent();
      }
    }

    final File projectDir = new File(FileUtil.toSystemDependentName(baseDir.getPath()), Project.DIRECTORY_STORE_FOLDER);

    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    if (!forceOpenInNewFrame && openProjects.length > 0) {
      if (projectToClose == null) {
        projectToClose = openProjects[openProjects.length - 1];
      }

      int exitCode = ProjectUtil.confirmOpenNewProject(false);
      if (exitCode == GeneralSettings.OPEN_PROJECT_SAME_WINDOW) {
        if (!ProjectUtil.closeAndDispose(projectToClose)) {
          result.reject("not closed project");
          return;
        }
      }
      else if (exitCode != GeneralSettings.OPEN_PROJECT_NEW_WINDOW) { // not in a new window
        result.reject("not open in new window");
        return;
      }
    }

    ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
    AtomicBoolean runConfigurators = new AtomicBoolean(true);

    final VirtualFile finalBaseDir = baseDir;

    Consumer<Project> afterProjectAction = project -> {
      openProjectToolWindow(project);
      openFileFromCommandLine(project, virtualFile, line);
      if (!projectManager.openProject(project, uiAccess)) {
        WelcomeFrame.showIfNoProjectOpened();
        final Project finalProject = project;
        ApplicationManager.getApplication().runWriteAction(() -> Disposer.dispose(finalProject));
      }

      if (callback != null && runConfigurators.get()) {
        callback.consume(project);
      }
    };

    result.doWhenDone(afterProjectAction);

    if (projectDir.exists()) {
      for (ProjectOpenProcessor processor : ProjectOpenProcessors.getInstance().getProcessors()) {
        processor.refreshProjectFiles(projectDir);
      }

      AsyncResult<Project> anotherResult = new AsyncResult<>();
      anotherResult.doWhenDone((project) -> {
        final Module[] modules = ReadAction.compute(() -> ModuleManager.getInstance(project).getModules());
        if (modules.length > 0) {
          runConfigurators.set(false);
        }

        result.setDone(project);
      });

      anotherResult.doWhenRejected((Runnable)result::setRejected);

      anotherResult.doWhenRejectedButNotThrowable(WelcomeFrame::showIfNoProjectOpened);

      anotherResult.doWhenRejectedWithThrowable(LOGGER::error);

      projectManager.convertAndLoadProjectAsync(anotherResult, baseDir.getPath());
    }
    else {
      projectDir.mkdirs();
      Project project = projectManager.newProject(projectDir.getParentFile().getName(), projectDir.getParent(), true, false);
      if (project == null) {
        result.reject("can't create project");
        return;
      }

      result.setDone(project);
    }
  }
  //endregion
}
