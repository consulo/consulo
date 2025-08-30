/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.ui.awt;

import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.internal.VersionControlSystemInternal;
import jakarta.annotation.Nonnull;

import java.awt.*;

public class VcsFontUtil {
    @Nonnull
    public static String getHtmlWithFonts(@Nonnull String input) {
        Font font = UIUtil.getLabelFont();
        return getHtmlWithFonts(input, font.getStyle(), font);
    }

    @Nonnull
    public static String getHtmlWithFonts(@Nonnull String input, int style, @Nonnull Font baseFont) {
        return VersionControlSystemInternal.getInstance().getHtmlWithFonts(input, style, baseFont);
    }
}
