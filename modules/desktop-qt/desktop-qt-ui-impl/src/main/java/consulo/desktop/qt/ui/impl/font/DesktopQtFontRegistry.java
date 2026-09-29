/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.ui.impl.font;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.ui.ex.font.BundledFont;
import consulo.ui.ex.internal.BundledFontRegistry;
import io.qt.gui.QFontDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Hands the faces of {@link BundledFontRegistry} to qt, the counterpart of the stylesheet the web frontend
 * writes and of the graphics environment the awt one registers with.
 * <p>
 * Without this a scheme asking for JetBrains Mono is silently answered with whatever monospace the system has,
 * and the coding ligatures of the bundled typeface go with it - qt reports the substitution only through
 * {@code QFontInfo.exactMatch}, never as an error.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public final class DesktopQtFontRegistry {
    private static final Logger LOG = Logger.getInstance(DesktopQtFontRegistry.class);

    private DesktopQtFontRegistry() {
    }

    public static List<BundledFont> collectBundledFonts() {
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            LOG.error("Bundled fonts are collected before the application exists");
            return List.of();
        }

        return application.getInstance(BundledFontRegistry.class).getFonts().values().stream().flatMap(Collection::stream).toList();
    }

    /**
     * Must run on the qt thread and after the application exists - the font database is part of it.
     */
    public static void registerFonts(List<BundledFont> fonts) {
        for (BundledFont font : fonts) {
            try (InputStream stream = font.url().openStream()) {
                if (QFontDatabase.addApplicationFontFromData(stream.readAllBytes()) < 0) {
                    throw new IOException("Rejected by qt");
                }
            }
            catch (Exception e) {
                LOG.error("Cannot register font: " + font.url(), e);
            }
        }
    }
}
