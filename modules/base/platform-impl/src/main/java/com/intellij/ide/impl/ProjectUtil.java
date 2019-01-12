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
package com.intellij.ide.impl;

import com.intellij.CommonBundle;
import com.intellij.ide.GeneralSettings;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.openapi.components.impl.stores.IProjectStore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.intellij.ui.AppIcon;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.annotations.DeprecationInfo;
import consulo.application.AccessRule;
import consulo.application.DefaultPaths;
import consulo.async.ex.PooledAsyncResult;
import consulo.project.ProjectOpenProcessors;
import consulo.ui.AlertBuilder;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * @author Eugene Belyaev
 */
public class ProjectUtil {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.impl.ProjectUtil");

  private ProjectUtil() {
  }

  public static boolean isSameProject(@Nullable String projectFilePath, @Nonnull Project project) {
    if (projectFilePath == null) return false;

    IProjectStore projectStore = ((ProjectEx)project).getStateStore();
    String existingBaseDirPath = projectStore.getProjectBasePath();
    if (existingBaseDirPath == null) {
      // could be null if not yet initialized
      return false;
    }

    File projectFile = new File(projectFilePath);
    if (projectFile.isDirectory()) {
      return FileUtil.pathsEqual(projectFilePath, existingBaseDirPath);
    }


    File parent = projectFile.getParentFile();
    if (parent.getName().equals(Project.DIRECTORY_STORE_FOLDER)) {
      parent = parent.getParentFile();
      return parent != null && FileUtil.pathsEqual(parent.getPath(), existingBaseDirPath);
    }
    return false;
  }

  public static void updateLastProjectLocation(final String projectFilePath) {
    File lastProjectLocation = new File(projectFilePath);
    if (lastProjectLocation.isFile()) {
      lastProjectLocation = lastProjectLocation.getParentFile(); // for directory-based project storage
    }
    if (lastProjectLocation == null) { // the immediate parent of the ipr file
      return;
    }
    lastProjectLocation = lastProjectLocation.getParentFile(); // the candidate directory to be saved
    if (lastProjectLocation == null) {
      return;
    }
    String path = lastProjectLocation.getPath();
    try {
      path = FileUtil.resolveShortWindowsName(path);
    }
    catch (IOException e) {
      LOG.info(e);
      return;
    }
    RecentProjectsManager.getInstance().setLastProjectCreationLocation(path.replace(File.separatorChar, '/'));
  }

  /**
   * @param project cannot be null
   */
  @RequiredUIAccess
  public static boolean closeAndDispose(@Nonnull final Project project) {
    return ProjectManagerEx.getInstanceEx().closeAndDispose(project).getResultSync();
  }

  @Deprecated
  @DeprecationInfo("ProjectUtil#open()")
  @RequiredUIAccess
  @SuppressWarnings({"unused", "deprecation"})
  public static Project openOrImport(@Nonnull final String path, final Project projectToClose, boolean forceOpenInNewFrame) {
    return open(path, projectToClose, forceOpenInNewFrame);
  }

