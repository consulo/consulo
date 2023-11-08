/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.impl.internal.content.scope;

import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import consulo.language.content.FileIndexFacade;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiBundle;
import jakarta.annotation.Nonnull;

public class ProjectScopeImpl extends GlobalSearchScope {
  private final FileIndexFacade myFileIndex;

  public ProjectScopeImpl(@Nonnull Project project, @Nonnull FileIndexFacade fileIndex) {
    super(project);
    myFileIndex = fileIndex;
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    if (file instanceof VirtualFileWindow) return true;

    if (myFileIndex.isInLibraryClasses(file) && !myFileIndex.isInSourceContent(file)) return false;

    return myFileIndex.isInContent(file);
  }

  @Override
  public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    return 0;
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule) {
    return true;
  }

  @Override
  public boolean isSearchInLibraries() {
    return false;
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return PsiBundle.message("psi.search.scope.project");
  }

  @Override
  public String toString() {
    return getDisplayName();
  }

  @Nonnull
  @Override
  public GlobalSearchScope uniteWith(@Nonnull GlobalSearchScope scope) {
    if (scope == this || !scope.isSearchInLibraries() || !scope.isSearchOutsideRootModel()) return this;
    return super.uniteWith(scope);
  }

  @Nonnull
  @Override
  public GlobalSearchScope intersectWith(@Nonnull GlobalSearchScope scope) {
    if (scope == this) return this;
    if (!scope.isSearchInLibraries()) return scope;
    return super.intersectWith(scope);
  }
}
