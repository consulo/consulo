/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ui.ex.awt.util;

import consulo.application.ui.UISettings;
import consulo.ui.AntialiasingType;
import consulo.ui.ex.awt.DesktopAntialiasingType;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;

public class DesktopAntialiasingTypeUtil {
    @Nonnull
    public static DesktopAntialiasingType getAntialiasingTypeForSwingComponent() {
        UISettings uiSettings = UISettings.getInstanceOrNull();
        if (uiSettings != null) {
            AntialiasingType type = uiSettings.IDE_AA_TYPE;
            if (type != null) {
                return DesktopAntialiasingType.from(type);
            }
        }
        return DesktopAntialiasingType.GREYSCALE;
    }

    public static Object getKeyForCurrentScope(boolean inEditor) {
        UISettings uiSettings = UISettings.getInstanceOrNull();
        if (uiSettings != null) {
            AntialiasingType type = inEditor ? uiSettings.EDITOR_AA_TYPE : uiSettings.IDE_AA_TYPE;
            if (type != null) {
                return DesktopAntialiasingType.from(type).getHint();
            }
        }
        return RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
    }

    /**
     * This method has to be used for setting up antialiasing and rendering hints in
     * editors only.
     */
    public static void setupAntialiasing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        int lcdContrastValue = UIUtil.getLcdContrastValue();

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, lcdContrastValue);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, DesktopAntialiasingTypeUtil.getKeyForCurrentScope(true));

        UISettingsUtil.setupFractionalMetrics(g2d);
    }
}
