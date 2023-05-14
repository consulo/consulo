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
package consulo.colorScheme;

import consulo.ui.color.ColorValue;
import consulo.ui.style.StyleManager;
import consulo.ui.util.ColorValueUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 26-Feb-22
 */
public class EditorColorsUtil {
  private EditorColorsUtil() {
  }

  /**
   * @return the appropriate color scheme for UI other than text editor (QuickDoc, UsagesView, etc.)
   * depending on the current LAF and current editor color scheme.
   */
  @Nonnull
  public static EditorColorsScheme getGlobalOrDefaultColorScheme() {
    return getColorSchemeForBackground(null);
  }

  @Nullable
  public static ColorValue getGlobalOrDefaultColor(@Nonnull EditorColorKey colorKey) {
    return getColorSchemeForBackground(null).getColor(colorKey);
  }


  /**
   * @return the appropriate color scheme for UI other than text editor (QuickDoc, UsagesView, etc.)
   * depending on the current LAF, current editor color scheme and background color.
   */
  public static EditorColorsScheme getColorSchemeForBackground(@Nullable ColorValue background) {
    EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
    boolean dark1 = background == null ? StyleManager.get().getCurrentStyle().isDark() : ColorValueUtil.isDark(background);
    boolean dark2 = ColorValueUtil.isDark(globalScheme.getDefaultBackground());
    if (dark1 != dark2) {
      EditorColorsScheme scheme = EditorColorsManager.getInstance().getScheme(EditorColorsScheme.DEFAULT_SCHEME_NAME);
      if (scheme != null) {
        return scheme;
      }
    }
    return globalScheme;
  }
}
