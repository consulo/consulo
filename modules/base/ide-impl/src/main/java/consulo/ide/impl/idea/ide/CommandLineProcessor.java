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
package consulo.ide.impl.idea.ide;

import consulo.application.Application;
import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.container.internal.ShowErrorCaller;
import consulo.module.content.ProjectRootManager;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessors;
import consulo.project.internal.ProjectOpenService;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.UIAccess;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * @author yole
 */
public class CommandLineProcessor {
  private CommandLineProcessor() {
  }

  public static CompletableFuture<Project> processExternalCommandLine(CommandLineArgs commandLineArgs,
                                                                      @Nullable String currentDirectory) {
    String file = commandLineArgs.getFile();
    if (file == null) {
      return CompletableFuture.completedFuture(null);
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
      ProjectOpenContext context = new ProjectOpenContext();
      context.putUserData(ProjectOpenContext.FORCE_OPEN_IN_NEW_FRAME, true);

      return Application.get().getInstance(ProjectOpenService.class)
          .openProjectAsync(projectFile.toPath(), uiAccess, context)
          .whenComplete((project, error) -> {
            if (error == null && project != null) {
              if (!FileUtil.filesEqual(projectFile, targetFile) && !targetFile.isDirectory()) {
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
                if (virtualFile != null) {
                  openFile(uiAccess, project, virtualFile, line);
                }
              }
            }
          });
    }
    else {
      VirtualFile targetVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
      if (targetVFile == null) {
        ShowErrorCaller.showErrorDialog("Cannot find file", "Cannot find file '" + file + "'", null);
        return CompletableFuture.completedFuture(null);
      }

      Project bestProject = findBestProject(targetVFile);

      openFile(uiAccess, bestProject, targetVFile, line);

      return CompletableFuture.completedFuture(bestProject);
    }
  }

  private static void openFile(UIAccess uiAccess, Project project, VirtualFile virtualFile, int line) {
    uiAccess.give(() -> {
      OpenFileDescriptorFactory.Builder builder = OpenFileDescriptorFactory.getInstance(project).newBuilder(virtualFile);
      if (line != -1) {
        builder = builder.line(line - 1);
      }

      builder.build().navigate(true);
    });
  }

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

  private static @Nullable File findProjectDirectoryOrFile(String path) {
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
