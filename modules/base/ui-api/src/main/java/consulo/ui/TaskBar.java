/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui;

import consulo.ui.internal.UIInternal;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-10-06
 */
public interface TaskBar {
  static TaskBar get() {
    return UIInternal.get()._TaskBar_get();
  }

  interface ProgressScheme {
    @Nonnull
    ColorValue getOkColor();

    @Nonnull
    ColorValue getErrorColor();
  }

  default void requestFocus(@Nonnull Window window) {
  }

  default boolean setProgress(@Nonnull Window window, Object processId, ProgressScheme scheme, double value, boolean isOk) {
    return false;
  }

  default boolean hideProgress(@Nonnull Window window, Object processId) {
    return false;
  }

  default void setTextBadge(@Nonnull Window window, String text) {
  }

  default void setErrorBadge(@Nonnull Window window, String text) {
  }

  default void setOkBadge(@Nonnull Window window, boolean visible) {
  }

  default void requestAttention(@Nonnull Window window, boolean critical) {
  }
}
