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
package com.intellij.openapi.module.impl.scopes;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleScopeProvider;
import com.intellij.psi.search.GlobalSearchScope;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * Author: dmitrylomov
 */
@Singleton
public class ModuleScopeProviderImpl implements ModuleScopeProvider {
  private final Module myModule;
  private final ConcurrentIntObjectMap<GlobalSearchScope> myScopeCache = IntMaps.newConcurrentIntObjectHashMap();
  private ModuleWithDependentsTestScope myModuleTestsWithDependentsScope;

  @Inject
  public ModuleScopeProviderImpl(@Nonnull Module module) {
    myModule = module;
  }

  @Nonnull
  private GlobalSearchScope getCachedScope(@ModuleWithDependenciesScope.ScopeConstant int options) {
    GlobalSearchScope scope = myScopeCache.get(options);
    if (scope == null) {
      scope = new ModuleWithDependenciesScope(myModule, options);
      myScopeCache.put(options, scope);
    }
    return scope;
  }

  @Override
  @Nonnull
  public GlobalSearchScope getModuleScope() {
    return getCachedScope(ModuleWithDependenciesScope.COMPILE | ModuleWithDependenciesScope.TESTS);
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleScope(boolean includeTests) {
    return getCachedScope(ModuleWithDependenciesScope.COMPILE | (includeTests ? ModuleWithDependenciesScope.TESTS : 0));
  }

  @Override
  @Nonnull
  public GlobalSearchScope getModuleWithLibrariesScope() {
    return getCachedScope(ModuleWithDependenciesScope.COMPILE | ModuleWithDependenciesScope.TESTS | ModuleWithDependenciesScope.LIBRARIES);
  }

  @Override
  @Nonnull
  public GlobalSearchScope getModuleWithDependenciesScope() {
    return getCachedScope(ModuleWithDependenciesScope.COMPILE | ModuleWithDependenciesScope.TESTS | ModuleWithDependenciesScope.MODULES);
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleContentScope() {
    return getCachedScope(ModuleWithDependenciesScope.CONTENT);
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleContentWithDependenciesScope() {
    return getCachedScope(ModuleWithDependenciesScope.CONTENT | ModuleWithDependenciesScope.MODULES);
  }

  @Override
  @Nonnull
  public GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests) {
    return getCachedScope(ModuleWithDependenciesScope.COMPILE |
                          ModuleWithDependenciesScope.MODULES |
                          ModuleWithDependenciesScope.LIBRARIES | (includeTests ? ModuleWithDependenciesScope.TESTS : 0));
  }

  @Override
  @Nonnull
  public GlobalSearchScope getModuleWithDependentsScope() {
    return getModuleTestsWithDependentsScope().getBaseScope();
  }

  @Override
  @Nonnull
  public ModuleWithDependentsTestScope getModuleTestsWithDependentsScope() {
    ModuleWithDependentsTestScope scope = myModuleTestsWithDependentsScope;
    if (scope == null) {
      myModuleTestsWithDependentsScope = scope = new ModuleWithDependentsTestScope(myModule);
    }
    return scope;
  }

  @Override
  @Nonnull
  public GlobalSearchScope getModuleRuntimeScope(boolean includeTests) {
    return getCachedScope(
            ModuleWithDependenciesScope.MODULES | ModuleWithDependenciesScope.LIBRARIES | (includeTests ? ModuleWithDependenciesScope.TESTS : 0));
  }

  public void clearCache() {
    myScopeCache.clear();
    myModuleTestsWithDependentsScope = null;
  }
}
