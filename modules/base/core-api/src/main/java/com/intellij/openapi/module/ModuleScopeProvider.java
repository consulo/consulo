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
package com.intellij.openapi.module;


import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.search.GlobalSearchScope;

import javax.annotation.Nonnull;

/**
 * Author: dmitrylomov
 */
public interface ModuleScopeProvider {
  @Nonnull
  static ModuleScopeProvider getInstance(@Nonnull Module module) {
    return ServiceManager.getService(module, ModuleScopeProvider.class);
  }

  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @return scope including sources and tests, excluding libraries and dependencies.
   */
  @Nonnull
  GlobalSearchScope getModuleScope();

  @Nonnull
  GlobalSearchScope getModuleScope(boolean includeTests);

  /**
   * Returns module scope including sources, tests, and libraries, excluding dependencies.
   *
   * @return scope including sources, tests, and libraries, excluding dependencies.
   */
  @Nonnull
  GlobalSearchScope getModuleWithLibrariesScope();

  /**
   * Returns module scope including sources, tests, and dependencies, excluding libraries.
   *
   * @return scope including sources, tests, and dependencies, excluding libraries.
   */
  @Nonnull
  GlobalSearchScope getModuleWithDependenciesScope();

  @Nonnull
  GlobalSearchScope getModuleContentScope();

  @Nonnull
  GlobalSearchScope getModuleContentWithDependenciesScope();

  @Nonnull
  GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests);

  @Nonnull
  GlobalSearchScope getModuleWithDependentsScope();

  @Nonnull
  GlobalSearchScope getModuleTestsWithDependentsScope();

  @Nonnull
  GlobalSearchScope getModuleRuntimeScope(boolean includeTests);
}
