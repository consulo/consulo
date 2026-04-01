/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
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
package consulo.desktop.awt.execution.terminal;

import com.jediterm.core.Color;
import com.jediterm.terminal.emulator.ColorPalette;
import com.jediterm.terminal.ui.AwtTransformers;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.execution.process.ColoredOutputTypeRegistry;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

/**
 * @author traff
 */
public class JBTerminalSchemeColorPalette extends ColorPalette {
    private final EditorColorsScheme myColorsScheme;

    protected JBTerminalSchemeColorPalette(EditorColorsScheme scheme) {
        super();
        myColorsScheme = scheme;
    }

    public Color getDefaultForeground() {
        java.awt.Color foregroundColor =
            TargetAWT.to(myColorsScheme.getAttributes(ConsoleViewContentType.NORMAL_OUTPUT_KEY).getForegroundColor());
        return AwtTransformers.fromAwtColor(
            foregroundColor != null ? foregroundColor : TargetAWT.to(myColorsScheme.getDefaultForeground()));
    }

    public Color getDefaultBackground() {
        java.awt.Color backgroundColor = TargetAWT.to(myColorsScheme.getColor(ConsoleViewContentType.CONSOLE_BACKGROUND_KEY));
        return AwtTransformers.fromAwtColor(
            backgroundColor != null ? backgroundColor : TargetAWT.to(myColorsScheme.getDefaultBackground()));
    }

    private @Nullable TextAttributes getAttributesByColorIndex(int index) {
        return myColorsScheme.getAttributes(ColoredOutputTypeRegistry.getAnsiColorKey(index));
    }

    @Override
    protected Color getForegroundByColorIndex(int colorIndex) {
        TextAttributes attributes = getAttributesByColorIndex(colorIndex);
        java.awt.Color color = null;
        if (attributes != null) {
            color = TargetAWT.to(attributes.getForegroundColor());
            if (color == null) {
                color = TargetAWT.to(attributes.getBackgroundColor());
            }
        }
        if (color != null) {
            return AwtTransformers.fromAwtColor(color);
        }
        return getDefaultForeground();
    }

    @Override
    protected Color getBackgroundByColorIndex(int colorIndex) {
        TextAttributes attributes = getAttributesByColorIndex(colorIndex);
        java.awt.Color color = null;
        if (attributes != null) {
            color = TargetAWT.to(attributes.getBackgroundColor());
            if (color == null) {
                color = TargetAWT.to(attributes.getForegroundColor());
            }
        }
        if (color != null) {
            return AwtTransformers.fromAwtColor(color);
        }
        return getDefaultBackground();
    }
}
