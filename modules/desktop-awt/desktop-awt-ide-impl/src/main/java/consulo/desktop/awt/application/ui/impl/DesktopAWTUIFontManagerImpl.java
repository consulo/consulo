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

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.impl.internal.UIFontManagerImpl;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Pair;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 21-Feb-22
 */
@Singleton
@ServiceImpl
public class DesktopAWTUIFontManagerImpl extends UIFontManagerImpl {
  @Override
  public void afterLoadState() {
    // 1. Sometimes system font cannot display standard ASCII symbols. If so we have
    // find any other suitable font withing "preferred" fonts first.
    boolean fontIsValid = UIUtil.isValidFont(new Font(getFontName(), Font.PLAIN, getFontSize()));
    if (!fontIsValid) {
      final String[] preferredFonts = {"dialog", "Arial", "Tahoma"};
      for (String preferredFont : preferredFonts) {
        if (UIUtil.isValidFont(new Font(preferredFont, Font.PLAIN, getFontSize()))) {
          setFontName(preferredFont);
          fontIsValid = true;
          break;
        }
      }

      // 2. If all preferred fonts are not valid in current environment
      // we have to find first valid font (if any)
      if (!fontIsValid) {
        String[] fontNames = UIUtil.getValidFontNames(false);
        if (fontNames.length > 0) {
          setFontName(fontNames[0]);
        }
      }
    }
  }

  @Nonnull
  @Override
  protected Pair<String, Integer> resolveSystemFontData() {
    return JBUIScale.getSystemFontData();
  }
}
