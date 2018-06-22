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
package consulo.ui.internal.image;

import com.intellij.ui.RowIcon;
import consulo.ui.image.Image;
import consulo.awt.internal.SwingIconWrapper;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2018-05-07
 */
public class DesktopAppendImageImpl extends RowIcon implements SwingIconWrapper, Image {
  public DesktopAppendImageImpl(int iconCount) {
    super(iconCount, Alignment.CENTER);        
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }

  @Nonnull
  @Override
  public Icon toSwingIcon() {
    return this;
  }
}
