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
import consulo.annotation.component.Service;
import consulo.module.Module;

import javax.annotation.Nonnull;

/**
 * @author dmitrylomov
 */
@Service(ComponentScope.MODULE)
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
  ModuleAwareSearchScope getModuleScope();

  @Nonnull
  ModuleAwareSearchScope getModuleScope(boolean includeTests);

  /**
   * Returns module scope including sources, tests, and libraries, excluding dependencies.
   *
   * @return scope including sources, tests, and libraries, excluding dependencies.
   */
  @Nonnull
  ModuleAwareSearchScope getModuleWithLibrariesScope();

  /**
   * Returns module scope including sources, tests, and dependencies, excluding libraries.
   *
   * @return scope including sources, tests, and dependencies, excluding libraries.
   */
  @Nonnull
  ModuleAwareSearchScope getModuleWithDependenciesScope();

  @Nonnull
  ModuleAwareSearchScope getModuleContentScope();

  @Nonnull
  ModuleAwareSearchScope getModuleContentWithDependenciesScope();

  @Nonnull
  ModuleAwareSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests);

  @Nonnull
  ModuleAwareSearchScope getModuleWithDependentsScope();

  @Nonnull
  ModuleAwareSearchScope getModuleTestsWithDependentsScope();

  @Nonnull
  ModuleAwareSearchScope getModuleRuntimeScope(boolean includeTests);
}
