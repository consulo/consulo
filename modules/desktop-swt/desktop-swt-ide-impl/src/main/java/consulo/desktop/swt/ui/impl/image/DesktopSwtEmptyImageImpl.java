/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl.image;

import consulo.ui.image.EmptyImage;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtEmptyImageImpl implements EmptyImage, DesktopSwtImage {
  private final int myWidth;
  private final int myHeight;

  public DesktopSwtEmptyImageImpl(int width, int height) {
    myWidth = width;
    myHeight = height;
  }

  @Override
  public int getHeight() {
    return myHeight;
  }

  @Override
  public int getWidth() {
    return myWidth;
  }

  @Nonnull
  @Override
  public org.eclipse.swt.graphics.Image toSWTImage() {
    return new org.eclipse.swt.graphics.Image(null, getWidth(), getHeight());
  }
}
