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

import com.intellij.ide.GeneralSettings;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.intellij.ui.AppIcon;
import consulo.annotation.DeprecationInfo;
import consulo.async.ex.PooledAsyncResult;
import consulo.components.impl.stores.IProjectStore;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.project.ProjectOpenProcessors;
import consulo.start.WelcomeFrameManager;
import consulo.ui.Alert;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Eugene Belyaev
 */
public class ProjectUtil {
  private static final Logger LOG = Logger.getInstance(ProjectUtil.class);

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
    return ProjectManagerEx.getInstanceEx().closeAndDispose(project);
  }

  @Nonnull
  private static AsyncResult<Integer> confirmOpenNewProjectAsync(UIAccess uiAccess, boolean isNewProject) {
    final GeneralSettings settings = GeneralSettings.getInstance();
    int confirmOpenNewProject = settings.getConfirmOpenNewProject();
    if (confirmOpenNewProject == GeneralSettings.OPEN_PROJECT_ASK) {
      Alert<Integer> alert = Alert.create();
      alert.asQuestion();
      alert.remember(ProjectNewWindowDoNotAskOption.INSTANCE);
      alert.title(isNewProject ? IdeBundle.message("title.new.project") : IdeBundle.message("title.open.project"));
      alert.text(IdeBundle.message("prompt.open.project.in.new.frame"));

      alert.button(IdeBundle.message("button.existingframe"), () -> GeneralSettings.OPEN_PROJECT_SAME_WINDOW);
      alert.asDefaultButton();

      alert.button(IdeBundle.message("button.newframe"), () -> GeneralSettings.OPEN_PROJECT_NEW_WINDOW);

      alert.button(Alert.CANCEL, Alert.CANCEL);
      alert.asExitButton();

      AsyncResult<Integer> result = AsyncResult.undefined();
      uiAccess.give(() -> alert.showAsync().notify(result));
      return result;
    }

    return AsyncResult.resolved(confirmOpenNewProject);
  }

  public static void focusProjectWindow(final Project p, boolean executeIfAppInactive) {
    JFrame f = WindowManager.getInstance().getFrame(p);
    if (f != null) {
      if (executeIfAppInactive) {
        AppIcon.getInstance().requestFocus(WindowManager.getInstance().getIdeFrame(p));
        f.toFront();
      }
      else {
        IdeFocusManager.getInstance(p).requestFocus(f, true);
      }
    }
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("use #openAsync() - just rename method reference")
  public static AsyncResult<Project> openOrOpenAsync(@Nonnull final String path, final Project projectToClose, boolean forceOpenInNewFrame, UIAccess uiAccess) {
    return openAsync(path, projectToClose, forceOpenInNewFrame, uiAccess);
  }

  @Nonnull
  public static Path getProjectsDirectory() {
    final String lastProjectLocation = RecentProjectsManager.getInstance().getLastProjectCreationLocation();
    if (lastProjectLocation != null) {
      return Paths.get(lastProjectLocation);
    }
    return ContainerPathManager.get().getDocumentsDir().toPath();
  }

  @Nonnull
  public static AsyncResult<Project> openAsync(@Nonnull String path, @Nullable final Project projectToCloseFinal, boolean forceOpenInNewFrame, @Nonnull UIAccess uiAccess) {
    final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);

    if (virtualFile == null) return AsyncResult.rejected("file path not find");

    return PooledAsyncResult.create((result) -> {
      ProjectOpenProcessor provider = ProjectOpenProcessors.getInstance().findProcessor(VfsUtilCore.virtualToIoFile(virtualFile));
      if (provider != null) {
        result.doWhenRejected(() -> WelcomeFrameManager.getInstance().showIfNoProjectOpened());

        AsyncResult<Void> reopenAsync = AsyncResult.undefined();

        Project projectToClose = projectToCloseFinal;
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (!forceOpenInNewFrame && openProjects.length > 0) {
          if (projectToClose == null) {
            projectToClose = openProjects[openProjects.length - 1];
          }

          final Project finalProjectToClose = projectToClose;
          confirmOpenNewProjectAsync(uiAccess, false).doWhenDone(exitCode -> {
            if (exitCode == GeneralSettings.OPEN_PROJECT_SAME_WINDOW) {
              AsyncResult<Void> closeResult = ProjectManagerEx.getInstanceEx().closeAndDisposeAsync(finalProjectToClose, uiAccess);
              closeResult.doWhenDone((Runnable)reopenAsync::setDone);
              closeResult.doWhenRejected(() -> result.reject("not closed project"));
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
        reopenAsync.doWhenDone(() -> PooledAsyncResult.create(() -> provider.doOpenProjectAsync(virtualFile, uiAccess)).notify(result));
      }
      else {
        result.reject("provider for file path is not find");
      }
    });
  }
}
