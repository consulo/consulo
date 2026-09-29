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
package consulo.desktop.awt.uiOld;

import consulo.application.Application;
import consulo.logging.Logger;
import consulo.ui.ex.font.BundledFont;
import consulo.ui.ex.internal.BundledFontRegistry;

import java.awt.*;
import java.io.InputStream;
import java.util.List;

public class DesktopAWTFontRegistry {
    private static final Logger LOG = Logger.getInstance(DesktopAWTFontRegistry.class);

    public static void registerBundledFonts(Application application) {
        try {
            GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();

            for (List<BundledFont> fonts : application.getInstance(BundledFontRegistry.class).getFonts().values()) {
                for (BundledFont font : fonts) {
                    registerFont(environment, font);
                }
            }
        }
        catch (Throwable e) {
            LOG.error("Cannot register bundled fonts", e);
        }
    }

    private static void registerFont(GraphicsEnvironment environment, BundledFont font) {
        try (InputStream stream = font.url().openStream()) {
            environment.registerFont(Font.createFont(Font.TRUETYPE_FONT, stream));
        }
        catch (Exception e) {
            LOG.error("Cannot register font: " + font.url(), e);
        }
    }
}
