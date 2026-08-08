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
package consulo.web.internal.execution.terminal;

import com.vaadin.flow.internal.JacksonUtils;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.execution.process.ColoredOutputTypeRegistry;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.ui.color.ColorValue;
import consulo.web.internal.ui.WebColors;
import tools.jackson.databind.node.ObjectNode;

/**
 * Builds the xterm palette out of the editor color scheme, the same source the desktop terminal reads, so the
 * terminal follows the theme rather than carrying colors of its own.
 *
 * @author VISTALL
 * @since 2026-08-08
 */
public class WebTerminalThemeBuilder {
    private WebTerminalThemeBuilder() {
    }

    public static ObjectNode build() {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

        ColorValue background = scheme.getColor(ConsoleViewContentType.CONSOLE_BACKGROUND_KEY);
        if (background == null) {
            background = scheme.getDefaultBackground();
        }

        ColorValue foreground = scheme.getAttributes(ConsoleViewContentType.NORMAL_OUTPUT_KEY).getForegroundColor();
        if (foreground == null) {
            foreground = scheme.getDefaultForeground();
        }

        ObjectNode theme = JacksonUtils.createObjectNode();
        theme.put("background", WebColors.toCssColor(background));
        theme.put("foreground", WebColors.toCssColor(foreground));
        theme.put("cursor", WebColors.toCssColor(foreground));
        theme.put("cursorAccent", WebColors.toCssColor(background));

        // index 7 is the plain white of ansi and index 15 the bright one, which is why the scheme names them
        // gray and white
        String[] names = {
            "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white",
            "brightBlack", "brightRed", "brightGreen", "brightYellow",
            "brightBlue", "brightMagenta", "brightCyan", "brightWhite"
        };
        for (int i = 0; i < names.length; i++) {
            theme.put(names[i], ansi(scheme, i, foreground));
        }

        return theme;
    }

    private static String ansi(EditorColorsScheme scheme, int index, ColorValue fallback) {
        TextAttributes attributes = scheme.getAttributes(ColoredOutputTypeRegistry.getAnsiColorKey(index));

        ColorValue color = null;
        if (attributes != null) {
            color = attributes.getForegroundColor();
            if (color == null) {
                color = attributes.getBackgroundColor();
            }
        }

        return WebColors.toCssColor(color != null ? color : fallback);
    }
}
