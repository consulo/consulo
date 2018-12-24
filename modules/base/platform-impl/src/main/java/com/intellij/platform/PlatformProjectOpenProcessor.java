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
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
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
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.intellij.util.Consumer;
import consulo.application.AccessRule;
import consulo.project.ProjectOpenProcessors;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  public boolean canOpenProject(@Nonnull File file) {
    return file.isDirectory() && new File(file, Project.DIRECTORY_STORE_FOLDER).exists();
  }

  public static void openProjectToolWindow(Project project, UIAccess uiAccess) {
    //noinspection RedundantCast
    StartupManager.getInstance(project).registerPostStartupActivity((DumbAwareRunnable)() -> {
      // ensure the dialog is shown after all startup activities are done
      uiAccess.give(() -> {
        if (project.isDisposed()) return;

        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);

        if (toolWindow != null && toolWindow.getType() != ToolWindowType.SLIDING) {
          toolWindow.activate(null);
        }
      });
    });
  }

  private static void openFileFromCommandLineAsync(@Nonnull Project project, final VirtualFile virtualFile, final int line, UIAccess uiAccess) {
    //noinspection RedundantCast
    StartupManager.getInstance(project).registerPostStartupActivity((DumbAwareRunnable)() -> {
      if (project.isDisposed()) {
        return;
      }
      uiAccess.give(() -> {
        if (!virtualFile.isDirectory()) {
          if (line > 0) {
            new OpenFileDescriptor(project, virtualFile, line - 1, 0).navigate(true);
          }
          else {
            new OpenFileDescriptor(project, virtualFile).navigate(true);
          }
        }
      });
    });
  }

  @Override
  @Nonnull
  public Image getIcon() {
    return Application.get().getIcon();
  }

  @Nonnull
  @Override
  public String getFileSample() {
    return "<b>Consulo</b> project";
  }

  //region Async staff
  @Override
  public void doOpenProjectAsync(@Nonnull AsyncResult<Project> asyncResult,
                                 @Nonnull VirtualFile virtualFile,
                                 @Nullable Project projectToClose,
                                 boolean forceOpenInNewFrame,
                                 @Nonnull UIAccess uiAccess) {
    doOpenProjectAsync(asyncResult, virtualFile, projectToClose, forceOpenInNewFrame, -1, uiAccess, null);
  }

  private static void doOpenProjectAsync(@Nonnull AsyncResult<Project> result,
                                         @Nonnull VirtualFile virtualFile,
                                         Project projectToClose,
                                         boolean forceOpenInNewFrame,
                                         int line,
                                         @Nonnull UIAccess uiAccess,
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

    Consumer<Project> afterProjectAction = project -> {
      openProjectToolWindow(project, uiAccess);
      openFileFromCommandLineAsync(project, virtualFile, line, uiAccess);

      projectManager.openProjectAsync(project, uiAccess).doWhenProcessed((value) -> {
         if(value == Boolean.FALSE) {
           WelcomeFrame.showIfNoProjectOpened();

           ApplicationManager.getApplication().runWriteAction(() -> Disposer.dispose(project));
         }

        if (callback != null && runConfigurators.get()) {
          callback.consume(project);
        }
      });
    };

    result.doWhenDone(afterProjectAction);

    if (projectDir.exists()) {
      for (ProjectOpenProcessor processor : ProjectOpenProcessors.getInstance().getProcessors()) {
        processor.refreshProjectFiles(projectDir);
      }

      AsyncResult<Project> anotherResult = new AsyncResult<>();
      anotherResult.doWhenDone((project) -> {
        ThrowableComputable<Module[], RuntimeException> action = () -> ModuleManager.getInstance(project).getModules();
        final Module[] modules = AccessRule.read(action);
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
