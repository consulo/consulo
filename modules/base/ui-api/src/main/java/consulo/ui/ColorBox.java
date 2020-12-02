/*
 * Copyright 2013-2018 consulo.io
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
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 6/9/18
 */
public interface ColorBox extends ValueComponent<ColorValue> {
  @Nonnull
  static ColorBox create() {
    return create(null);
  }

  static ColorBox create(@Nullable ColorValue colorValue) {
    return UIInternal.get()._Components_colorBox(colorValue);
  }

  void setEditable(boolean editable);

  boolean isEditable();
}
