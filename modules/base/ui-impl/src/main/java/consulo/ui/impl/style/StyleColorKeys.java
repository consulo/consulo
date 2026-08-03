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
package consulo.ui.impl.style;

import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleColorValue;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class StyleColorKeys {
    private static final Map<StyleColorValue, String> ourKeys = new LinkedHashMap<>();

    static {
        ourKeys.put(ComponentColors.LAYOUT, "Panel.background");
        ourKeys.put(ComponentColors.TEXT, "Label.foreground");
        ourKeys.put(ComponentColors.TEXT_FOREGROUND, "Label.foreground");
        ourKeys.put(ComponentColors.DISABLED_TEXT, "Label.disabledForeground");
        ourKeys.put(ComponentColors.BORDER, "Component.borderColor");

        ourKeys.put(ComponentColors.COMPONENT_BACKGROUND, "TextField.background");

        ourKeys.put(ComponentColors.SELECTION_BACKGROUND, "List.selectionBackground");
        ourKeys.put(ComponentColors.SELECTION_FOREGROUND, "List.selectionForeground");
        ourKeys.put(ComponentColors.SELECTION_INACTIVE_BACKGROUND, "List.selectionInactiveBackground");
        ourKeys.put(ComponentColors.SELECTION_INACTIVE_FOREGROUND, "List.selectionInactiveForeground");
        ourKeys.put(ComponentColors.MENU_SELECTION_BACKGROUND, "MenuItem.selectionBackground");

        ourKeys.put(ComponentColors.HOVER_BACKGROUND, "Button.hoverBackground");
        ourKeys.put(ComponentColors.FOCUS_COLOR, "Component.focusColor");

        ourKeys.put(ComponentColors.SEPARATOR, "Separator.foreground");
        ourKeys.put(ComponentColors.DISABLED_BORDER, "Component.disabledBorderColor");

        ourKeys.put(ComponentColors.LINK_FOREGROUND, "Component.linkColor");

        ourKeys.put(ComponentColors.ERROR_BORDER, "Component.error.borderColor");
        ourKeys.put(ComponentColors.WARNING_BORDER, "Component.warning.borderColor");
        ourKeys.put(ComponentColors.SUCCESS_BORDER, "Component.success.borderColor");

        ourKeys.put(ComponentColors.TOOLTIP_BACKGROUND, "ToolTip.background");
        ourKeys.put(ComponentColors.TOOLTIP_FOREGROUND, "ToolTip.foreground");

        ourKeys.put(ComponentColors.TOOL_WINDOW_BUTTON_HOVER_BACKGROUND, "ToolWindow.Button.hoverBackground");
        ourKeys.put(ComponentColors.TOOL_WINDOW_BUTTON_SELECTED_BACKGROUND, "ToolWindow.Button.selectedBackground");

        ourKeys.put(ComponentColors.SCROLL_BAR_THUMB, "ScrollBar.thumb");
        ourKeys.put(ComponentColors.SCROLL_BAR_HOVER_THUMB, "ScrollBar.hoverThumbColor");

        ourKeys.put(ComponentColors.TABBED_PANE_BACKGROUND, "TabbedPane.background");
        ourKeys.put(ComponentColors.TABBED_PANE_FOREGROUND, "TabbedPane.foreground");
        ourKeys.put(ComponentColors.TABBED_PANE_HOVER, "TabbedPane.hoverColor");
        ourKeys.put(ComponentColors.TABBED_PANE_UNDERLINE, "TabbedPane.underlineColor");
    }

    public static @Nullable String getKey(StyleColorValue colorValue) {
        return ourKeys.get(colorValue);
    }

    public static Map<StyleColorValue, String> getKeys() {
        return ourKeys;
    }
}
