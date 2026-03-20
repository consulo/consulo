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

import consulo.module.content.internal.ModuleScopeProviderInternal;

/**
 * @author dmitrylomov
 */
@ServiceAPI(ComponentScope.MODULE)
public sealed interface ModuleScopeProvider permits ModuleScopeProviderInternal {
  
  static ModuleScopeProvider getInstance(Module module) {
    return module.getInstance(ModuleScopeProvider.class);
  }

  /**
   * Returns module scope including sources and tests, excluding libraries and dependencies.
   *
   * @return scope including sources and tests, excluding libraries and dependencies.
   */
  ModuleWithDependenciesScope getModuleScope();

  
  ModuleWithDependenciesScope getModuleScope(boolean includeTests);

  /**
   * Returns module scope including sources, tests, and libraries, excluding dependencies.
   *
   * @return scope including sources, tests, and libraries, excluding dependencies.
   */
  ModuleWithDependenciesScope getModuleWithLibrariesScope();

  /**
   * Returns module scope including sources, tests, and dependencies, excluding libraries.
   *
   * @return scope including sources, tests, and dependencies, excluding libraries.
   */
  ModuleWithDependenciesScope getModuleWithDependenciesScope();

  
  ModuleWithDependenciesScope getModuleContentScope();

  
  ModuleWithDependenciesScope getModuleContentWithDependenciesScope();

  
  ModuleWithDependenciesScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests);

  
  ModuleAwareSearchScope getModuleWithDependentsScope();

  
  ModuleAwareSearchScope getModuleTestsWithDependentsScope();

  
  ModuleWithDependenciesScope getModuleRuntimeScope(boolean includeTests);
}
