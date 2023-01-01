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
package consulo.ide.impl.idea.execution.filters;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.SearchScope;
import consulo.execution.ui.console.ConsoleFilterProviderEx;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.UrlFilter;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;

import javax.annotation.Nonnull;

@ExtensionImpl
public class UrlFilterProvider implements ConsoleFilterProviderEx {
  @Override
  @Nonnull
  public Filter[] getDefaultFilters(@Nonnull Project project, @Nonnull SearchScope scope) {
    return new Filter[]{new UrlFilter(project)};
  }

  @Nonnull
  @Override
  public Filter[] getDefaultFilters(@Nonnull Project project) {
    return getDefaultFilters(project, GlobalSearchScope.allScope(project));
  }
}
