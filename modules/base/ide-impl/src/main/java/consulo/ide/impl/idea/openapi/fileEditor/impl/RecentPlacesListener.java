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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import javax.annotation.Nonnull;

/**
 * {@link RecentPlacesListener} listens recently viewed or changed place adding and removing events.
 */
@TopicAPI(ComponentScope.PROJECT)
public interface RecentPlacesListener {
  /**
   * Fires on a new place info adding into {@link #myChangePlaces} or {@link #myBackPlaces} infos list
   *
   * @param changePlace new place info
   * @param isChanged   true if place info was added into the changed infos list {@link #myChangePlaces};
   *                    false if place info was added into the back infos list {@link #myBackPlaces}
   */
  void recentPlaceAdded(@Nonnull IdeDocumentHistoryImpl.PlaceInfo changePlace, boolean isChanged);

  /**
   * Fires on a place info removing from the {@link #myChangePlaces} or the {@link #myBackPlaces} infos list
   *
   * @param changePlace place info that was removed
   * @param isChanged   true if place info was removed from the changed infos list {@link #myChangePlaces};
   *                    false if place info was removed from the back infos list {@link #myBackPlaces}
   */
  void recentPlaceRemoved(@Nonnull IdeDocumentHistoryImpl.PlaceInfo changePlace, boolean isChanged);
}
