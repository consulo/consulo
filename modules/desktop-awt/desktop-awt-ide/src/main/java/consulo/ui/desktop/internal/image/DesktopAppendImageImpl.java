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
package consulo.ui.desktop.internal.image;

import com.intellij.ui.RowIcon;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-05-07
 */
public class DesktopAppendImageImpl extends RowIcon implements Image, DesktopImage<DesktopAppendImageImpl> {
  private final Image myImg1;
  private final Image myImg2;

  public DesktopAppendImageImpl(Image img1, Image img2) {
    super(2, Alignment.CENTER);
    myImg1 = img1;
    setIcon(TargetAWT.to(img1), 0);
    myImg2 = img2;             
    setIcon(TargetAWT.to(img2), 1);
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
  public DesktopAppendImageImpl copyWithScale(float scale) {
    return new DesktopAppendImageImpl(ImageEffects.resize(myImg1, scale), ImageEffects.resize(myImg2, scale));
  }

  @Nonnull
  @Override
  public DesktopAppendImageImpl copyWithTargetIconLibrary(@Nonnull String iconLibraryId, @Nonnull Function<Image, Image> converter) {
    return new DesktopAppendImageImpl(converter.apply(myImg1), converter.apply(myImg2));
  }
}
