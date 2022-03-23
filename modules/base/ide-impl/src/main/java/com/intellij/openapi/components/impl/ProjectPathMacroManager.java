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
package com.intellij.openapi.components.impl;

import consulo.application.impl.internal.macro.PathMacrosImpl;
import consulo.application.macro.PathMacros;
import consulo.component.impl.macro.BasePathMacroManager;
import consulo.component.macro.ExpandMacroToPathMap;
import consulo.component.macro.ReplacePathToMacroMap;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;

@Singleton
public class ProjectPathMacroManager extends BasePathMacroManager {
  public static ProjectPathMacroManager getInstance(Project project) {
    return project.getInstance(ProjectPathMacroManager.class);
  }

  private final Project myProject;

  @Inject
  public ProjectPathMacroManager(PathMacros pathMacros, Project project) {
    super(pathMacros);
    myProject = project;
  }

  @Override
  public ExpandMacroToPathMap getExpandMacroMap() {
    final ExpandMacroToPathMap result = super.getExpandMacroMap();
    addFileHierarchyReplacements(result, PathMacrosImpl.PROJECT_DIR_MACRO_NAME, getProjectDir(myProject));
    return result;
  }

  @Override
  public ReplacePathToMacroMap getReplacePathMap() {
    final ReplacePathToMacroMap result = super.getReplacePathMap();
    addFileHierarchyReplacements(result, PathMacrosImpl.PROJECT_DIR_MACRO_NAME, getProjectDir(myProject), null);
    return result;
  }

  @Nullable
  private static String getProjectDir(Project project) {
    final VirtualFile baseDir = project.getBaseDir();
    return baseDir != null ? baseDir.getPath() : null;
  }
}
