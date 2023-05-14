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
package consulo.ide.impl.idea.packaging.impl.artifacts;

import consulo.project.Project;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablesRegistrar;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.DefaultModulesProvider;
import consulo.module.content.layer.ModulesProvider;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactModel;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
