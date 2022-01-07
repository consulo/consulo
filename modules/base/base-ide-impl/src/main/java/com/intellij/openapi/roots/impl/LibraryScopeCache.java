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
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.scopes.LibraryRuntimeClasspathScope;
import com.intellij.openapi.module.impl.scopes.SdkScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @author yole
 */
@Singleton
public class LibraryScopeCache {
  public static LibraryScopeCache getInstance(Project project) {
    return ServiceManager.getService(project, LibraryScopeCache.class);
  }

  private final Project myProject;
  private final ConcurrentMap<List<Module>, GlobalSearchScope> myLibraryScopes = ContainerUtil.newConcurrentMap();
  private final ConcurrentMap<String, GlobalSearchScope> mySdkScopes = ContainerUtil.newConcurrentMap();
  private final LibrariesOnlyScope myLibrariesOnlyScope;

  @Inject
  public LibraryScopeCache(Project project) {
    myProject = project;
    myLibrariesOnlyScope = new LibrariesOnlyScope(GlobalSearchScope.allScope(myProject), myProject);
  }

  public void clear() {
    myLibraryScopes.clear();
    mySdkScopes.clear();
  }

  @Nonnull
  public GlobalSearchScope getLibrariesOnlyScope() {
    return myLibrariesOnlyScope;
  }

  @Nonnull
  public GlobalSearchScope getScopeForLibraryUsedIn(@Nonnull List<Module> modulesLibraryIsUsedIn) {
    GlobalSearchScope scope = myLibraryScopes.get(modulesLibraryIsUsedIn);
    if (scope != null) {
      return scope;
    }
    GlobalSearchScope newScope = modulesLibraryIsUsedIn.isEmpty()
                                 ? myLibrariesOnlyScope
                                 : new LibraryRuntimeClasspathScope(myProject, modulesLibraryIsUsedIn);
    return ConcurrencyUtil.cacheOrGet(myLibraryScopes, modulesLibraryIsUsedIn, newScope);
  }

  @Nonnull
  public GlobalSearchScope getScopeForSdk(@Nonnull final ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
    final String jdkName = sdkOrderEntry.getSdkName();
    if (jdkName == null) return GlobalSearchScope.allScope(myProject);
    GlobalSearchScope scope = mySdkScopes.get(jdkName);
    if (scope == null) {
      scope = new SdkScope(myProject, sdkOrderEntry);
      return ConcurrencyUtil.cacheOrGet(mySdkScopes, jdkName, scope);
    }
    return scope;
  }

  private static class LibrariesOnlyScope extends GlobalSearchScope {
    private final GlobalSearchScope myOriginal;
    private final ProjectFileIndex myIndex;

    private LibrariesOnlyScope(@Nonnull GlobalSearchScope original, @Nonnull Project project) {
      super(project);
      myIndex = ProjectRootManager.getInstance(project).getFileIndex();
      myOriginal = original;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return myOriginal.contains(file) && (myIndex.isInLibraryClasses(file) || myIndex.isInLibrarySource(file));
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      return myOriginal.compare(file1, file2);
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return false;
    }

    @Override
    public boolean isSearchOutsideRootModel() {
      return myOriginal.isSearchOutsideRootModel();
    }

    @Override
    public boolean isSearchInLibraries() {
      return true;
    }
  }

}
