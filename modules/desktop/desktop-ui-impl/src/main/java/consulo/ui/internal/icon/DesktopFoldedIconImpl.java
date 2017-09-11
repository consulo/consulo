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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
public class DesktopFoldedIconImpl extends LayeredIcon implements SwingIconWrapper, FoldedImage {
  @NotNull
  public static Icon[] remap(consulo.ui.image.FoldedImage[] icons) {
    return Arrays.stream(icons).map(DesktopFoldedIconImpl::to).toArray(Icon[]::new);
  }

  public static Icon to(consulo.ui.image.FoldedImage icon) {
    return ((SwingIconWrapper)icon).toSwingIcon();
  }

  private final consulo.ui.image.FoldedImage[] myImages;

  public DesktopFoldedIconImpl(@NotNull consulo.ui.image.FoldedImage... images) {
    super(remap(images));
    myImages = images;
  }

  @NotNull
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

  @NotNull
  @Override
  public consulo.ui.image.FoldedImage[] getImages() {
    return myImages;
  }
}
