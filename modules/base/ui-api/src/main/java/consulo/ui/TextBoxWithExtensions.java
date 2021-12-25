/*
 * Copyright 2013-2019 consulo.io
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
import consulo.ui.event.ClickListener;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-10-31
 */
public interface TextBoxWithExtensions extends TextBox {
  public final class Extension {
    private final boolean myLeft;
    private final Image myIcon;
    private final Image myHoveredIcon;

    private ClickListener myClickListener;

    public Extension(boolean left, @Nonnull Image icon, @Nullable Image hoveredIcon) {
      this(left, icon, hoveredIcon, null);
    }

    public Extension(boolean left, @Nonnull Image icon, @Nullable Image hoveredIcon, @RequiredUIAccess @Nullable ClickListener clickListener) {
      myLeft = left;
      myIcon = icon;
      myHoveredIcon = ObjectUtil.notNull(hoveredIcon, icon);
      myClickListener = clickListener;
    }

    @Nullable
    public ClickListener getClickListener() {
      return myClickListener;
    }

    public boolean isLeft() {
      return myLeft;
    }

    @Nonnull
    public Image getIcon() {
      return myIcon;
    }

    @Nonnull
    public Image getHoveredIcon() {
      return myHoveredIcon;
    }
  }

  @Nonnull
  static TextBoxWithExtensions create() {
    return create(null);
  }

  @Nonnull
  static TextBoxWithExtensions create(@Nullable String text) {
    return UIInternal.get()._Components_textBoxWithExtensions(text);
  }

  @Nonnull
  TextBoxWithExtensions setExtensions(@Nonnull Extension... extensions);

  @Nonnull
  @Deprecated
  default TextBoxWithExtensions addExtension(@Nonnull Extension extension) {
    return addLastExtension(extension);
  }

  @Nonnull
  TextBoxWithExtensions addLastExtension(@Nonnull Extension extension);

  @Nonnull
  TextBoxWithExtensions addFirstExtension(@Nonnull Extension extension);
}
