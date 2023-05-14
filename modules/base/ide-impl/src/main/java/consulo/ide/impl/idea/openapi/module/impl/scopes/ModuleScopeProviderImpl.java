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
package consulo.ide.impl.idea.openapi.module.impl.scopes;

import consulo.annotation.component.ServiceImpl;
import consulo.module.Module;
import consulo.module.content.scope.ModuleAwareSearchScope;
import consulo.module.content.scope.ModuleScopeProvider;
import consulo.module.content.scope.ModuleWithDependenciesScope;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * Author: dmitrylomov
 */
@Singleton
@ServiceImpl
public class ModuleScopeProviderImpl implements ModuleScopeProvider {
  private final Module myModule;
  private final ConcurrentIntObjectMap<ModuleWithDependenciesScope> myScopeCache = IntMaps.newConcurrentIntObjectHashMap();
  private ModuleWithDependentsTestScope myModuleTestsWithDependentsScope;

  @Inject
  public ModuleScopeProviderImpl(@Nonnull Module module) {
    myModule = module;
  }

  @Nonnull
  private ModuleWithDependenciesScope getCachedScope(@ModuleWithDependenciesScopeImpl.ScopeConstant int options) {
    ModuleWithDependenciesScope scope = myScopeCache.get(options);
    if (scope == null) {
      scope = new ModuleWithDependenciesScopeImpl(myModule, options);
      myScopeCache.put(options, scope);
    }
    return scope;
  }

  @Override
  @Nonnull
  public ModuleWithDependenciesScope getModuleScope() {
    return getCachedScope(ModuleWithDependenciesScopeImpl.COMPILE | ModuleWithDependenciesScopeImpl.TESTS);
  }

  @Nonnull
  @Override
  public ModuleWithDependenciesScope getModuleScope(boolean includeTests) {
    return getCachedScope(ModuleWithDependenciesScopeImpl.COMPILE | (includeTests ? ModuleWithDependenciesScopeImpl.TESTS : 0));
  }

  @Override
  @Nonnull
  public ModuleWithDependenciesScope getModuleWithLibrariesScope() {
    return getCachedScope(ModuleWithDependenciesScopeImpl.COMPILE | ModuleWithDependenciesScopeImpl.TESTS | ModuleWithDependenciesScopeImpl.LIBRARIES);
  }

  @Override
  @Nonnull
  public ModuleWithDependenciesScope getModuleWithDependenciesScope() {
    return getCachedScope(ModuleWithDependenciesScopeImpl.COMPILE | ModuleWithDependenciesScopeImpl.TESTS | ModuleWithDependenciesScopeImpl.MODULES);
  }

  @Nonnull
  @Override
  public ModuleWithDependenciesScope getModuleContentScope() {
    return getCachedScope(ModuleWithDependenciesScopeImpl.CONTENT);
  }

  @Nonnull
  @Override
  public ModuleWithDependenciesScope getModuleContentWithDependenciesScope() {
    return getCachedScope(ModuleWithDependenciesScopeImpl.CONTENT | ModuleWithDependenciesScopeImpl.MODULES);
  }

  @Override
  @Nonnull
  public ModuleWithDependenciesScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests) {
    return getCachedScope(ModuleWithDependenciesScopeImpl.COMPILE |
                          ModuleWithDependenciesScopeImpl.MODULES |
                          ModuleWithDependenciesScopeImpl.LIBRARIES | (includeTests ? ModuleWithDependenciesScopeImpl.TESTS : 0));
  }

  @Override
  @Nonnull
  public ModuleAwareSearchScope getModuleWithDependentsScope() {
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
  public ModuleWithDependenciesScope getModuleRuntimeScope(boolean includeTests) {
    return getCachedScope(
            ModuleWithDependenciesScopeImpl.MODULES | ModuleWithDependenciesScopeImpl.LIBRARIES | (includeTests ? ModuleWithDependenciesScopeImpl.TESTS : 0));
  }

  public void clearCache() {
    myScopeCache.clear();
    myModuleTestsWithDependentsScope = null;
  }
}
