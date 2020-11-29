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

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11/29/2020
 */
public interface IntSlider extends ValueComponent<Integer>, FocusableComponent {
  @Nonnull
  static IntSlider create() {
    return create(0);
  }

  @Nonnull
  static IntSlider create(int value) {
    return UIInternal.get()._Components_intSlider(0, 100, value);
  }

  @Nonnull
  static IntSlider create(int min, int max, int value) {
    return UIInternal.get()._Components_intSlider(min, max, value);
  }

  void setRange(int min, int max);

  @Nonnull
  default IntSlider withRange(int min, int max) {
    setRange(min, max);
    return this;
  }
}
