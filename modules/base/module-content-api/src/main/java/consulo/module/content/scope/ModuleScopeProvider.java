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
package consulo.module.content.scope;


import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.module.Module;

import javax.annotation.Nonnull;

/**
 * @author dmitrylomov
 */
@ServiceAPI(ComponentScope.MODULE)
public interface ModuleScopeProvider {
  @Nonnull
  static ModuleScopeProvider getInstance(@Nonnull Module module) {
    return module.getInstance(ModuleScopeProvider.class);
  }

  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @return scope including sources and tests, excluding libraries and dependencies.
   */
  @Nonnull
  ModuleWithDependenciesScope getModuleScope();

  @Nonnull
  ModuleWithDependenciesScope getModuleScope(boolean includeTests);

  /**
   * Returns module scope including sources, tests, and libraries, excluding dependencies.
   *
   * @return scope including sources, tests, and libraries, excluding dependencies.
   */
  @Nonnull
  ModuleWithDependenciesScope getModuleWithLibrariesScope();

  /**
   * Returns module scope including sources, tests, and dependencies, excluding libraries.
   *
   * @return scope including sources, tests, and dependencies, excluding libraries.
   */
  @Nonnull
  ModuleWithDependenciesScope getModuleWithDependenciesScope();

  @Nonnull
  ModuleWithDependenciesScope getModuleContentScope();

  @Nonnull
  ModuleWithDependenciesScope getModuleContentWithDependenciesScope();

  @Nonnull
  ModuleWithDependenciesScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests);

  @Nonnull
  ModuleAwareSearchScope getModuleWithDependentsScope();

  @Nonnull
  ModuleAwareSearchScope getModuleTestsWithDependentsScope();

  @Nonnull
  ModuleWithDependenciesScope getModuleRuntimeScope(boolean includeTests);
}
