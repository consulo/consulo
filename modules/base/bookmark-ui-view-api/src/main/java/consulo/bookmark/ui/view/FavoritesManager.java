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
package consulo.bookmark.ui.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.bookmark.ui.view.event.FavoritesListener;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.view.internal.AbstractUrl;
import consulo.util.lang.Pair;
import consulo.util.lang.TreeItem;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 2023-11-09
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface FavoritesManager {
  static FavoritesManager getInstance(Project project) {
    return project.getInstance(FavoritesManager.class);
  }

  @Deprecated
  void addFavoritesListener(FavoritesListener listener);

  void addFavoritesListener(final FavoritesListener listener, @Nonnull Disposable parent);

  @Deprecated
  void removeFavoritesListener(FavoritesListener listener);

  FavoritesViewSettings getViewSettings();

  @Nonnull
  List<TreeItem<Pair<AbstractUrl, String>>> getFavoritesListRootUrls(@Nonnull String name);

  void fireListeners(@Nonnull final String listName);
}
