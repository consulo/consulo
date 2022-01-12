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
package consulo.desktop.swt.ui.impl.font;

import consulo.ui.font.Font;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class DesktopSwtFontImpl implements Font {
  private final String myFontName;
  private final int myFontSize;
  private final int myFontStyle;

  public DesktopSwtFontImpl(String fontName, int fontSize, int fontStyle) {
    myFontName = fontName;
    myFontSize = fontSize;
    myFontStyle = fontStyle;
  }

  @Nonnull
  @Override
  public String getName() {
    return null;
  }

  @Nonnull
  @Override
  public String getFontName() {
    return null;
  }

  @Nonnull
  @Override
  public String getFamily() {
    return null;
  }

  @Override
  public int getFontStyle() {
    return 0;
  }

  @Override
  public int getFontSize() {
    return 0;
  }

  @Nonnull
  @Override
  public Font buildNewFont(int newSize) {
    return new DesktopSwtFontImpl(myFontName, newSize, myFontStyle);
  }
}
