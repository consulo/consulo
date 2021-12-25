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

import consulo.localize.LocalizeValue;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public interface TextBox extends ValueComponent<String>, ValidableComponent<String>, FocusableComponent {
  @Nonnull
  static TextBox create() {
    return create(null);
  }

  @Nonnull
  static TextBox create(@Nullable String text) {
    return UIInternal.get()._Components_textBox(text);
  }

  default void setPlaceholder(@Nonnull LocalizeValue text) {
    // unwarranted action
  }

  @Nonnull
  default TextBox withPlaceholder(@Nonnull LocalizeValue text) {
    setPlaceholder(text);
    return this;
  }

  @Deprecated
  default void setPlaceholder(@Nullable String text) {
    setPlaceholder(text == null ? LocalizeValue.of() : LocalizeValue.of(text));
  }

  @Nonnull
  @Deprecated
  default TextBox withPlaceholder(@Nullable String text) {
    setPlaceholder(text == null ? LocalizeValue.of() : LocalizeValue.of(text));
    return this;
  }

  default void setVisibleLength(int columns) {
    // unwarranted action
  }

  @Nonnull
  default TextBox withVisibleLength(int columns) {
    setVisibleLength(columns);
    return this;
  }

  void selectAll();

  void setEditable(boolean editable);

  boolean isEditable();

  default TextBox withEditable(boolean editable) {
    setEditable(editable);
    return this;
  }
}
