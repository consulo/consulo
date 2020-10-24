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
package consulo.ui.font;

import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public interface FontManager {
  @Nonnull
  static FontManager get() {
    return UIInternal.get()._FontManager_get();
  }

  @Nonnull
  Set<String> getAvaliableFontNames();

  @Nonnull
  default Font createFont(@Nonnull String fontName, int fontSize) {
    return createFont(fontName, fontSize, 0);
  }

  @Nonnull
  Font createFont(@Nonnull String fontName, int fontSize, int fontStyle);
}
