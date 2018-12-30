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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.startup.DumbAwareStartupAction;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

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
    return file.isDirectory() && new File(file, Project.DIRECTORY_STORE_FOLDER + "/modules.xml").exists();
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

  @Override
  public void doOpenProjectAsync(@Nonnull AsyncResult<Project> result, @Nonnull VirtualFile virtualFile, @Nullable Project projectToClose, boolean forceOpenInNewFrame, @Nonnull UIAccess uiAccess) {

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

    if (projectDir.exists()) {
      AsyncResult<Project> projectAsync = projectManager.openProjectAsync(baseDir, uiAccess);

      projectAsync.doWhenDone(project -> openFileFromCommandLineAsync(project, virtualFile, -1));
      projectAsync.doWhenRejected(WelcomeFrame::showIfNoProjectOpened);
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

  private static void openFileFromCommandLineAsync(@Nonnull Project project, final VirtualFile virtualFile, final int line) {
    //noinspection RedundantCast
    StartupManager.getInstance(project).registerPostStartupActivity((DumbAwareStartupAction)(ui) -> {
      if (project.isDisposed()) {
        return;
      }

      ui.give(() -> {
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
}
