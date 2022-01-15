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
package consulo.desktop.awt.util;

import consulo.desktop.util.awt.MorphValue;
import consulo.ui.style.StyleManager;

/**
 * @author VISTALL
 * @since 2020-08-01
 *
 * This is optimization for call isDarkTheme, since IDEA love call for this check inside UI paint
 */
public class DarkThemeCalculator {
   private static MorphValue<Boolean> ourDarkMode = MorphValue.of(() -> StyleManager.get().getCurrentStyle().isDark());

   public static boolean isDark() {
     return ourDarkMode.getValue();
   }
}
