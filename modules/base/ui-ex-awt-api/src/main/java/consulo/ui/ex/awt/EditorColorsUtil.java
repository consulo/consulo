/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/**
 * @author gregsh
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
    return consulo.colorScheme.EditorColorsUtil.getColorSchemeForBackground(background);
  }

  /**
   * @return the appropriate color scheme for UI other than text editor (QuickDoc, UsagesView, etc.)
   * depending on the current LAF, current editor color scheme and the component background.
   */
  @Nonnull
  public static EditorColorsScheme getColorSchemeForComponent(@Nullable JComponent component) {
    return getColorSchemeForBackground(component != null ? TargetAWT.from(component.getBackground()) : null);
  }

  @Nonnull
  @Deprecated
  public static EditorColorKey createColorKey(@NonNls @Nonnull String name, @Nonnull Color defaultColor) {
    return EditorColorKey.createColorKey(name, TargetAWT.from(JBColor.namedColor(name, defaultColor)));
  }

  @Nullable
  public static ColorValue getColor(@Nullable Component component, @Nonnull EditorColorKey key) {
    Function<EditorColorKey, ColorValue> function = UIUtil.getClientProperty(component, EditorColorKey.FUNCTION_KEY);
    ColorValue color = function == null ? null : function.apply(key);
    return color != null ? color : key.getDefaultColorValue();
  }
}
