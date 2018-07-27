/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.psi.search;

import com.intellij.core.CoreProjectScopeBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yole
 */
@Singleton
public class ProjectScopeBuilderImpl extends ProjectScopeBuilder {
  protected final Project myProject;

  @Inject
  public ProjectScopeBuilderImpl(Project project) {
    myProject = project;
  }

  @Override
  public GlobalSearchScope buildLibrariesScope() {
    return new ProjectAndLibrariesScope(myProject) {
      @Override
      public boolean contains(@Nonnull VirtualFile file) {
        return myProjectFileIndex.isInLibrarySource(file) || myProjectFileIndex.isInLibraryClasses(file);
      }

      @Override
      public boolean isSearchInModuleContent(@Nonnull Module aModule) {
        return false;
      }
    };
  }

  @Override
  public GlobalSearchScope buildAllScope() {
    final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(myProject);
    if (projectRootManager == null) {
      return new EverythingGlobalScope(myProject);
    }

    return new ProjectAndLibrariesScope(myProject, false);
  }

  @Override
  public GlobalSearchScope buildProjectScope() {
    final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(myProject);
    if (projectRootManager == null) {
      return new EverythingGlobalScope(myProject) {
        @Override
        public boolean isSearchInLibraries() {
          return false;
        }
      };
    }
    else {
      return new ProjectScopeImpl(myProject, FileIndexFacade.getInstance(myProject));
    }
  }

  @Override
  public GlobalSearchScope buildContentScope() {
    return new CoreProjectScopeBuilder.ContentSearchScope(myProject, FileIndexFacade.getInstance(myProject));
  }
}
