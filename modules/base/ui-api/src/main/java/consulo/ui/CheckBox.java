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

import consulo.annotation.ApiType;
import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
@ApiType
public interface CheckBox extends ValueComponent<Boolean>, Mnemonicable {
  @Nonnull
  @RequiredUIAccess
  @Deprecated(forRemoval = true)
  @DeprecationInfo("Please don't use not localize text")
  static CheckBox create(@Nonnull String label) {
    return create(LocalizeValue.of(label));
  }

  @Nonnull
  @RequiredUIAccess
  @Deprecated(forRemoval = true)
  @DeprecationInfo("Please don't use not localize text")
  static CheckBox create(@Nonnull String label, boolean selected) {
    return create(LocalizeValue.of(label), selected);
  }

  @Nonnull
  @RequiredUIAccess
  static CheckBox create(@Nonnull LocalizeValue label) {
    return create(label, false);
  }

  @Nonnull
  @RequiredUIAccess
  static CheckBox create(@Nonnull LocalizeValue label, boolean selected) {
    CheckBox box = UIInternal.get()._Components_checkBox();
    box.setLabelText(label);
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
  void setValue(@Nonnull Boolean value, boolean fireListeners);

  @Nonnull
  LocalizeValue getLabelText();

  @RequiredUIAccess
  void setLabelText(@Nonnull LocalizeValue labelText);
}
