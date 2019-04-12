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

import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface CheckBox extends ValueComponent<Boolean>, Mnemonicable{
  @Nonnull
  @RequiredUIAccess
  static CheckBox create(@Nonnull String text) {
    return create(text, false);
  }

  @Nonnull
  @RequiredUIAccess
  static CheckBox create(@Nonnull String text, boolean selected) {
    return create(LocalizeKey.of(text), selected);
  }

  @Nonnull
  @RequiredUIAccess
  static CheckBox create(@Nonnull LocalizeValue textValue) {
    return create(textValue, false);
  }

  @Nonnull
  @RequiredUIAccess
  static CheckBox create(@Nonnull LocalizeValue textValue, boolean selected) {
    CheckBox box = UIInternal.get()._Components_checkBox();
    box.setText(textValue);
    box.setValue(selected);
    return box;
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
  void setValue(@Nonnull Boolean value, boolean fireEvents);

  @Nonnull
  String getText();

  @RequiredUIAccess
  default void setText(@Nonnull String text) {
    setText(LocalizeKey.of(text));
  }

  @RequiredUIAccess
  void setText(@Nonnull LocalizeValue localizeValue);
}
