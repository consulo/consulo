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
    INFO_FOREGROUND,

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

    /**
     * The fill and the text of a run of text which matches a search.
     */
    SEARCH_MATCH_BACKGROUND,
    SEARCH_MATCH_FOREGROUND,

    ERROR_FOREGROUND,
    /**
     * The fill of a component which shows that its content is in error, such as a search box which found nothing.
     */
    ERROR_BACKGROUND,

    ERROR_BORDER,
    WARNING_BORDER,
    SUCCESS_BORDER,

    /**
     * The dot drawn over an icon to mark an error.
     */
    ICON_BADGE_ERROR,
    /**
     * The dot drawn over an icon to mark a warning.
     */
    ICON_BADGE_WARNING,
    /**
     * The dot drawn over an icon to mark something to look at.
     */
    ICON_BADGE_INFO,
    /**
     * The dot drawn over an icon to mark success or something running.
     */
    ICON_BADGE_SUCCESS,

    TOOLTIP_BACKGROUND,
    TOOLTIP_FOREGROUND,

    /**
     * The fill of the area editors are opened in, which shows while no editor is open.
     */
    EDITOR_AREA_BACKGROUND,
    /**
     * The text drawn on {@link #EDITOR_AREA_BACKGROUND} while no editor is open.
     */
    EDITOR_AREA_FOREGROUND,

    TOOL_WINDOW_BUTTON_HOVER_BACKGROUND,
    TOOL_WINDOW_BUTTON_SELECTED_BACKGROUND,

    SCROLL_BAR_THUMB,
    SCROLL_BAR_HOVER_THUMB,

    TABBED_LAYOUT_BACKGROUND,
    TABBED_LAYOUT_FOREGROUND,
    TABBED_LAYOUT_HOVER,
    TABBED_LAYOUT_UNDERLINE,
    /**
     * The fill of the selected tab, and of a selected tab whose window is not the active one. Both are what a
     * tab is drawn as - a tab painter fills the tab with them, so a frontend which cannot ask for them has
     * to invent a way to tell the selected tab apart.
     */
    TABBED_LAYOUT_SELECTED_BACKGROUND,
    TABBED_LAYOUT_INACTIVE_SELECTED_BACKGROUND,

    /**
     * The background of a grid cell with a modified value which is not submitted yet, and of the row header of its row.
     */
    GRID_CELL_MODIFIED_BACKGROUND,
    /**
     * The background of the cells and the row header of an inserted row which is not submitted yet.
     */
    GRID_CELL_INSERTED_BACKGROUND,
    /**
     * The background of the cells and the row header of a deleted row which is not submitted yet.
     */
    GRID_CELL_DELETED_BACKGROUND,
    /**
     * The background of a grid cell whose value failed to parse or to be submitted, and of the row header of its row.
     */
    GRID_CELL_ERROR_BACKGROUND,

    /**
     * A palette of ten text colors which tell grid columns apart: column {@code i} takes entry {@code i % 10 + 1}. A data source
     * turns it on with a color layer of its grid.
     */
    GRID_COLUMN_FOREGROUND_1,
    GRID_COLUMN_FOREGROUND_2,
    GRID_COLUMN_FOREGROUND_3,
    GRID_COLUMN_FOREGROUND_4,
    GRID_COLUMN_FOREGROUND_5,
    GRID_COLUMN_FOREGROUND_6,
    GRID_COLUMN_FOREGROUND_7,
    GRID_COLUMN_FOREGROUND_8,
    GRID_COLUMN_FOREGROUND_9,
    GRID_COLUMN_FOREGROUND_10,

    /**
     * The colors of the parameter hint of a call: its panel, its border, the line between two signatures, the
     * signature the call matches best, the text, the parameter at the caret, a parameter which no longer fits, and
     * the note under the signatures.
     */
    PARAMETER_INFO_BACKGROUND,
    PARAMETER_INFO_BORDER,
    PARAMETER_INFO_LINE_SEPARATOR,
    PARAMETER_INFO_CURRENT_OVERLOAD_BACKGROUND,
    PARAMETER_INFO_FOREGROUND,
    PARAMETER_INFO_CURRENT_PARAMETER_FOREGROUND,
    PARAMETER_INFO_DISABLED_FOREGROUND,
    PARAMETER_INFO_INFO_FOREGROUND,

    /**
     * The fill of a short message pointing at a tool window, by how serious it is.
     */
    NOTIFICATION_INFO_BACKGROUND,
    NOTIFICATION_WARNING_BACKGROUND,
    NOTIFICATION_ERROR_BACKGROUND,

    /**
     * The fill of a toast, by its style.
     */
    TOAST_INFO_BACKGROUND,
    TOAST_SUCCESS_BACKGROUND,
    TOAST_WARNING_BACKGROUND,
    TOAST_ERROR_BACKGROUND,

    /**
     * The fill of the allocated and of the used part of a memory gauge.
     */
    MEMORY_INDICATOR_ALLOCATED_BACKGROUND,
    MEMORY_INDICATOR_USED_BACKGROUND,

    /**
     * The text of the number of commits a branch has to pull from its remote, and of the number it has to push to it.
     */
    VCS_INCOMING_FOREGROUND,
    VCS_OUTGOING_FOREGROUND,

    /**
     * The text of a branch label, and the colour its background is mixed towards from the background under it.
     */
    VCS_REF_LABEL_FOREGROUND,
    VCS_REF_LABEL_BACKGROUND_BASE,

    /**
     * The fill of a reaction pill in its normal, hovered and pressed states, and the border of a selected one.
     */
    REACTION_BACKGROUND,
    REACTION_HOVERED_BACKGROUND,
    REACTION_PRESSED_BACKGROUND,
    REACTION_SELECTED_BORDER,

    /**
     * The ring of an avatar with an {@link consulo.ui.AvatarStyle#OUTLINE_SUCCESS}, {@link consulo.ui.AvatarStyle#OUTLINE_WARNING}
     * or {@link consulo.ui.AvatarStyle#OUTLINE_NEUTRAL} style.
     */
    AVATAR_OUTLINE_SUCCESS,
    AVATAR_OUTLINE_WARNING,
    AVATAR_OUTLINE_NEUTRAL
}
