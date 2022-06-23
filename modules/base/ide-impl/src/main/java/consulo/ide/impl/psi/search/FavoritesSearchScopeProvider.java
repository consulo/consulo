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
import consulo.application.ReadAction;
import consulo.content.scope.SearchScope;
import consulo.content.scope.SearchScopeProvider;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManager;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractUrl;
import consulo.ide.impl.idea.util.TreeItem;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ExtensionImpl(id = "favorites", order = "first")
public class FavoritesSearchScopeProvider implements SearchScopeProvider {
  @Override
  public String getDisplayName() {
    return "Favorites";
  }

  @Nonnull
  @Override
  public List<SearchScope> getSearchScopes(@Nonnull Project project) {
    FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
    if (favoritesManager == null) return Collections.emptyList();
    List<SearchScope> result = new ArrayList<>();
    for (String favorite : favoritesManager.getAvailableFavoritesListNames()) {
      Collection<TreeItem<Pair<AbstractUrl, String>>> rootUrls = favoritesManager.getFavoritesListRootUrls(favorite);
      if (rootUrls.isEmpty()) continue;  // ignore unused root
      result.add(new GlobalSearchScope(project) {
        @Nonnull
        @Override
        public String getDisplayName() {
          return "Favorite \'" + favorite + "\'";
        }

        @Override
        public boolean contains(@Nonnull VirtualFile file) {
          return ReadAction.compute(() -> favoritesManager.contains(favorite, file));
        }

        @Override
        public boolean isSearchInModuleContent(@Nonnull Module aModule) {
          return true;
        }

        @Override
        public boolean isSearchInLibraries() {
          return true;
        }
      });
    }
    return result;
  }
}
