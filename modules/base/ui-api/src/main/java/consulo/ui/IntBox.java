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

import consulo.annotation.DeprecationInfo;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-04-19
 */
public interface IntBox extends ValueComponent<Integer>, ValidableComponent<Integer>, FocusableComponent {
  @Nonnull
  static IntBox create() {
    return create(0);
  }

  @Nonnull
  static IntBox create(int value) {
    return UIInternal.get()._Components_intBox(value);
  }

  void setPlaceholder(@Nullable String text);

  @Nonnull
  default IntBox withPlaceholder(@Nullable String text) {
    setPlaceholder(text);
    return this;
  }

  void setRange(int min, int max);

  @Nonnull
  default IntBox withRange(int min, int max) {
    setRange(min, max);
    return this;
  }
}
