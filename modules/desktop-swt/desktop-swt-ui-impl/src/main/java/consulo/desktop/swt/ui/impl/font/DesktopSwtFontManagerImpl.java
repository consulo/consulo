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

import consulo.desktop.swt.ui.impl.DesktopSwtUIAccess;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import org.eclipse.swt.graphics.FontData;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class DesktopSwtFontManagerImpl implements FontManager {
  public static final DesktopSwtFontManagerImpl INSTANCE = new DesktopSwtFontManagerImpl();

  @Nonnull
  @Override
  public Set<String> getAvaliableFontNames() {
    FontData[] fontList = DesktopSwtUIAccess.INSTANCE.getDisplay().getFontList(null, true);
    return Arrays.stream(fontList).map(FontData::getName).collect(Collectors.toSet());
  }

  @Nonnull
  @Override
  public Font createFont(@Nonnull String fontName, int fontSize, int fontStyle) {
    return new DesktopSwtFontImpl(fontName, fontSize, fontStyle);
  }
}
