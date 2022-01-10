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
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactModel;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
* @author nik
*/
public class DefaultPackagingElementResolvingContext implements PackagingElementResolvingContext {
  @Nullable
  public static Library findLibrary(Project project, String level, String libraryName) {
    LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(level, project);
    return table != null ? table.getLibraryByName(libraryName) : null;
  }

  private final Project myProject;
  private final ArtifactManager myArtifactManager;
  private final ModulesProvider myModulesProvider;

  public DefaultPackagingElementResolvingContext(Project project, ArtifactManager artifactManager) {
    myProject = project;
    myArtifactManager = artifactManager;
    myModulesProvider = DefaultModulesProvider.of(myProject);
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  @Nonnull
  public ArtifactModel getArtifactModel() {
    return myArtifactManager;
  }

  @Override
  @Nonnull
  public ModulesProvider getModulesProvider() {
    return myModulesProvider;
  }

  @Override
  public Library findLibrary(@Nonnull String level, @Nonnull String libraryName) {
    return findLibrary(myProject, level, libraryName);
  }
}
