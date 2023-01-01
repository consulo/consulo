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
import consulo.module.UnloadedModuleDescription;
import consulo.project.content.scope.ProjectAwareSearchScope;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

/**
 * @author VISTALL
 * @since 09-Feb-22
 */
public interface ModuleAwareSearchScope extends ProjectAwareSearchScope {
  boolean isSearchInLibraries();

  public abstract boolean isSearchInModuleContent(@Nonnull Module aModule);

  default boolean isSearchInModuleContent(@Nonnull Module aModule, boolean testSources) {
    return isSearchInModuleContent(aModule);
  }

  default boolean isForceSearchingInLibrarySources() {
    return false;
  }

  @Nonnull
  default Collection<UnloadedModuleDescription> getUnloadedModulesBelongingToScope() {
    return Set.of();
  }

  default boolean isSearchOutsideRootModel() {
    return false;
  }
}
