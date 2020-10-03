/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.web.internal.image;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-10-03
 */
public class WebDataImageImpl implements Image {
  private final byte[] myX1Data;
  private final byte[] myX2Data;
  private final boolean myIsSVG;
  private final int myWidth;
  private final int myHeight;

  public WebDataImageImpl(byte[] x1Data, byte[] x2Data, boolean isSVG, int width, int height) {
    myX1Data = x1Data;
    myX2Data = x2Data;
    myIsSVG = isSVG;
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

  public boolean isSVG() {
    return myIsSVG;
  }

  @Nonnull
  public byte[] getData() {
    if(myX2Data != null) {
      return myX2Data;
    }

    return myX1Data;
  }
}
