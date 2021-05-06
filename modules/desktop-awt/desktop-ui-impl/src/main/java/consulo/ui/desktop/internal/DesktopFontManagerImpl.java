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
package consulo.ui.desktop.internal;

import consulo.logging.Logger;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public class DesktopFontManagerImpl implements FontManager {
  public static final DesktopFontManagerImpl ourInstance = new DesktopFontManagerImpl();

  private static final Logger LOG = Logger.getInstance(DesktopFontManagerImpl.class);

  @Nonnull
  @Override
  public Set<String> getAvaliableFontNames() {
    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Set<String> fontNames = new TreeSet<>();
    try {
      java.awt.Font[] fonts = environment.getAllFonts();
      for (java.awt.Font font : fonts) {
        fontNames.add(font.getFontName());
      }
    }
    catch (Exception e) {
      LOG.error(e);
    }
    return fontNames;
  }

  @Nonnull
  @Override
  public Font createFont(@Nonnull String fontName, int fontSize, int fontStyle) {
    return new DesktopFontImpl(fontName, fontSize, fontStyle);
  }
}
