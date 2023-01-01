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
package consulo.execution.configuration;

import consulo.content.scope.SearchScope;
import consulo.module.Module;
import consulo.module.content.scope.ModuleSearchScopes;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vojtech Krasa
 */
public class ExecutionSearchScopeProvider {
  @Nonnull
  public static SearchScope createSearchScope(@Nonnull Project project, @Nullable RunProfile runProfile) {
    Module[] modules = null;
    if (runProfile instanceof SearchScopeProvidingRunProfile) {
      modules = ((SearchScopeProvidingRunProfile)runProfile).getModules();
    }
    if (modules == null || modules.length == 0) {
      return ProjectScopes.getAllScope(project);
    }
    else {
      SearchScope scope = ModuleSearchScopes.moduleRuntimeScope(modules[0], true);
      for (int idx = 1; idx < modules.length; idx++) {
        Module module = modules[idx];
        scope = scope.union(ModuleSearchScopes.moduleRuntimeScope(module, true));
      }
      return scope;
    }
  }
}
