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

import consulo.project.ui.view.tree.ViewSettings;
import consulo.util.dataholder.KeyWithDefaultValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2023-11-09
 */
public interface FavoritesViewSettings extends ViewSettings {
  @Override
  boolean isShowMembers();

  void setShowMembers(boolean showMembers);

  @Override
  boolean isStructureView();

  @Override
  boolean isShowModules();

  @Override
  boolean isFlattenPackages();

  boolean isAutoScrollFromSource();

  void setAutoScrollFromSource(boolean autoScrollFromSource);

  void setFlattenPackages(boolean flattenPackages);

  @Override
  boolean isAbbreviatePackageNames();

  @Override
  boolean isHideEmptyMiddlePackages();

  @Override
  boolean isShowLibraryContents();

  @Nonnull
  @Override
  <T> T getViewOption(@Nonnull KeyWithDefaultValue<T> option);

  boolean isAutoScrollToSource();

  void setAutoScrollToSource(boolean autoScrollToSource);

  void setHideEmptyMiddlePackages(boolean hide);

  void setAbbreviateQualifiedPackages(boolean abbreviate);
}
