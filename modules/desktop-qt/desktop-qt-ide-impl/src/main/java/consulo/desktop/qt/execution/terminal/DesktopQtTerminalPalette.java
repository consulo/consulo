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

/**
 * What the sixteen colours an indexed style names are drawn as. The numbers are the xterm defaults, which is what
 * the awt terminal falls back to before a colour scheme has anything to say.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtTerminalPalette extends ColorPalette {
    public static final DesktopQtTerminalPalette INSTANCE = new DesktopQtTerminalPalette();

    private static final Color[] ourColors = {
        new Color(0x00, 0x00, 0x00),
        new Color(0xCD, 0x00, 0x00),
        new Color(0x00, 0xCD, 0x00),
        new Color(0xCD, 0xCD, 0x00),
        new Color(0x00, 0x00, 0xEE),
        new Color(0xCD, 0x00, 0xCD),
        new Color(0x00, 0xCD, 0xCD),
        new Color(0xE5, 0xE5, 0xE5),

        new Color(0x7F, 0x7F, 0x7F),
        new Color(0xFF, 0x00, 0x00),
        new Color(0x00, 0xFF, 0x00),
        new Color(0xFF, 0xFF, 0x00),
        new Color(0x5C, 0x5C, 0xFF),
        new Color(0xFF, 0x00, 0xFF),
        new Color(0x00, 0xFF, 0xFF),
        new Color(0xFF, 0xFF, 0xFF)
    };

    private DesktopQtTerminalPalette() {
    }

    @Override
    protected Color getForegroundByColorIndex(int colorIndex) {
        return ourColors[colorIndex % ourColors.length];
    }

    @Override
    protected Color getBackgroundByColorIndex(int colorIndex) {
        return ourColors[colorIndex % ourColors.length];
    }
}