  /**
   * @param path                project file path
   * @param projectToClose      currently active project
   * @param forceOpenInNewFrame forces opening in new frame
   * @return project by path if the path was recognized as Consulo project file or one of the project formats supported by
   * installed importers (regardless of opening/import result)
   * null otherwise
   */
  @Nullable
  @Deprecated
  @DeprecationInfo("Use #openAsync()")
  public static Project open(@Nonnull final String path, final Project projectToClose, boolean forceOpenInNewFrame) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return {@link com.intellij.ide.GeneralSettings#OPEN_PROJECT_SAME_WINDOW}
   * {@link com.intellij.ide.GeneralSettings#OPEN_PROJECT_NEW_WINDOW}
   * {@link com.intellij.openapi.ui.Messages#CANCEL} - if user canceled the dialog
   */
  @Deprecated
  @DeprecationInfo("confirmOpenNewProjectAsync()")
  public static int confirmOpenNewProject(boolean isNewProject) {
    final GeneralSettings settings = GeneralSettings.getInstance();
    int confirmOpenNewProject = settings.getConfirmOpenNewProject();
    if (confirmOpenNewProject == GeneralSettings.OPEN_PROJECT_ASK) {
      if (isNewProject) {
        int exitCode = Messages.showYesNoDialog(IdeBundle.message("prompt.open.project.in.new.frame"), IdeBundle.message("title.new.project"), IdeBundle.message("button.existingframe"),
                                                IdeBundle.message("button.newframe"), Messages.getQuestionIcon(), new ProjectNewWindowDoNotAskOption());
        return exitCode == 0 ? GeneralSettings.OPEN_PROJECT_SAME_WINDOW : GeneralSettings.OPEN_PROJECT_NEW_WINDOW;
      }
      else {
        int exitCode = Messages.showYesNoCancelDialog(IdeBundle.message("prompt.open.project.in.new.frame"), IdeBundle.message("title.open.project"), IdeBundle.message("button.existingframe"),
                                                      IdeBundle.message("button.newframe"), CommonBundle.getCancelButtonText(), Messages.getQuestionIcon(), new ProjectNewWindowDoNotAskOption());
        return exitCode == 0 ? GeneralSettings.OPEN_PROJECT_SAME_WINDOW : exitCode == 1 ? GeneralSettings.OPEN_PROJECT_NEW_WINDOW : Messages.CANCEL;
      }
    }
    return confirmOpenNewProject;
  }

  @Nonnull
  public static AsyncResult<Integer> confirmOpenNewProjectAsync(UIAccess uiAccess, boolean isNewProject) {
    final GeneralSettings settings = GeneralSettings.getInstance();
    int confirmOpenNewProject = settings.getConfirmOpenNewProject();
    if (confirmOpenNewProject == GeneralSettings.OPEN_PROJECT_ASK) {
      AlertBuilder<Integer> alertBuilder = AlertBuilder.create();
      alertBuilder.asQuestion();

      if (isNewProject) {
        alertBuilder.title(IdeBundle.message("title.new.project"));
        alertBuilder.text(IdeBundle.message("prompt.open.project.in.new.frame"));

        alertBuilder.button(IdeBundle.message("button.existingframe"), () -> GeneralSettings.OPEN_PROJECT_SAME_WINDOW);
        alertBuilder.markDefault();
        alertBuilder.button(IdeBundle.message("button.newframe"), () -> GeneralSettings.OPEN_PROJECT_NEW_WINDOW);
      }
      else {
        alertBuilder.title(IdeBundle.message("title.open.project"));
        alertBuilder.text(IdeBundle.message("prompt.open.project.in.new.frame"));

        alertBuilder.button(IdeBundle.message("button.existingframe"), () -> GeneralSettings.OPEN_PROJECT_SAME_WINDOW);
        alertBuilder.markDefault();
        alertBuilder.button(IdeBundle.message("button.newframe"), () -> GeneralSettings.OPEN_PROJECT_NEW_WINDOW);
        alertBuilder.button(AlertBuilder.CANCEL, () -> AlertBuilder.CANCEL);
      }

      AsyncResult<Integer> result = new AsyncResult<>();
      uiAccess.give(() -> alertBuilder.show().notify(result));
      return result;
    }

    return AsyncResult.resolved(GeneralSettings.OPEN_PROJECT_NEW_WINDOW);
  }

  public static void focusProjectWindow(final Project p, boolean executeIfAppInactive) {
    FocusCommand cmd = new FocusCommand() {
      @Nonnull
      @Override
      public AsyncResult<Void> run() {
        JFrame f = WindowManager.getInstance().getFrame(p);
        if (f != null) {
          f.toFront();
          //f.requestFocus();
        }
        return AsyncResult.resolved();
      }
    };

    if (executeIfAppInactive) {
      AppIcon.getInstance().requestFocus((IdeFrame)WindowManager.getInstance().getFrame(p));
      cmd.run();
    }
    else {
      IdeFocusManager.getInstance(p).requestFocus(cmd, false);
    }
  }

  @Nonnull
  public static String getBaseDir() {
    final String lastProjectLocation = RecentProjectsManager.getInstance().getLastProjectCreationLocation();
    if (lastProjectLocation != null) {
      return lastProjectLocation.replace('/', File.separatorChar);
    }
    return DefaultPaths.getInstance().getDocumentsDir();
  }

  @Nonnull
  public static AsyncResult<Project> openAsync(@Nonnull String path, @Nullable final Project projectToCloseFinal, boolean forceOpenInNewFrame, @Nonnull UIAccess uiAccess) {
    final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);

    if (virtualFile == null) return AsyncResult.rejected("file path not find");

    return PooledAsyncResult.create((result) -> {
      ProjectOpenProcessor provider = ProjectOpenProcessors.getInstance().findProcessor(VfsUtilCore.virtualToIoFile(virtualFile));
      if (provider != null) {
        result.doWhenDone((project) -> {
          uiAccess.give(() -> {
            if (project.isDisposed()) return;

            final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);

            if (toolWindow != null && toolWindow.getType() != ToolWindowType.SLIDING) {
              toolWindow.activate(null);
            }
          });
        });

        result.doWhenRejected(WelcomeFrame::showIfNoProjectOpened);

        AsyncResult<Void> reopenAsync = new AsyncResult<>();

        Project projectToClose = projectToCloseFinal;
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (!forceOpenInNewFrame && openProjects.length > 0) {
          if (projectToClose == null) {
            projectToClose = openProjects[openProjects.length - 1];
          }

          final Project finalProjectToClose = projectToClose;
          confirmOpenNewProjectAsync(uiAccess, false).doWhenDone(exitCode -> {
            if (exitCode == GeneralSettings.OPEN_PROJECT_SAME_WINDOW) {
              AccessRule.writeAsync(() -> {
                ProjectManagerEx.getInstanceEx().closeAndDisposeAsync(finalProjectToClose, uiAccess).doWhenDone((value) -> {
                  if (value == Boolean.FALSE) {
                    result.reject("not closed project");
                  }
                  else {
                    reopenAsync.setDone();
                  }
                });
              });
            }
            else if (exitCode != GeneralSettings.OPEN_PROJECT_NEW_WINDOW) { // not in a new window
              result.reject("not open in new window");
            }
            else {
              reopenAsync.setDone();
            }
          });
        }
        else {
          reopenAsync.setDone();
        }

        // we need reexecute it due after dialog - it will be executed in ui thread
        reopenAsync.doWhenDone(() -> AppExecutorUtil.getAppExecutorService().execute(() -> provider.doOpenProjectAsync(virtualFile, uiAccess).notify(result)));
      }
      else {
        result.reject("provider for file path is not find");
      }
    });
  }
}
