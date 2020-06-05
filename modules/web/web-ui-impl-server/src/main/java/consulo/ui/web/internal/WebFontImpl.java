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
package consulo.ui.web.internal;

import consulo.ui.font.Font;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public class WebFontImpl implements Font {
  private final String myFontName;
  private final int myFontSize;
  private final int myFontStyle;

  public WebFontImpl(String fontName, int fontSize, int fontStyle) {
    myFontName = fontName;
    myFontSize = fontSize;
    myFontStyle = fontStyle;
  }

  @Nonnull
  @Override
  public String getName() {
    return myFontName;
  }

  @Nonnull
  @Override
  public String getFontName() {
    return myFontName;
  }

  @Nonnull
  @Override
  public String getFamily() {
    return "";
  }

  @Override
  public int getFontStyle() {
    return myFontStyle;
  }

  @Override
  public int getFontSize() {
    return myFontSize;
  }

  @Nonnull
  @Override
  public Font buildNewFont(int newSize) {
    return new WebFontImpl(myFontName, newSize, myFontStyle);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WebFontImpl webFont = (WebFontImpl)o;
    return myFontSize == webFont.myFontSize && myFontStyle == webFont.myFontStyle && Objects.equals(myFontName, webFont.myFontName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myFontName, myFontSize, myFontStyle);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("WebFontImpl{");
    sb.append("myFontName='").append(myFontName).append('\'');
    sb.append(", myFontSize=").append(myFontSize);
    sb.append(", myFontStyle=").append(myFontStyle);
    sb.append('}');
    return sb.toString();
  }
}
