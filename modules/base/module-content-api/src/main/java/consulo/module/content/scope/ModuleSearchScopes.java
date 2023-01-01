/*
 * Copyright 2013-2022 consulo.io
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

import consulo.module.Module;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11-Feb-22
 */
public class ModuleSearchScopes {
  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @param module the module to get the scope.
   * @return scope including sources and tests, excluding libraries and dependencies.
   */
  @Nonnull
  public static ModuleAwareSearchScope moduleScope(@Nonnull Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleScope();
  }

  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @param module       the module to get the scope.
   * @param includeTests include tests or not
   * @return scope including sources and tests(if set includeTests), excluding libraries and dependencies.
   */
  @Nonnull
  public static ModuleAwareSearchScope moduleScope(@Nonnull Module module, boolean includeTests) {
    return ModuleScopeProvider.getInstance(module).getModuleScope(includeTests);
  }

  @Nonnull
  public static ModuleAwareSearchScope moduleContentScope(@Nonnull Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleContentScope();
  }

  /**
   * Returns module scope including sources, tests, and libraries, excluding dependencies.
   *
   * @param module the module to get the scope.
   * @return scope including sources, tests, and libraries, excluding dependencies.
   */
  @Nonnull
  public static ModuleAwareSearchScope moduleWithLibrariesScope(@Nonnull Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleWithLibrariesScope();
  }

  /**
   * Returns module scope including sources, tests, and dependencies, excluding libraries.
   *
   * @param module the module to get the scope.
   * @return scope including sources, tests, and dependencies, excluding libraries.
   */
  @Nonnull
  public static ModuleAwareSearchScope moduleWithDependenciesScope(@Nonnull Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleWithDependenciesScope();
  }

  @Nonnull
  public static ModuleAwareSearchScope moduleRuntimeScope(@Nonnull Module module, final boolean includeTests) {
    return ModuleScopeProvider.getInstance(module).getModuleRuntimeScope(includeTests);
  }

  @Nonnull
  public static ModuleAwareSearchScope moduleWithDependenciesAndLibrariesScope(@Nonnull Module module) {
    return moduleWithDependenciesAndLibrariesScope(module, true);
  }

  @Nonnull
  public static ModuleAwareSearchScope moduleWithDependenciesAndLibrariesScope(@Nonnull Module module, boolean includeTests) {
    return ModuleScopeProvider.getInstance(module).getModuleWithDependenciesAndLibrariesScope(includeTests);
  }

  @Nonnull
  public static ModuleAwareSearchScope moduleWithDependentsScope(@Nonnull Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleWithDependentsScope();
  }

  @Nonnull
  public static ModuleAwareSearchScope moduleTestsWithDependentsScope(@Nonnull Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleTestsWithDependentsScope();
  }

  @Nonnull
  public static ModuleAwareSearchScope moduleContentWithDependenciesScope(@Nonnull Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleContentWithDependenciesScope();
  }

}
