/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.hint;

import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorColorsUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.JBColor;
import consulo.ui.util.LightDarkColorValue;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 29-Apr-22
 */
public class HintColorUtil {
  /**
   * @deprecated use getInformationColor()
   */
  @Deprecated
  public static final Color INFORMATION_COLOR = new JBColor(0xF7F7F7, 0x4B4D4D);
  public static final Color INFORMATION_BORDER_COLOR = JBColor.namedColor("InformationHint.borderColor", new JBColor(0xE0E0E0, 0x5C5E61));
  /**
   * @deprecated use getErrorColor()
   */
  @Deprecated
  public static final Color ERROR_COLOR = new JBColor(0xffdcdc, 0x781732);

  public static final EditorColorKey INFORMATION_COLOR_KEY = EditorColorKey.createColorKey("INFORMATION_HINT", new LightDarkColorValue(new RGBColor(247, 247, 247), new RGBColor(75, 77, 77)));
  public static final EditorColorKey QUESTION_COLOR_KEY = EditorColorKey.createColorKey("QUESTION_HINT", new LightDarkColorValue(new RGBColor(181, 208, 251), new RGBColor(55, 108, 137)));
  public static final EditorColorKey ERROR_COLOR_KEY = EditorColorKey.createColorKey("ERROR_HINT", new LightDarkColorValue(new RGBColor(255, 220, 220), new RGBColor(120, 23, 50)));

  public static final Color QUESTION_UNDERSCORE_COLOR = JBColor.foreground();

  public static final EditorColorKey RECENT_LOCATIONS_SELECTION_KEY =
          EditorColorKey.createColorKey("RECENT_LOCATIONS_SELECTION", new LightDarkColorValue(new RGBColor(233, 238, 245), new RGBColor(56, 56, 56)));

  private HintColorUtil() {
  }

  @Nonnull
  public static ColorValue getInformationColor() {
    return ObjectUtil.notNull(EditorColorsUtil.getGlobalOrDefaultColor(INFORMATION_COLOR_KEY), INFORMATION_COLOR_KEY.getDefaultColorValue());
  }

  @Nonnull
  public static ColorValue getQuestionColor() {
    return ObjectUtil.notNull(EditorColorsUtil.getGlobalOrDefaultColor(QUESTION_COLOR_KEY), QUESTION_COLOR_KEY.getDefaultColorValue());
  }

  @Nonnull
  public static ColorValue getErrorColor() {
    return ObjectUtil.notNull(EditorColorsUtil.getGlobalOrDefaultColor(ERROR_COLOR_KEY), ERROR_COLOR_KEY.getDefaultColorValue());
  }

  @Nonnull
  public static ColorValue getRecentLocationsSelectionColor(EditorColorsScheme colorsScheme) {
    return ObjectUtil.notNull(colorsScheme.getColor(RECENT_LOCATIONS_SELECTION_KEY), RECENT_LOCATIONS_SELECTION_KEY.getDefaultColorValue());
  }
}
