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
import org.eclipse.nebula.cwt.svg.SvgDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class DesktopSwtSvgLibraryImageImpl implements Image, DesktopSwtImage {
  private final int myWidth;
  private final int myHeight;
  private final SvgDocument mySvgDocument;

  public DesktopSwtSvgLibraryImageImpl(int width, int height, SvgDocument svgDocument) {
    myWidth = width;
    myHeight = height;
    mySvgDocument = svgDocument;
  }

  public SvgDocument getSvgDocument() {
    return mySvgDocument;
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

    org.eclipse.swt.graphics.Image swtImage = new org.eclipse.swt.graphics.Image(null, myWidth, myHeight);

    GC gc = new GC(swtImage);

    gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_TRANSPARENT));
    gc.fillRectangle(0, 0, myWidth, myHeight);

    mySvgDocument.apply(gc, swtImage.getBounds());
    gc.dispose();
    return swtImage;
  }
}
