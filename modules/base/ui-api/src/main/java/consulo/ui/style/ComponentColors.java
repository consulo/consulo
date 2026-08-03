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
package consulo.ui.style;

/**
 * @author VISTALL
 * @since 2017-09-15
 */
public enum ComponentColors implements StyleColorValue {
    BORDER,
    @Deprecated
    // Use Use TEXT_FOREGROUND
    TEXT,
    TEXT_FOREGROUND,
    LAYOUT,
    DISABLED_TEXT,

    COMPONENT_BACKGROUND,

    SELECTION_BACKGROUND,
    SELECTION_FOREGROUND,
    SELECTION_INACTIVE_BACKGROUND,
    SELECTION_INACTIVE_FOREGROUND,
    MENU_SELECTION_BACKGROUND,

    HOVER_BACKGROUND,
    FOCUS_COLOR,

    SEPARATOR,
    DISABLED_BORDER,

    LINK_FOREGROUND,

    ERROR_BORDER,
    WARNING_BORDER,
    SUCCESS_BORDER,

    TOOLTIP_BACKGROUND,
    TOOLTIP_FOREGROUND,

    TOOL_WINDOW_BUTTON_HOVER_BACKGROUND,
    TOOL_WINDOW_BUTTON_SELECTED_BACKGROUND,

    SCROLL_BAR_THUMB,
    SCROLL_BAR_HOVER_THUMB,

    TABBED_PANE_BACKGROUND,
    TABBED_PANE_FOREGROUND,
    TABBED_PANE_HOVER,
    TABBED_PANE_UNDERLINE
}
