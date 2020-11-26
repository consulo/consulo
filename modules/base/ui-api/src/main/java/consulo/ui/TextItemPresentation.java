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
import consulo.ui.font.Font;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public interface TextItemPresentation {
  @Nonnull
  default TextItemPresentation withIcon(@Nullable Image image) {
    // unwarranted action
    return this;
  }

  @Nonnull
  default TextItemPresentation withAntialiasingType(@Nonnull AntialiasingType type) {
    // unwarranted action
    return this;
  }

  @Nonnull
  default TextItemPresentation withFont(@Nonnull Font font) {
    // unwarranted action
    return this;
  }

  void clearText();

  default void append(@Nonnull String text) {
    append(text, TextAttribute.REGULAR);
  }

  default void append(@Nonnull String text, @Nonnull TextAttribute textAttribute) {
    append(LocalizeValue.of(text), textAttribute);
  }

  default void append(@Nonnull LocalizeValue text) {
    append(text, TextAttribute.REGULAR);
  }

  void append(@Nonnull LocalizeValue text, @Nonnull TextAttribute textAttribute);
}
