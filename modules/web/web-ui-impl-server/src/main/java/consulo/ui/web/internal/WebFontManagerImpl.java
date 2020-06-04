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
import consulo.ui.font.FontManager;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public class WebFontManagerImpl implements FontManager {
  public static final WebFontManagerImpl ourInstance = new WebFontManagerImpl();

  private static final String DEFAULT_FONT = "Default";

  @Nonnull
  @Override
  public Set<String> getAvaliableFontNames() {
    return Collections.singleton(DEFAULT_FONT);
  }

  @Nonnull
  @Override
  public Font createFont(@Nonnull String fontName, int fontSize, int fontStyle) {
    return new WebFontImpl(fontName, fontSize, fontStyle);
  }
}
