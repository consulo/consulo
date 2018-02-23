/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.internal.icon;

import com.intellij.ui.LayeredIcon;
import consulo.ui.image.FoldedImage;
import consulo.ui.internal.SwingIconWrapper;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
public class DesktopFoldedImageImpl extends LayeredIcon implements SwingIconWrapper, FoldedImage {
  @Nonnull
  public static Icon[] remap(consulo.ui.image.Image[] icons) {
    return Arrays.stream(icons).map(DesktopFoldedImageImpl::to).toArray(Icon[]::new);
  }

  public static Icon to(consulo.ui.image.Image icon) {
    if(icon instanceof Icon) {
      return (Icon)icon;
    }
    return ((SwingIconWrapper)icon).toSwingIcon();
  }

  private final consulo.ui.image.Image[] myImages;

  public DesktopFoldedImageImpl(@Nonnull consulo.ui.image.Image... images) {
    super(remap(images));
    myImages = images;
  }

  @Nonnull
  @Override
  public Icon toSwingIcon() {
    return this;
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  @Nonnull
  @Override
  public consulo.ui.image.Image[] getImages() {
    return myImages;
  }
}
