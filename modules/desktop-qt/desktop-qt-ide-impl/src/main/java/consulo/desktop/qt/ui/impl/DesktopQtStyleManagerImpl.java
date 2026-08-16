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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.image.DesktopQtIconLibraryManager;
import consulo.application.Application;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.logging.Logger;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconRefresher;
import consulo.ui.impl.style.StyleManagerImpl;
import consulo.ui.impl.style.StyleImpl;
import consulo.ui.style.Style;
import consulo.ui.impl.style.UITheme;
import io.qt.core.Qt;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QPalette;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtStyleManagerImpl extends StyleManagerImpl {
    private static final Logger LOG = Logger.getInstance(DesktopQtStyleManagerImpl.class);


    private static final Style LIGHT = new DesktopQtStyleImpl(Style.LIGHT_ID);
    private static final Style SEMI_DARK = new DesktopQtStyleImpl(Style.SEMI_DARK);
    private static final Style DARK = new DesktopQtStyleImpl(Style.DARK_ID);

    public static final DesktopQtStyleManagerImpl INSTANCE = new DesktopQtStyleManagerImpl();

    private static final List<Style> ourStyles = List.of(LIGHT, SEMI_DARK, DARK);

    private Style myCurrentStyle = LIGHT;

    /** the preference of the desktop is only the default, and a theme picked in the ide outranks it */
    private boolean myStyleChosen;

    @Override
    public List<Style> getStyles() {
        return ourStyles;
    }

    @Override
    public Style getCurrentStyle() {
        return myCurrentStyle;
    }

    @Override
    public void setCurrentStyle(Style style) {
        myStyleChosen = true;

        applyStyle(style);

        updateEditorColorsScheme(style);
    }

    /**
     * Only reachable from the public entry point on purpose: {@link #syncWithPlatform()} runs on the qt thread
     * straight after the application is initialized, long before the container can answer a service.
     */
    private static void updateEditorColorsScheme(Style style) {
        UITheme theme = style instanceof StyleImpl styleImpl ? styleImpl.getTheme() : null;
        String editorSchemeId = theme == null ? null : theme.getEditorSchemeId();
        if (editorSchemeId == null) {
            return;
        }

        if (!Application.get().isInitialized()) {
            return;
        }

        EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
        EditorColorsScheme scheme = editorColorsManager.getScheme(editorSchemeId);
        if (scheme == null) {
            LOG.warn("No editor scheme " + editorSchemeId + " for style " + style.getId());
            return;
        }

        editorColorsManager.setGlobalScheme(scheme);
    }

    /**
     * Also the way an icon library picked by hand in the settings reaches the ui - it changes no style, so
     * nothing but this is fired for it.
     */
    @Override
    public void refreshUI() {
        DesktopQtStyleApplier.apply(myCurrentStyle);

        DesktopQtIconRefresher.refreshAll();
    }

    /**
     * Follows whatever color scheme the desktop asks for, so the theme the ide opens with matches the rest of the
     * session. Qt answers Unknown on a platform which exposes no preference, and there the palette is the only hint
     * left.
     */
    public void syncWithPlatform() {
        if (!myStyleChosen) {
            Style style = isPlatformDark() ? DARK : LIGHT;
            if (style != myCurrentStyle) {
                applyStyle(style);
                return;
            }
        }

        // the platform theme hands qt a palette of its own whenever the color scheme changes, and it has to be
        // taken back out - the initial call comes through here too, which is what first paints the theme
        DesktopQtStyleApplier.apply(myCurrentStyle);
    }

    private void applyStyle(Style style) {
        Style oldStyle = myCurrentStyle;
        myCurrentStyle = style;

        DesktopQtStyleApplier.apply(style);

        DesktopQtIconLibraryManager.INSTANCE.setActiveLibraryFromActiveStyle();

        // a qt widget holds the pixmap it was handed rather than resolving an icon while it paints, so a repaint
        // of everything - which is all the awt frontend needs - would keep drawing the icons of the old library
        DesktopQtIconRefresher.refreshAll();

        fireStyleChanged(oldStyle, style);
    }

    private static boolean isPlatformDark() {
        Qt.ColorScheme colorScheme = QGuiApplication.styleHints().colorScheme();

        return switch (colorScheme) {
            case Dark -> true;
            case Light -> false;
            default -> QGuiApplication.palette().color(QPalette.ColorRole.Window).lightness() < 128;
        };
    }
}
