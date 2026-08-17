/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.page.ColorScheme;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.logging.Logger;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.impl.style.StyleImpl;
import consulo.ui.impl.style.StyleManagerImpl;
import consulo.ui.impl.style.UITheme;
import consulo.ui.style.Style;

import java.util.List;

/**
 * @author VISTALL
 * @since 2017-09-15
 */
public class WebStyleManagerImpl extends StyleManagerImpl {
    private static final Logger LOG = Logger.getInstance(WebStyleManagerImpl.class);

    private static final WebStyleImpl LIGHT = new WebStyleImpl(Style.LIGHT_ID, "Light", false, ColorScheme.Value.LIGHT);
    private static final WebStyleImpl SEMI_DARK = new WebStyleImpl(Style.SEMI_DARK, "Dark Grey", true, ColorScheme.Value.DARK);
    private static final WebStyleImpl DARK = new WebStyleImpl(Style.DARK_ID, "Dark", true, ColorScheme.Value.DARK);

    private List<Style> myStyles = List.of(LIGHT, SEMI_DARK, DARK);

    public static final WebStyleManagerImpl ourInstance = new WebStyleManagerImpl();

    private Style myCurrentStyle = LIGHT;

    @Override
    public List<Style> getStyles() {
        return myStyles;
    }

    @Override
    public Style getCurrentStyle() {
        return myCurrentStyle;
    }

    @Override
    public void setCurrentStyle(Style style) {
        Style oldStyle = myCurrentStyle;
        myCurrentStyle = style;

        IconLibraryManager.get().setActiveLibraryFromActiveStyle();

        updateEditorColorsScheme(style);

        fireStyleChanged(oldStyle, style);
    }

    private static void updateEditorColorsScheme(Style style) {
        UITheme theme = style instanceof StyleImpl styleImpl ? styleImpl.getTheme() : null;
        String editorSchemeId = theme == null ? null : theme.getEditorSchemeId();
        if (editorSchemeId == null) {
            return;
        }

        EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
        EditorColorsScheme scheme = editorColorsManager.getScheme(editorSchemeId);
        if (scheme == null) {
            LOG.warn("No editor scheme " + editorSchemeId + " for style " + style.getId());
            return;
        }

        // the awt side uses the no-refresh variant because its laf change repaints everything afterwards,
        // there is no such repaint here
        editorColorsManager.setGlobalScheme(scheme);

        LOG.info("Editor scheme of " + style.getId() + " is " + editorColorsManager.getGlobalScheme().getName());
    }
}
