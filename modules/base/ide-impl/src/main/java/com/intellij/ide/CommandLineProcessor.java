/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide;

import com.intellij.ide.impl.ProjectUtil;
import consulo.application.Application;
import com.intellij.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.module.content.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.project.impl.ProjectOpenProcessors;
import consulo.start.CommandLineArgs;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * @author yole
 */
public class CommandLineProcessor {
  private CommandLineProcessor() {
  }

  @Nonnull
  public static AsyncResult<Project> processExternalCommandLine(@Nonnull CommandLineArgs commandLineArgs, @Nullable String currentDirectory) {
    String file = commandLineArgs.getFile();
    if (file == null) {
      return AsyncResult.rejected();
    }
    int line = commandLineArgs.getLine();

    if (StringUtil.isQuotedString(file)) {
      file = StringUtil.stripQuotesAroundValue(file);
    }

    if (!new File(file).isAbsolute()) {
      file = currentDirectory != null ? new File(currentDirectory, file).getAbsolutePath() : new File(file).getAbsolutePath();
    }

    File projectFile = findProjectDirectoryOrFile(file);

    File targetFile = new File(file);

    UIAccess uiAccess = Application.get().getLastUIAccess();
    if (projectFile != null) {
      return ProjectUtil.openAsync(projectFile.getPath(), null, true, uiAccess).doWhenDone(project -> {
        if (!FileUtil.filesEqual(projectFile, targetFile) && !targetFile.isDirectory()) {
          final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
          if (virtualFile != null) {
            openFile(uiAccess, project, virtualFile, line);
          }
        }
      });
    }
    else {
      final VirtualFile targetVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
      if (targetVFile == null) {
        Messages.showErrorDialog("Cannot find file '" + file + "'", "Cannot find file");
        return AsyncResult.rejected();
      }

      Project bestProject = findBestProject(targetVFile);

      openFile(uiAccess, bestProject, targetVFile, line);

      return AsyncResult.resolved(bestProject);
    }
  }

  private static void openFile(@Nonnull UIAccess uiAccess, @Nonnull Project project, @Nonnull VirtualFile virtualFile, int line) {
    uiAccess.give(() -> {
      if (line == -1) {
        new OpenFileDescriptorImpl(project, virtualFile).navigate(true);
      }
      else {
        new OpenFileDescriptorImpl(project, virtualFile, line - 1, 0).navigate(true);
      }
    });
  }

  @Nonnull
  private static Project findBestProject(VirtualFile virtualFile) {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project aProject : projects) {
      if (ProjectRootManager.getInstance(aProject).getFileIndex().isInContent(virtualFile)) {
        return aProject;
      }
    }
    IdeFrame frame = (IdeFrame)IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
    Project project = frame == null ? null : frame.getProject();
    return project != null ? project : projects[0];
  }

  @Nullable
  private static File findProjectDirectoryOrFile(@Nonnull String path) {
    File target = new File(path);

    while (target != null) {
      ProjectOpenProcessor processor = ProjectOpenProcessors.getInstance().findProcessor(target);
      if (processor != null) {
        return target;
      }

      target = target.getParentFile();
    }

    return null;
  }
}
