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
package consulo.desktop.qt.execution.terminal;

import com.jediterm.core.Color;
import com.jediterm.terminal.emulator.ColorPalette;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.execution.process.ColoredOutputTypeRegistry;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The colours the terminal draws with, taken from the console keys of the active scheme so the terminal follows the
 * theme rather than carrying colours of its own - the same keys the awt and web terminals read.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtTerminalPalette extends ColorPalette {
    public static DesktopQtTerminalPalette ofGlobalScheme() {
        return new DesktopQtTerminalPalette(() -> EditorColorsManager.getInstance().getGlobalScheme());
    }

    private final Supplier<EditorColorsScheme> myScheme;

    public DesktopQtTerminalPalette(Supplier<EditorColorsScheme> scheme) {
        myScheme = scheme;
    }

    public Color getDefaultForeground() {
        EditorColorsScheme scheme = myScheme.get();

        ColorValue color = scheme.getAttributes(ConsoleViewContentType.NORMAL_OUTPUT_KEY).getForegroundColor();

        return toColor(color != null ? color : scheme.getDefaultForeground());
    }

    public Color getDefaultBackground() {
        EditorColorsScheme scheme = myScheme.get();

        ColorValue color = scheme.getColor(ConsoleViewContentType.CONSOLE_BACKGROUND_KEY);

        return toColor(color != null ? color : scheme.getDefaultBackground());
    }

    @Override
    protected Color getForegroundByColorIndex(int colorIndex) {
        ColorValue color = ansi(colorIndex, true);

        return color != null ? toColor(color) : getDefaultForeground();
    }

    @Override
    protected Color getBackgroundByColorIndex(int colorIndex) {
        ColorValue color = ansi(colorIndex, false);

        return color != null ? toColor(color) : getDefaultBackground();
    }

    private @Nullable ColorValue ansi(int colorIndex, boolean foreground) {
        TextAttributes attributes = myScheme.get().getAttributes(ColoredOutputTypeRegistry.getAnsiColorKey(colorIndex));
        if (attributes == null) {
            return null;
        }

        ColorValue color = foreground ? attributes.getForegroundColor() : attributes.getBackgroundColor();
        if (color != null) {
            return color;
        }

        return foreground ? attributes.getBackgroundColor() : attributes.getForegroundColor();
    }

    private static Color toColor(ColorValue color) {
        RGBColor rgb = color.toRGB();

        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
    }
}
