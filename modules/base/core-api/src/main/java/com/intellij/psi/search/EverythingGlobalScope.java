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
package com.intellij.psi.search;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

public class EverythingGlobalScope extends GlobalSearchScope {
  public EverythingGlobalScope(Project project) {
    super(project);
  }

  public EverythingGlobalScope() {
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "All Places";
  }

  @Override
  public int compare(@Nonnull final VirtualFile file1, @Nonnull final VirtualFile file2) {
    return 0;
  }

  @Override
  public boolean contains(@Nonnull final VirtualFile file) {
    return true;
  }

  @Override
  public boolean isSearchInLibraries() {
    return true;
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull final Module aModule) {
    return true;
  }

  @Override
  public boolean isSearchOutsideRootModel() {
    return true;
  }

  @Nonnull
  @Override
  public GlobalSearchScope union(@Nonnull SearchScope scope) {
    return this;
  }

  @Nonnull
  @Override
  public SearchScope intersectWith(@Nonnull SearchScope scope2) {
    return scope2;
  }
}