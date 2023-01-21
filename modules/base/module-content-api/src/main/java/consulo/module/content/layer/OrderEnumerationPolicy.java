/*
 * Copyright 2013-2023 consulo.io
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
package consulo.module.content.layer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.module.Module;
import consulo.project.Project;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21/01/2023
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface OrderEnumerationPolicy {

  boolean isApplicable(@Nonnull Project project);

  boolean isApplicable(@Nonnull Module module);

  default boolean shouldAddRuntimeDependenciesToTestCompilationClasspath() {
    return false;
  }

  default boolean shouldIncludeTestsFromDependentModulesToTestClasspath() {
    return true;
  }

  default boolean shouldProcessDependenciesRecursively() {
    return true;
  }
}
