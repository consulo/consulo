/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 06-Apr-2007
 */
package consulo.ide.impl.psi.search.scope.packageSet;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.content.scope.NamedScope;

import javax.annotation.Nonnull;

import java.util.List;

@Extension(ComponentScope.PROJECT)
public interface CustomScopesProvider {
  ExtensionPointName<CustomScopesProvider> CUSTOM_SCOPES_PROVIDER = ExtensionPointName.create(CustomScopesProvider.class);

  @Nonnull
  List<NamedScope> getCustomScopes();

  @Nonnull
  default List<NamedScope> getFilteredScopes() {
    List<CustomScopesFilter> filters = CustomScopesFilter.EP_NAME.getExtensionList();
    return ContainerUtil.filter(getCustomScopes(), scope -> {
      for (CustomScopesFilter filter : filters) {
        if (filter.excludeScope(scope)) return false;
      }
      return true;
    });
  }
}