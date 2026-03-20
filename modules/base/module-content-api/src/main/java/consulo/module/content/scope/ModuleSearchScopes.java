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
  
  public static ModuleAwareSearchScope moduleScope(Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleScope();
  }

  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @param module       the module to get the scope.
   * @param includeTests include tests or not
   * @return scope including sources and tests(if set includeTests), excluding libraries and dependencies.
   */
  
  public static ModuleAwareSearchScope moduleScope(Module module, boolean includeTests) {
    return ModuleScopeProvider.getInstance(module).getModuleScope(includeTests);
  }

  
  public static ModuleAwareSearchScope moduleContentScope(Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleContentScope();
  }

  /**
   * Returns module scope including sources, tests, and libraries, excluding dependencies.
   *
   * @param module the module to get the scope.
   * @return scope including sources, tests, and libraries, excluding dependencies.
   */
  
  public static ModuleAwareSearchScope moduleWithLibrariesScope(Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleWithLibrariesScope();
  }

  /**
   * Returns module scope including sources, tests, and dependencies, excluding libraries.
   *
   * @param module the module to get the scope.
   * @return scope including sources, tests, and dependencies, excluding libraries.
   */
  
  public static ModuleAwareSearchScope moduleWithDependenciesScope(Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleWithDependenciesScope();
  }

  
  public static ModuleAwareSearchScope moduleRuntimeScope(Module module, boolean includeTests) {
    return ModuleScopeProvider.getInstance(module).getModuleRuntimeScope(includeTests);
  }

  
  public static ModuleAwareSearchScope moduleWithDependenciesAndLibrariesScope(Module module) {
    return moduleWithDependenciesAndLibrariesScope(module, true);
  }

  
  public static ModuleAwareSearchScope moduleWithDependenciesAndLibrariesScope(Module module, boolean includeTests) {
    return ModuleScopeProvider.getInstance(module).getModuleWithDependenciesAndLibrariesScope(includeTests);
  }

  
  public static ModuleAwareSearchScope moduleWithDependentsScope(Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleWithDependentsScope();
  }

  
  public static ModuleAwareSearchScope moduleTestsWithDependentsScope(Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleTestsWithDependentsScope();
  }

  
  public static ModuleAwareSearchScope moduleContentWithDependenciesScope(Module module) {
    return ModuleScopeProvider.getInstance(module).getModuleContentWithDependenciesScope();
  }

}
