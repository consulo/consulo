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
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jun-16
 *
 * For advanced version of label {@link AdvancedLabel}
 */
public interface Label extends Component, Mnemonicable {
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
    return create(value, LabelOptions.builder().build());
  }

  @Nonnull
  static Label create(@Nonnull LocalizeValue value, @Nonnull LabelOptions options) {
    return UIInternal.get()._Components_label(value, options);
  }

  @Nonnull
  LocalizeValue getText();

  @RequiredUIAccess
  @Deprecated
  default void setText(@Nonnull String text) {
    setText(LocalizeValue.of(text));
  }

  @RequiredUIAccess
  void setText(@Nonnull LocalizeValue text);

  @Nullable
  String getTooltipText();

  void setToolTipText(@Nullable String text);

  void setImage(@Nullable Image icon);

  @Nullable
  Image getImage();
}
