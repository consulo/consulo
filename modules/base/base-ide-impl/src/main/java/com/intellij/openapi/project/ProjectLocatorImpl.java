/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.project;

import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Singleton
public class ProjectLocatorImpl extends ProjectLocator {
  private final ProjectManager myProjectManager;

  @Inject
  public ProjectLocatorImpl(ProjectManager projectManager) {
    myProjectManager = projectManager;
  }

  @Override
  @Nullable
  public Project guessProjectForFile(final VirtualFile file) {
    // StubUpdatingIndex calls this method very often, so, optimized implementation is required
    Project project = ProjectCoreUtil.theOnlyOpenProject();
    if (project != null && !project.isDisposed()) {
      return project;
    }

    if(file != null) {
      Project preferredProject = getPreferredProject(file);
      if(preferredProject != null) {
        return preferredProject;
      }
    }

    final Project[] projects = myProjectManager.getOpenProjects();
    if (projects.length == 0) return null;
    if (projects.length == 1 && !projects[0].isDisposed()) return projects[0];

    if (file != null) {
      for (Project openProject : projects) {
        if (openProject.isInitialized() && !openProject.isDisposed() && ProjectRootManager.getInstance(openProject).getFileIndex().isInContent(file)) {
          return openProject;
        }
      }
    }

    return !projects[0].isDisposed() ? projects[0] : null;
  }

  @Override
  @Nonnull
  public Collection<Project> getProjectsForFile(VirtualFile file) {
    if (file == null) {
      return Collections.emptyList();
    }

    Project[] openProjects = myProjectManager.getOpenProjects();
    if (openProjects.length == 0) {
      return Collections.emptyList();
    }

    List<Project> result = new ArrayList<>(openProjects.length);
    for (Project openProject : openProjects) {
      if (openProject.isInitialized() && !openProject.isDisposed() && ProjectRootManager.getInstance(openProject).getFileIndex().isInContent(file)) {
        result.add(openProject);
      }
    }

    return result;
  }
}