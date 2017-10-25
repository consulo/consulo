/*
 * Copyright 2013-2017 consulo.io
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

import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 01-Oct-17
 */
public interface AlertBuilder {
  @NotNull
  static AlertBuilder createInfo() {
    return UIInternal.get()._Alerts_builder();
  }

  @NotNull
  static AlertBuilder createError() {
    return UIInternal.get()._Alerts_builder();
  }

  @NotNull
  static AlertBuilder createWarning() {
    return UIInternal.get()._Alerts_builder();
  }

  @NotNull
  default AlertBuilder ok() {
    return ok(() -> {
    });
  }

  @NotNull
  AlertBuilder ok(@NotNull Runnable runnable);

  @NotNull
  default AlertBuilder button(@NotNull String text) {
    return button(text, () -> {
    });
  }

  @NotNull
  AlertBuilder button(@NotNull String text, @NotNull Runnable runnable);

  /**
   * Mark last added button as default (enter will hit it)
   */
  @NotNull
  AlertBuilder markDefault();

  @NotNull
  AlertBuilder title(@NotNull String text);

  @NotNull
  AlertBuilder text(@NotNull String text);

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  void show();
}
