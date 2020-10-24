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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import consulo.ui.color.ColorValue;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public interface Label extends Component {
  @Nonnull
  static Label create() {
    return create(LocalizeValue.empty());
  }

  @Nonnull
  @Deprecated
  static Label create(@Nullable String text) {
    return create(LocalizeValue.of(StringUtil.notNullize(text)));
  }

  @Nonnull
  static Label create(@Nonnull LocalizeValue value) {
    Label label = UIInternal.get()._Components_label(value);
    label.setHorizontalAlignment(HorizontalAlignment.LEFT);
    return label;
  }

  @Nonnull
  LocalizeValue getText();

  @RequiredUIAccess
  @Nonnull
  @Deprecated
  default Label setText(@Nonnull String text) {
    return setText(LocalizeValue.of(text));
  }

  @RequiredUIAccess
  @Nonnull
  Label setText(@Nonnull LocalizeValue text);

  @Nullable
  String getTooltipText();

  @Nonnull
  Label setToolTipText(@Nullable String text);

  @Nonnull
  Label setHorizontalAlignment(@Nonnull HorizontalAlignment horizontalAlignment);

  @Nonnull
  HorizontalAlignment getHorizontalAlignment();

  @Nonnull
  Label setForeground(@Nonnull ColorValue colorValue);

  @Nonnull
  Label setImage(@Nullable Image icon);

  @Nullable
  Image getImage();
}
