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
package consulo.ui.grid.editor;

import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;

import java.util.List;

/**
 * What a frontend shows inside the cell for a {@link GridCellEditor}. Each frontend builds a native widget of the {@link #kind()}
 * in the cell, reports every change of its text with {@link GridCellEditor#setText} and a chosen option with
 * {@link GridCellEditor#selectOption}.
 *
 * @param kind           which widget to show
 * @param text           the text the field starts with ({@link Kind#TEXT}, {@link Kind#MULTILINE_TEXT}); empty when the edit was
 *                       started by a typed key in an editable grid, which the frontend then puts into the field
 * @param selectAll      select the whole text when the field opens; otherwise the caret is at offset 0 (the whole text is
 *                       selected when the field is editable and single-line)
 * @param readOnly       the field shows the value but refuses changes: the grid is not editable or the factory's
 *                       {@link GridCellEditorFactory.IsEditableChecker} refused the value
 * @param readOnlyHint   what to tell the user who tries to change a read-only field - the reason of the rejecting
 *                       {@link GridEditGuard}, or empty
 * @param alignment      the alignment of the text, {@link HorizontalAlignment#RIGHT} for numbers
 * @param placeholder    the text shown while the field is empty, also while it has the focus - the display name of the null
 *                       value ({@code <null>}); empty for none
 * @param options        the labels of the choices of a {@link Kind#LIST}, in order; empty for the other kinds
 * @param selectedOption the index of the choice to highlight first in {@link #options()}, or {@code -1}
 * @since 2026-10-04
 */
public record GridCellEditorPresentation(Kind kind,
                                         String text,
                                         boolean selectAll,
                                         boolean readOnly,
                                         LocalizeValue readOnlyHint,
                                         HorizontalAlignment alignment,
                                         String placeholder,
                                         List<String> options,
                                         int selectedOption) {
    public enum Kind {
        /**
         * A single-line text field without a frame.
         */
        TEXT,
        /**
         * A multi-line text field. Enter commits, Ctrl/Cmd+Enter inserts a new line.
         */
        MULTILINE_TEXT,
        /**
         * A list of {@link #options()} under the cell, with speed search where the frontend has it. Choosing an option calls
         * {@link GridCellEditor#selectOption} and then commits; closing the list without a choice cancels.
         */
        LIST,
        /**
         * The value is already decided when the editor is created - for example a boolean set by a typed key, or toggled in
         * check box mode. Nothing is shown: the grid commits the editor at once.
         */
        NONE
    }

    public GridCellEditorPresentation {
        options = List.copyOf(options);
    }

    public static GridCellEditorPresentation text(String text) {
        return new GridCellEditorPresentation(Kind.TEXT, text, false, false, LocalizeValue.empty(), HorizontalAlignment.LEFT, "",
            List.of(), -1);
    }

    public static GridCellEditorPresentation multilineText(String text) {
        return new GridCellEditorPresentation(Kind.MULTILINE_TEXT, text, false, false, LocalizeValue.empty(), HorizontalAlignment.LEFT,
            "", List.of(), -1);
    }

    public static GridCellEditorPresentation list(String text, List<String> options, int selectedOption) {
        return new GridCellEditorPresentation(Kind.LIST, text, false, false, LocalizeValue.empty(), HorizontalAlignment.LEFT, "",
            options, selectedOption);
    }

    public static GridCellEditorPresentation none(String text) {
        return new GridCellEditorPresentation(Kind.NONE, text, false, false, LocalizeValue.empty(), HorizontalAlignment.LEFT, "",
            List.of(), -1);
    }

    public boolean isTextKind() {
        return kind == Kind.TEXT || kind == Kind.MULTILINE_TEXT;
    }

    public GridCellEditorPresentation withText(String text) {
        return new GridCellEditorPresentation(kind, text, selectAll, readOnly, readOnlyHint, alignment, placeholder, options,
            selectedOption);
    }

    public GridCellEditorPresentation withSelectAll(boolean selectAll) {
        return new GridCellEditorPresentation(kind, text, selectAll, readOnly, readOnlyHint, alignment, placeholder, options,
            selectedOption);
    }

    public GridCellEditorPresentation withReadOnly(boolean readOnly, LocalizeValue readOnlyHint) {
        return new GridCellEditorPresentation(kind, text, selectAll, readOnly, readOnlyHint, alignment, placeholder, options,
            selectedOption);
    }

    public GridCellEditorPresentation withAlignment(HorizontalAlignment alignment) {
        return new GridCellEditorPresentation(kind, text, selectAll, readOnly, readOnlyHint, alignment, placeholder, options,
            selectedOption);
    }

    public GridCellEditorPresentation withPlaceholder(String placeholder) {
        return new GridCellEditorPresentation(kind, text, selectAll, readOnly, readOnlyHint, alignment, placeholder, options,
            selectedOption);
    }
}
