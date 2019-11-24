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
import consulo.ui.image.Image;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.HorizontalAlignment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public interface Label extends Component {
  @Nonnull
  static Label create() {
    return create(null);
  }

  @Nonnull
  static Label create(@Nullable String text) {
    Label label = UIInternal.get()._Components_label(text);
    label.setHorizontalAlignment(HorizontalAlignment.LEFT);
    return label;
  }

  @Nonnull
  String getText();

  @RequiredUIAccess
  @Nonnull
  Label setText(@Nonnull String text);

  @Nullable
  String getTooltipText();

  @Nonnull
  Label setToolTipText(@Nullable String text);

  @Nonnull
  Label setHorizontalAlignment(@Nonnull HorizontalAlignment horizontalAlignment);

  @Nonnull
  HorizontalAlignment getHorizontalAlignment();

  default Label setForeground(@Nonnull ColorValue colorValue) {
    return setForeground(() -> colorValue);
  }

  @Nonnull
  Label setForeground(@Nonnull Supplier<ColorValue> colorValueSupplier);

  @Nonnull
  Label setImage(@Nullable Image icon);

  @Nullable
  Image getImage();
}
