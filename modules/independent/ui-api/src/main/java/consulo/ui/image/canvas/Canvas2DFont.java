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
package consulo.ui.image.canvas;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-06-21
 */
public class Canvas2DFont {
  @Nonnull
  private final String myFontName;
  private final int myFontSize;
  private final int myFontStyle;

  public Canvas2DFont(@Nonnull String fontName, int fontSize) {
    this(fontName, fontSize, 0);
  }

  public Canvas2DFont(@Nonnull String fontName, int fontSize, int fontStyle) {
    myFontName = fontName;
    myFontSize = fontSize;
    myFontStyle = fontStyle;
  }

  public int getFontSize() {
    return myFontSize;
  }

  public int getFontStyle() {
    return myFontStyle;
  }

  @Nonnull
  public String getFontName() {
    return myFontName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Canvas2DFont that = (Canvas2DFont)o;

    if (myFontSize != that.myFontSize) return false;
    if (myFontStyle != that.myFontStyle) return false;
    if (!myFontName.equals(that.myFontName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myFontName.hashCode();
    result = 31 * result + myFontSize;
    result = 31 * result + myFontStyle;
    return result;
  }
}
