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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.platform.PlatformProjectOpenProcessor;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.start.CommandLineArgs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * @author yole
 */
public class CommandLineProcessor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.CommandLineProcessor");

  private CommandLineProcessor() {
  }

  public static void openFileOrProject(final String name) {
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (name != null) {
          doOpenFileOrProject(name);
        }
      }
    });
  }

  @Nullable
  private static Project doOpenFileOrProject(String name) {
    final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(name);
    if (virtualFile == null) {
      Messages.showErrorDialog("Cannot find file '" + name + "'", "Cannot find file");
      return null;
    }
    ProjectOpenProcessor provider = ProjectOpenProcessor.findProcessor(virtualFile);
    if (provider instanceof PlatformProjectOpenProcessor && !virtualFile.isDirectory()) {
      // HACK: PlatformProjectOpenProcessor agrees to open anything
      provider = null;
    }
    if (provider != null || new File(name, Project.DIRECTORY_STORE_FOLDER).exists()) {
      final Project result = ProjectUtil.open(name, null, true);
      if (result == null) {
        Messages.showErrorDialog("Cannot open project '" + name + "'", "Cannot open project");
      }
      return result;
    }
    else {
      return doOpenFile(virtualFile, -1);
    }
  }

  @Nullable
  private static Project doOpenFile(VirtualFile virtualFile, int line) {
    final Project[] projects = ProjectManager.getInstance().getOpenProjects();
    if (projects.length == 0) {
      return PlatformProjectOpenProcessor.doOpenProject(virtualFile, null, false, line, null);
    }
    else {
      Project project = findBestProject(virtualFile, projects);
      if (line == -1) {
        new OpenFileDescriptor(project, virtualFile).navigate(true);
      }
      else {
        new OpenFileDescriptor(project, virtualFile, line - 1, 0).navigate(true);
      }
      return project;
    }
  }

  @NotNull
  private static Project findBestProject(VirtualFile virtualFile, Project[] projects) {
    for (Project aProject : projects) {
      if (ProjectRootManager.getInstance(aProject).getFileIndex().isInContent(virtualFile)) {
        return aProject;
      }
    }
    IdeFrame frame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
    Project project = frame == null ? null : frame.getProject();
    return project != null ? project : projects[0];
  }

  @Nullable
  public static Project processExternalCommandLine(CommandLineArgs commandLineArgs, @Nullable String currentDirectory) {
    String file = commandLineArgs.getFile();
    if (file == null) {
      return null;
    }
    Project lastOpenedProject = null;
    int line = commandLineArgs.getLine();

    if (StringUtil.isQuotedString(file)) {
      file = StringUtil.stripQuotesAroundValue(file);
    }
    if (!new File(file).isAbsolute()) {
      file = currentDirectory != null ? new File(currentDirectory, file).getAbsolutePath() : new File(file).getAbsolutePath();
    }
    if (line != -1) {
      final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(file);
      if (virtualFile != null) {
        lastOpenedProject = doOpenFile(virtualFile, line);
      }
      else {
        Messages.showErrorDialog("Cannot find file '" + file + "'", "Cannot find file");
      }
    }
    else {
      lastOpenedProject = doOpenFileOrProject(file);
    }
    return lastOpenedProject;
  }
}
