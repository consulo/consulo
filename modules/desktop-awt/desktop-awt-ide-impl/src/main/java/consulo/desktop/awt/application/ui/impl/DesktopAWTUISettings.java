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
package consulo.desktop.awt.application.ui.impl;

import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.ui.ex.awt.ComponentTreeEventDispatcher;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Pair;
import jakarta.inject.Singleton;

import java.awt.*;

/**
 * @author VISTALL
 * @since 21-Feb-22
 */
@Singleton
public class DesktopAWTUISettings extends UISettings {
  private final ComponentTreeEventDispatcher<UISettingsListener> myDispatcher = ComponentTreeEventDispatcher.create(UISettingsListener.class);

  @Override
  protected void notifyDispatcher() {
    myDispatcher.getMulticaster().uiSettingsChanged(this);
  }

  @Override
  protected void validateFont() {
    setSystemFontFaceAndSize();
    // 1. Sometimes system font cannot display standard ASCII symbols. If so we have
    // find any other suitable font withing "preferred" fonts first.
    boolean fontIsValid = UIUtil.isValidFont(new Font(FONT_FACE, Font.PLAIN, FONT_SIZE));
    if (!fontIsValid) {
      final String[] preferredFonts = {"dialog", "Arial", "Tahoma"};
      for (String preferredFont : preferredFonts) {
        if (UIUtil.isValidFont(new Font(preferredFont, Font.PLAIN, FONT_SIZE))) {
          FONT_FACE = preferredFont;
          fontIsValid = true;
          break;
        }
      }

      // 2. If all preferred fonts are not valid in current environment
      // we have to find first valid font (if any)
      if (!fontIsValid) {
        String[] fontNames = UIUtil.getValidFontNames(false);
        if (fontNames.length > 0) {
          FONT_FACE = fontNames[0];
        }
      }
    }
  }

  private void setSystemFontFaceAndSize() {
    if (FONT_FACE == null || FONT_SIZE <= 0) {
      final Pair<String, Integer> fontData = getSystemFontFaceAndSize();
      FONT_FACE = fontData.first;
      FONT_SIZE = fontData.second;
    }
  }

  public static Pair<String, Integer> getSystemFontFaceAndSize() {
    final Pair<String, Integer> fontData = JBUIScale.getSystemFontData();
    if (fontData != null) {
      return fontData;
    }

    return Pair.create("Dialog", 12);
  }

  private static boolean hasDefaultFontSetting(final UISettings settings) {
    final Pair<String, Integer> fontData = getSystemFontFaceAndSize();
    return fontData.first.equals(settings.FONT_FACE) && fontData.second.equals(settings.FONT_SIZE);
  }
}
