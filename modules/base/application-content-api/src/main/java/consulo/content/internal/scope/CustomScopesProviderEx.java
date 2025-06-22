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
package consulo.content.internal.scope;

import consulo.content.scope.NamedScope;
import consulo.project.Project;

import java.util.Iterator;
import java.util.List;

/**
 * @author anna
 * @since 2012-03-14
 */
public abstract class CustomScopesProviderEx implements CustomScopesProvider {
  public boolean isVetoed(NamedScope scope, ScopePlace place) {
    return false;
  }

  public static void filterNoSettingsScopes(Project project, List<NamedScope> scopes) {
    for (Iterator<NamedScope> iterator = scopes.iterator(); iterator.hasNext(); ) {
      final NamedScope scope = iterator.next();
      for (CustomScopesProvider provider : CUSTOM_SCOPES_PROVIDER.getExtensionList(project)) {
        if (provider instanceof CustomScopesProviderEx && ((CustomScopesProviderEx)provider).isVetoed(scope, ScopePlace.SETTING)) {
          iterator.remove();
          break;
        }
      }
    }
  }

  public static enum ScopePlace {
    SETTING,
    ACTION
  }
}
