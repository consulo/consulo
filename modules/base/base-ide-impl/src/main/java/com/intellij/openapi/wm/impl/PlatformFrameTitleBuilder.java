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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtilCore;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * @author yole
 */
@Singleton
public class PlatformFrameTitleBuilder extends FrameTitleBuilder {
  private final UISettings myUISettings;
  private final ProjectManager myProjectManager;

  @Inject
  public PlatformFrameTitleBuilder(UISettings uiSettings, ProjectManager projectManager) {
    myUISettings = uiSettings;
    myProjectManager = projectManager;
  }

  @Nonnull
  @RequiredReadAction
  @Override
  public String getProjectTitle(@Nonnull final Project project) {
    String basePath = project.getBasePath();
    if (basePath == null) return project.getName();

    Project[] projects = myProjectManager.getOpenProjects();
    int sameNamedProjects = ContainerUtil.count(Arrays.asList(projects), (it) -> it.getName().equals(project.getName()));
    if (sameNamedProjects == 1 && !myUISettings.getFullPathsInWindowHeader()) {
      return project.getName();
    }

    basePath = FileUtil.toSystemDependentName(basePath);
    if (basePath.equals(project.getName()) && !myUISettings.getFullPathsInWindowHeader()) {
      return "[" + FileUtil.getLocationRelativeToUserHome(basePath) + "]";
    }
    else {
      return project.getName() + " [" + FileUtil.getLocationRelativeToUserHome(basePath) + "]";
    }
  }

  @Nonnull
  @RequiredReadAction
  @Override
  public String getFileTitle(@Nonnull final Project project, @Nonnull final VirtualFile file) {
    String fileTitle = VfsPresentationUtil.getPresentableNameForUI(project, file);
    if (!fileTitle.endsWith(file.getPresentableName()) || file.getParent() == null) {
      return fileTitle;
    }

    if (myUISettings.getFullPathsInWindowHeader()) {
      return ProjectUtilCore.displayUrlRelativeToProject(file, file.getPresentableUrl(), project, true, false);
    }

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    if (!fileIndex.isInContent(file)) {
      String pathWithLibrary = ProjectUtilCore.decorateWithLibraryName(file, project, file.getPresentableName());
      if (pathWithLibrary != null) {
        return pathWithLibrary;
      }
      return FileUtil.getLocationRelativeToUserHome(file.getPresentableUrl());
    }

    return ProjectUtilCore.appendModuleName(file, project, fileTitle, false);
  }
}
