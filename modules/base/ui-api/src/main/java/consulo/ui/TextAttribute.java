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
package consulo.ui;

import consulo.ui.font.Font;
import consulo.ui.color.ColorValue;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-14
 */
public final class TextAttribute {
  @Deprecated
  public static final int STYLE_PLAIN = Font.STYLE_PLAIN;
  @Deprecated
  public static final int STYLE_BOLD = Font.STYLE_BOLD;
  @Deprecated
  public static final int STYLE_ITALIC = Font.STYLE_ITALIC;

  public static final TextAttribute REGULAR = new TextAttribute(STYLE_PLAIN, null, null);
  public static final TextAttribute REGULAR_BOLD = new TextAttribute(STYLE_BOLD, null);
  public static final TextAttribute REGULAR_ITALIC = new TextAttribute(STYLE_ITALIC, null, null);
  public static final TextAttribute ERROR = new TextAttribute(STYLE_PLAIN, StandardColors.RED, null);
  public static final TextAttribute ERROR_BOLD = new TextAttribute(STYLE_BOLD, StandardColors.RED, null);

  public static final TextAttribute GRAYED = new TextAttribute(STYLE_PLAIN, ComponentColors.DISABLED_TEXT);

  public static final TextAttribute GRAY = new TextAttribute(STYLE_PLAIN, StandardColors.GRAY);

  private final int myStyle;
  private final ColorValue myBackgroundColor;
  private final ColorValue myForegroundColor;

  public TextAttribute(int style, @Nullable ColorValue foregroundColor) {
    this(style, foregroundColor, null);
  }

  public TextAttribute(int style, @Nullable ColorValue foregroundColor, @Nullable ColorValue backgroundColor) {
    myStyle = style;
    myForegroundColor = foregroundColor;
    myBackgroundColor = backgroundColor;
  }

  public int getStyle() {
    return myStyle;
  }

  @Nullable
  public ColorValue getForegroundColor() {
    return myForegroundColor;
  }

  @Nullable
  public ColorValue getBackgroundColor() {
    return myBackgroundColor;
  }
}
