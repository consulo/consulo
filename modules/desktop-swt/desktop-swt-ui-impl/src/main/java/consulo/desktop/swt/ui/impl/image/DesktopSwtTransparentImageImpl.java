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

import consulo.ui.image.Image;
import org.eclipse.swt.graphics.GC;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class DesktopSwtTransparentImageImpl implements Image, DesktopSwtImage {
  private final Image myOriginal;
  private final float myAlpha;

  public DesktopSwtTransparentImageImpl(Image original, float alpha) {
    myOriginal = original;
    myAlpha = alpha;
  }

  @Override
  public int getHeight() {
    return myOriginal.getHeight();
  }

  @Override
  public int getWidth() {
    return myOriginal.getWidth();
  }

  @Nonnull
  @Override
  public org.eclipse.swt.graphics.Image toSWTImage() {
    org.eclipse.swt.graphics.Image swtImage = new org.eclipse.swt.graphics.Image(null, getWidth(), getHeight());

    GC gc = new GC(swtImage);
    gc.setAlpha((int)(myAlpha * 255));
    gc.drawImage(((DesktopSwtImage)myOriginal).toSWTImage(), 0, 0);
    gc.dispose();

    return swtImage;
  }
}
