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
package consulo.ui.internal.image;

import com.intellij.ui.LayeredIcon;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.awt.internal.SwingIconWrapper;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
@SuppressWarnings("deprecation")
public class DesktopLayeredImageImpl extends LayeredIcon implements SwingIconWrapper, Image {
  @Nonnull
  public static Icon[] remap(Image[] icons) {
    return Arrays.stream(icons).map(TargetAWT::to).toArray(Icon[]::new);
  }

  public DesktopLayeredImageImpl(@Nonnull Image... images) {
    super(remap(images));
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
}
