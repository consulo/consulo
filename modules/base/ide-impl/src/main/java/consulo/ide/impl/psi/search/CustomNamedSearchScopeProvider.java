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
package consulo.ide.impl.psi.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.SearchScope;
import consulo.content.scope.SearchScopeProvider;
import consulo.project.Project;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 23-Jun-22
 */
@ExtensionImpl(order = "last")
public class CustomNamedSearchScopeProvider implements SearchScopeProvider {
  @Override
  public String getDisplayName() {
    return "Other";
  }

  @Nonnull
  @Override
  public List<SearchScope> getSearchScopes(@Nonnull Project project) {
    List<SearchScope> result = new ArrayList<>();
    NamedScopesHolder[] holders = NamedScopesHolder.getAllNamedScopeHolders(project);
    for (NamedScopesHolder holder : holders) {
      NamedScope[] scopes = holder.getEditableScopes();  // predefined scopes already included
      for (NamedScope scope : scopes) {
        result.add(DefaultSearchScopeProviders.wrapNamedScope(project, scope, true));
      }
    }
    return result;
  }
}
