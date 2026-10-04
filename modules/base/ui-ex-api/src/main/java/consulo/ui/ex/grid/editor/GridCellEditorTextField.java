// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.ex.grid.editor;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.editor.GridCellEditorFactory;
import consulo.ui.grid.editor.GridCellEditorFactory.ValueFormatterResult;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditGuard;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.util.lang.StringUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The text field of a cell editor. The grid has no widget for an editor: this class keeps what the field holds - its text and the
 * settings of the field - and describes the field a frontend shows with {@link #getPresentation()}. The frontend reports every
 * change of the text, which arrives at {@link #setText(String)} and is passed to the {@link #addDocumentListener document
 * listeners}.
 * <p/>
 * The field holds plain text, so it has:
 * <ul>
 * <li>no completion ({@code TextCompletionProvider}), no PSI code fragment, no language and no file with a charset and BOM - the
 * frontends show a plain text field;</li>
 * <li>no Enter, Tab and Up/Down actions - each frontend maps its keys to the grid itself.</li>
 * </ul>
 */
public class GridCellEditorTextField implements Disposable {
    private final DataGrid myGrid;
    private final boolean myMultiline;
    private final boolean myClear;
    private final List<Consumer<String>> myDocumentListeners = new CopyOnWriteArrayList<>();

    private String myText = "";
    private String myPlaceholder = "";
    private HorizontalAlignment myAlignment = HorizontalAlignment.LEFT;

    public GridCellEditorTextField(GridCellRequest<GridRow, GridColumn> request,
                                   boolean multiline,
                                   GridEditInitiator initiator,
                                   GridCellEditorFactory.ValueFormatter valueFormatter) {
        myGrid = (DataGrid) request.getGrid();
        myMultiline = multiline;
        myClear = initiator.isKey() && myGrid.isEditable();
        if (!myClear) {
            setText(valueFormatter, request);
        }
    }

    public void setText(GridCellEditorFactory.ValueFormatter valueFormatter, GridCellRequest<GridRow, GridColumn> request) {
        ValueFormatterResult result = valueFormatter.format();
        setText(result.text());
    }

    public String getText() {
        return myText;
    }

    /**
     * Replaces the text of the document, and notifies the document listeners when it
     * changed. A frontend calls it, through the cell editor, on every change of its field.
     */
    public void setText(String text) {
        if (myText.equals(text)) {
            return;
        }
        myText = text;
        for (Consumer<String> listener : myDocumentListeners) {
            listener.accept(text);
        }
    }

    /**
     * A listener of the text of the field. The listener gets the new text.
     */
    public void addDocumentListener(Consumer<String> listener) {
        myDocumentListeners.add(listener);
    }

    /**
     * The text shown while the field is empty, also while it has the focus.
     */
    public void setPlaceholder(String placeholder) {
        myPlaceholder = placeholder;
    }

    /**
     * The alignment of the text in the field.
     */
    public void setHorizontalAlignment(HorizontalAlignment alignment) {
        myAlignment = alignment;
    }

    public boolean isMultiline() {
        return myMultiline;
    }

    /**
     * The field: read-only when it is not {@link #isEditable() editable}, the read-only hint of the rejecting {@link GridEditGuard},
     * and the whole text selected when the field is editable and has one line.
     * <p/>
     * The text of an edit started by a typed key is not selected: the key goes into the emptied field, so nothing would stay
     * selected.
     */
    public GridCellEditorPresentation getPresentation() {
        boolean editable = isEditable();
        boolean selectAll = editable && !myClear && (!myMultiline || getLineCount() == 1);
        GridEditGuard guard = GridEditGuard.get(myGrid);
        LocalizeValue readOnlyHint = guard == null ? LocalizeValue.empty() : guard.getReasonText(myGrid);
        GridCellEditorPresentation.Kind kind =
            myMultiline ? GridCellEditorPresentation.Kind.MULTILINE_TEXT : GridCellEditorPresentation.Kind.TEXT;
        return new GridCellEditorPresentation(kind, myText, selectAll, !editable, readOnlyHint, myAlignment, myPlaceholder, List.of(), -1);
    }

    @Override
    public void dispose() {
    }

    protected boolean isEditable() {
        return myGrid.isEditable();
    }

    protected final DataGrid getGrid() {
        return myGrid;
    }

    /**
     * The number of lines of the text; an empty text is one line.
     */
    private int getLineCount() {
        return StringUtil.countNewLines(myText) + 1;
    }
}
