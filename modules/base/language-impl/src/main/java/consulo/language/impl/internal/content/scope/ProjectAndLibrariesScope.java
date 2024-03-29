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

import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.UnloadedModuleDescription;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiBundle;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectAndLibrariesScope extends GlobalSearchScope {
  protected final ProjectFileIndex myProjectFileIndex;
  private String myDisplayName = PsiBundle.message("psi.search.scope.project.and.libraries");

  public ProjectAndLibrariesScope(@Nonnull Project project) {
    super(project);
    myProjectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
  }

  /**
   * @deprecated use {@link #ProjectAndLibrariesScope(Project)}
   */
  @Deprecated
  public ProjectAndLibrariesScope(Project project, boolean searchOutsideRootModel) {
    this(project);
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    return myProjectFileIndex.isInContent(file) || myProjectFileIndex.isInLibrary(file);
  }

  @Override
  public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    List<OrderEntry> entries1 = myProjectFileIndex.getOrderEntriesForFile(file1);
    List<OrderEntry> entries2 = myProjectFileIndex.getOrderEntriesForFile(file2);
    if (entries1.size() != entries2.size()) return 0;

    int res = 0;
    for (OrderEntry entry1 : entries1) {
      Module module = entry1.getOwnerModule();
      ModuleFileIndex moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex();
      OrderEntry entry2 = moduleFileIndex.getOrderEntryForFile(file2);
      if (entry2 == null) {
        return 0;
      }
      else {
        int aRes = entry2.compareTo(entry1);
        if (aRes == 0) return 0;
        if (res == 0) {
          res = aRes;
        }
        else if (res != aRes) {
          return 0;
        }
      }
    }

    return res;
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule) {
    return true;
  }

  @Override
  public boolean isSearchInLibraries() {
    return true;
  }

  @Nonnull
  @Override
  public Collection<UnloadedModuleDescription> getUnloadedModulesBelongingToScope() {
    Project project = getProject();
    return project != null ? ModuleManager.getInstance(project).getUnloadedModuleDescriptions() : Collections.emptySet();
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return myDisplayName;
  }

  public void setDisplayName(@Nonnull String displayName) {
    myDisplayName = displayName;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }
}