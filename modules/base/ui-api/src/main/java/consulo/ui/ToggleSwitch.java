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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-26
 *
 * Similar component to {@link CheckBox}, but has different cases for usage
 *
 * Read for example {@linkplain https://docs.microsoft.com/ru-ru/windows/uwp/design/controls-and-patterns/toggles}
 */
public interface ToggleSwitch extends ValueComponent<Boolean> {
  @Nonnull
  static ToggleSwitch create() {
    return create(false);
  }

  @Nonnull
  static ToggleSwitch create(boolean enabled) {
    return UIInternal.get()._Components_toggleSwitch(enabled);
  }

  @Nonnull
  @Override
  Boolean getValue();

  @Override
  @RequiredUIAccess
  default void setValue(@Nonnull Boolean value) {
    setValue(value, true);
  }

  @RequiredUIAccess
  void setValue(@Nonnull Boolean value, boolean fireListeners);
}
