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

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 01-Oct-17
 */
public interface AlertBuilder {
  @Nonnull
  static AlertBuilder createInfo() {
    return UIInternal.get()._Alerts_builder();
  }

  @Nonnull
  static AlertBuilder createError() {
    return UIInternal.get()._Alerts_builder();
  }

  @Nonnull
  static AlertBuilder createWarning() {
    return UIInternal.get()._Alerts_builder();
  }

  @Nonnull
  default AlertBuilder ok() {
    return ok(() -> {
    });
  }

  @Nonnull
  AlertBuilder ok(@Nonnull Runnable runnable);

  @Nonnull
  default AlertBuilder button(@Nonnull String text) {
    return button(text, () -> {
    });
  }

  @Nonnull
  AlertBuilder button(@Nonnull String text, @Nonnull Runnable runnable);

  /**
   * Mark last added button as default (enter will hit it)
   */
  @Nonnull
  AlertBuilder markDefault();

  @Nonnull
  AlertBuilder title(@Nonnull String text);

  @Nonnull
  AlertBuilder text(@Nonnull String text);

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  void show();
}
