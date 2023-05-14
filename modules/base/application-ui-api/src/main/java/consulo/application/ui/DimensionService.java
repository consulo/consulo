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
package consulo.application.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.util.ModificationTracker;
import consulo.project.Project;
import consulo.ui.Coordinate2D;
import consulo.ui.Size;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class represents map between strings and rectangles. It's intended to store
 * sizes of window, dialogs, etc.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DimensionService extends ModificationTracker {
  public static DimensionService getInstance() {
    return Application.get().getInstance(DimensionService.class);
  }

  /**
   * @param key a String key to perform a query for.
   * @return point stored under the specified {@code key}. The method returns
   * {@code null} if there is no stored value under the {@code key}.
   * @throws IllegalArgumentException if {@code key} is {@code null}.
   */
  @Nullable
  Size getSize(@Nonnull String key);

  @Nullable
  Size getSize(@Nonnull String key, Project project);

  /**
   * Store specified {@code size} under the {@code key}. If {@code size} is
   * {@code null} then the value stored under {@code key} will be removed.
   *
   * @param key  a String key to to save size for.
   * @param size a Size to save.
   * @throws IllegalArgumentException if {@code key} is {@code null}.
   */
  void setSize(@Nonnull String key, Size size);

  void setSize(@Nonnull String key, Size size, Project project);

  /**
   * @param key a String key to perform a query for.
   * @return point stored under the specified {@code key}. The method returns
   * {@code null} if there is no stored value under the {@code key}. If point
   * is outside of current screen bounds then the method returns {@code null}. It
   * properly works in multi-monitor configuration.
   * @throws IllegalArgumentException if {@code key} is {@code null}.
   */
  @Nullable
  Coordinate2D getLocation(String key);

  @Nullable
  Coordinate2D getLocation(@Nonnull String key, Project project);

  /**
   * Store specified {@code point} under the {@code key}. If {@code point} is
   * {@code null} then the value stored under {@code key} will be removed.
   *
   * @param key   a String key to store location for.
   * @param point location to save.
   * @throws IllegalArgumentException if {@code key} is {@code null}.
   */
  void setLocation(String key, Coordinate2D point);

  void setLocation(@Nonnull String key, Coordinate2D point, Project project);
}
