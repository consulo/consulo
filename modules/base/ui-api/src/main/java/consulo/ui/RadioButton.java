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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2016-06-14
 */
public interface RadioButton extends ValueComponent<Boolean>, HasFocus {
  @Deprecated
  @DeprecationInfo("Use with LocalizeValue parameter")
  static RadioButton create(String text) {
    return create(text, false);
  }

  @Deprecated
  @DeprecationInfo("Use with LocalizeValue parameter")
  static RadioButton create(String text, boolean selected) {
    return create(LocalizeValue.of(text), selected);
  }

  static RadioButton create(LocalizeValue textValue) {
    return create(textValue, false);
  }

  static RadioButton create(LocalizeValue textValue, boolean selected) {
    return UIInternal.get()._Components_radioButton(textValue, selected);
  }

  @Override
  Boolean getValue();

  @Override
  @RequiredUIAccess
  default void setValue(@Nullable Boolean value) {
    setValue(value, true);
  }

  @Override
  @RequiredUIAccess
  void setValue(@Nullable Boolean value, boolean fireListeners);
  LocalizeValue getLabelText();

  @RequiredUIAccess
  void setLabelText(LocalizeValue text);
  default RadioButton toGroup(ValueGroup<Boolean> group) {
    group.add(this);
    return this;
  }
}
