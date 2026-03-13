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
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2020-11-26
 *
 * Similar component to {@link CheckBox}, but has different cases for usage
 *
 * Read for example {@linkplain https://docs.microsoft.com/ru-ru/windows/uwp/design/controls-and-patterns/toggles}
 */
public interface ToggleSwitch extends ValueComponent<Boolean> {
  static ToggleSwitch create() {
    return create(false);
  }

  static ToggleSwitch create(boolean enabled) {
    return UIInternal.get()._Components_toggleSwitch(enabled);
  }

  @Override
  Boolean getValue();

  @Override
  @RequiredUIAccess
  default void setValue(@Nullable Boolean value) {
    setValue(value, true);
  }

  @RequiredUIAccess
  void setValue(@Nullable Boolean value, boolean fireListeners);
}
