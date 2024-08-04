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
package consulo.project.impl.internal;

import consulo.application.util.concurrent.PooledAsyncResult;
import consulo.logging.Logger;
import consulo.project.*;
import consulo.project.internal.*;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Alert;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Eugene Belyaev
 */
public class ProjectImplUtil {
  private static final Logger LOG = Logger.getInstance(ProjectImplUtil.class);

  private ProjectImplUtil() {
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
  private static AsyncResult<Integer> confirmOpenNewProjectAsync(Project projectToClose, UIAccess uiAccess, boolean isNewProject) {
    final ProjectOpenSetting settings = ProjectOpenSetting.getInstance();
    int confirmOpenNewProject = settings.getConfirmOpenNewProject();
    if (confirmOpenNewProject == ProjectOpenSetting.OPEN_PROJECT_ASK) {
      Alert<Integer> alert = Alert.create();
      alert.asQuestion();
      alert.remember(ProjectNewWindowDoNotAskOption.INSTANCE);
      alert.title(isNewProject ? ProjectLocalize.titleNewProject() : ProjectLocalize.titleOpenProject());
      alert.text(ProjectLocalize.promptOpenProjectInNewFrame());

      alert.button(ProjectLocalize.buttonExistingframe(), () -> ProjectOpenSetting.OPEN_PROJECT_SAME_WINDOW);
      alert.asDefaultButton();

      alert.button(ProjectLocalize.buttonNewframe(), () -> ProjectOpenSetting.OPEN_PROJECT_NEW_WINDOW);

      alert.button(Alert.CANCEL, Alert.CANCEL);
      alert.asExitButton();

      AsyncResult<Integer> result = AsyncResult.undefined();
      uiAccess.give(() -> {
        if (projectToClose != null) {
          return alert.showAsync(projectToClose).notify(result);
        }
        else {
          return alert.showAsync().notify(result);
        }
      });
      return result;
    }

    return AsyncResult.resolved(confirmOpenNewProject);
  }

  @Nonnull
  public static AsyncResult<Project> openAsync(@Nonnull String path,
                                               @Nullable final Project projectToCloseFinal,
                                               boolean forceOpenInNewFrame,
                                               @Nonnull UIAccess uiAccess) {
    final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);

    if (virtualFile == null) return AsyncResult.rejected("file path not find");

    return PooledAsyncResult.create((result) -> {
      ProjectOpenProcessor provider = ProjectOpenProcessors.getInstance().findProcessor(VirtualFileUtil.virtualToIoFile(virtualFile));
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
          confirmOpenNewProjectAsync(finalProjectToClose, uiAccess, false).doWhenDone(exitCode -> {
            if (exitCode == ProjectOpenSetting.OPEN_PROJECT_SAME_WINDOW) {
              AsyncResult<Void> closeResult = ProjectManagerEx.getInstanceEx().closeAndDisposeAsync(finalProjectToClose, uiAccess);
              closeResult.doWhenDone((Runnable)reopenAsync::setDone);
              closeResult.doWhenRejected(() -> result.reject("not closed project"));
            }
            else if (exitCode != ProjectOpenSetting.OPEN_PROJECT_NEW_WINDOW) { // not in a new window
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
