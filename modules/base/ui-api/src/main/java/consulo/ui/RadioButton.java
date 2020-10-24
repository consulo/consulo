/*
 * Copyright 2013-2016 consulo.io
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

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public interface RadioButton extends ValueComponent<Boolean> {
  @Nonnull
  static RadioButton create(@Nonnull String text) {
    return create(text, false);
  }

  @Nonnull
  static RadioButton create(@Nonnull String text, boolean selected) {
    return UIInternal.get()._Components_radioButton(text, selected);
  }

  @Nonnull
  @Override
  Boolean getValue();

  @Override
  @RequiredUIAccess
  default void setValue(@Nonnull Boolean value) {
    setValue(value, true);
  }

  @Override
  @RequiredUIAccess
  void setValue(@Nonnull Boolean value, boolean fireEvents);

  @Nonnull
  String getText();

  @RequiredUIAccess
  void setText(@Nonnull String text);

  @Nonnull
  default RadioButton toGroup(ValueGroup<Boolean> group) {
    group.add(this);
    return this;
  }
}
