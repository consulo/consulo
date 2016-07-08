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
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
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
import consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * @author max
 */
@Logger
public class PlatformProjectOpenProcessor extends ProjectOpenProcessor {

  public static PlatformProjectOpenProcessor getInstance() {
    PlatformProjectOpenProcessor projectOpenProcessor = getInstanceIfItExists();
    assert projectOpenProcessor != null;
    return projectOpenProcessor;
  }

  @Nullable
  public static PlatformProjectOpenProcessor getInstanceIfItExists() {
    return EXTENSION_POINT_NAME.findExtension(PlatformProjectOpenProcessor.class);
  }

  @Override
  public boolean canOpenProject(final VirtualFile file) {
    return file.isDirectory() && file.findChild(Project.DIRECTORY_STORE_FOLDER) != null;
  }

  @Override
  public boolean isProjectFile(VirtualFile file) {
    return false;
  }

  @Override
  public boolean lookForProjectsInDirectory() {
    return false;
  }

  @Override
  @Nullable
  public Project doOpenProject(@NotNull final VirtualFile virtualFile, @Nullable final Project projectToClose, final boolean forceOpenInNewFrame) {
    return doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame, -1, null);
  }

  @Nullable
  public static Project doOpenProject(@NotNull final VirtualFile virtualFile,
                                      Project projectToClose,
                                      final boolean forceOpenInNewFrame,
                                      final int line,
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
        for (ProjectOpenProcessor processor : ProjectOpenProcessor.EXTENSION_POINT_NAME.getExtensions()) {
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
    ProjectBaseDirectory.getInstance(project).setBaseDir(baseDir);
    openProjectToolWindow(project);
    openFileFromCommandLine(project, virtualFile, line);
    if (!projectManager.openProject(project)) {
      WelcomeFrame.showIfNoProjectOpened();
      final Project finalProject = project;
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          Disposer.dispose(finalProject);
        }
      });
      return project;
    }

    if (callback != null && runConfigurators) {
      callback.consume(project);
    }

    return project;
  }

  public static void openProjectToolWindow(final Project project) {
    StartupManager.getInstance(project).registerPostStartupActivity(new DumbAwareRunnable() {
      @Override
      public void run() {
        // ensure the dialog is shown after all startup activities are done
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                if (project.isDisposed()) return;
                final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
                if (toolWindow != null && toolWindow.getType() != ToolWindowType.SLIDING) {
                  toolWindow.activate(null);
                }
              }
            }, ModalityState.NON_MODAL);
          }
        });
      }
    });
  }

  private static void openFileFromCommandLine(final Project project, final VirtualFile virtualFile, final int line) {
    StartupManager.getInstance(project).registerPostStartupActivity(new DumbAwareRunnable() {
      @Override
      public void run() {
        if(project.isDisposed()) {
          return;
        }
        ToolWindowManager.getInstance(project).invokeLater(new Runnable() {
          @Override
          public void run() {
            ToolWindowManager.getInstance(project).invokeLater(new Runnable() {
              @Override
              public void run() {
                if (!virtualFile.isDirectory()) {
                  if (line > 0) {
                    new OpenFileDescriptor(project, virtualFile, line-1, 0).navigate(true);
                  }
                  else {
                    new OpenFileDescriptor(project, virtualFile).navigate(true);
                  }
                }
              }
            });
          }
        });
      }
    });
  }

  @Override
  @Nullable
  public Icon getIcon() {
    return null;
  }

  @Override
  public String getName() {
    return "text editor";
  }
}
