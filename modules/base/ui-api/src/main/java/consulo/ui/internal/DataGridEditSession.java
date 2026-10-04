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
package consulo.ui.internal;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ModelIndexSet;
import consulo.ui.grid.editor.GridCellEditor;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.grid.editor.UnparsedValue;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * One open cell edit of a {@link DataGridController} - everything of the edit which is not the native widget.
 * <p/>
 * The controller creates it in {@link DataGridController#startEditing} and owns its {@link GridCellEditor}; the frontend
 * shows a native widget for {@link #getPresentation()} inside the cell, reports every change of the widget's text with
 * {@link #setText} (or a choice in a list with {@link #selectOption}), and ends the edit with
 * {@link DataGridController#commitEditing()} or {@link DataGridController#discardEditing()}. Every method is called on the
 * UI thread.
 *
 * @since 2026-10-04
 */
public final class DataGridEditSession {
    private final DataGridController myController;
    private final ModelIndex<GridRow> myRowIdx;
    private final ModelIndex<GridColumn> myColumnIdx;
    private final GridCellEditor myEditor;
    private final GridCellEditorPresentation myPresentation;
    private final String myTypedText;
    private final ModelIndexSet<GridRow> myTargetRows;
    private final ModelIndexSet<GridColumn> myTargetColumns;
    private final int[] mySortedTargetRows;
    private final int[] mySortedTargetColumns;
    private final boolean myShouldMoveFocus;
    private final boolean myAllowUniqueMultiEdit;
    private final boolean myHosted;

    private UnparsedValue.@Nullable ParsingError myError;
    private boolean myTextChanged;
    private boolean myHasCommonValue;
    private @Nullable Object myCommonValue;
    private boolean myWaitingForAnswer;
    private boolean myIgnoreUnsubmittedChanges;

    DataGridEditSession(DataGridController controller,
                        ModelIndex<GridRow> rowIdx,
                        ModelIndex<GridColumn> columnIdx,
                        GridCellEditor editor,
                        GridEditInitiator initiator,
                        ModelIndexSet<GridRow> targetRows,
                        ModelIndexSet<GridColumn> targetColumns,
                        boolean shouldMoveFocus,
                        boolean allowUniqueMultiEdit) {
        myController = controller;
        myRowIdx = rowIdx;
        myColumnIdx = columnIdx;
        myEditor = editor;
        myPresentation = editor.getPresentation();
        myTypedText = initiator.isKey() ? initiator.typedText() : "";
        myTargetRows = targetRows;
        myTargetColumns = targetColumns;
        mySortedTargetRows = sorted(targetRows.asArray());
        mySortedTargetColumns = sorted(targetColumns.asArray());
        myShouldMoveFocus = shouldMoveFocus;
        myAllowUniqueMultiEdit = allowUniqueMultiEdit;
        myHosted = myPresentation.kind() != GridCellEditorPresentation.Kind.NONE;
    }

    /**
     * The view row of the edited cell, or {@code -1} when it is not shown any more.
     */
    public int getViewRow() {
        return myController.toViewRow(myRowIdx);
    }

    /**
     * The view column of the edited cell, or {@code -1} when it is not shown any more.
     */
    public int getViewColumn() {
        return myController.toViewColumn(myColumnIdx);
    }

    public ModelIndex<GridRow> getRowIdx() {
        return myRowIdx;
    }

    public ModelIndex<GridColumn> getColumnIdx() {
        return myColumnIdx;
    }

    /**
     * What the frontend shows in the cell. Once the text was changed, the text of the presentation is the current text of the
     * editor - so a frontend which opens its widget again for the same edit shows what the user typed.
     */
    public GridCellEditorPresentation getPresentation() {
        return myTextChanged ? myPresentation.withText(myEditor.getText()) : myPresentation;
    }

    /**
     * The text typed to start the edit, which the frontend appends to the text of {@link #getPresentation()}; empty when the edit
     * was not started by a typed key, once the text was changed, and when the presentation is no editable text field (a list, or a
     * read-only field, which ignores the key). A frontend whose native table forwards the typed key into the field by itself
     * ignores it.
     */
    public String getTypedText() {
        if (myTextChanged || !myPresentation.isTextKind() || myPresentation.readOnly()) {
            return "";
        }
        return myTypedText;
    }

    public String getText() {
        return myEditor.getText();
    }

    /**
     * The text of the hosted field changed: the error goes away, and the editor fires the new value, which the grid shows in every
     * cell of a multi-cell edit.
     */
    @RequiredUIAccess
    public void setText(String text) {
        myTextChanged = true;
        myError = null;
        myEditor.setText(text);
    }

    /**
     * The user chose the option with this index of {@link GridCellEditorPresentation#options()}; the frontend commits the edit
     * afterwards.
     */
    @RequiredUIAccess
    public void selectOption(int index) {
        myTextChanged = true;
        myError = null;
        myEditor.selectOption(index);
    }

    /**
     * Why the last {@link DataGridController#commitEditing()} refused, or {@code null}. The frontend shows it as a red outline,
     * the text from the offset to the end highlighted, and a tooltip with the message.
     */
    public UnparsedValue.@Nullable ParsingError getError() {
        return myError;
    }

    /**
     * The grid asked the user whether to ignore unsubmitted changes, and the answer has not come yet. A commit refused while this
     * is true is not a failure: the edit goes on after the answer. A frontend which discards a refused commit when the focus
     * leaves the grid keeps the editor open while this is true - the question takes the focus.
     */
    public boolean isWaitingForAnswer() {
        return myWaitingForAnswer;
    }

    /**
     * The editor of the edit, which the controller owns.
     */
    public GridCellEditor getEditor() {
        return myEditor;
    }

    public boolean isEditingCell(ModelIndex<GridRow> rowIdx, ModelIndex<GridColumn> columnIdx) {
        return myRowIdx.equals(rowIdx) && myColumnIdx.equals(columnIdx);
    }

    /**
     * Whether the commit moves the selection to the next cell.
     */
    boolean shouldMoveFocus() {
        return myShouldMoveFocus && myEditor.shouldMoveFocus();
    }

    /**
     * Whether the editor factory lets a multi-cell edit write a unique column.
     */
    boolean allowsUniqueMultiEdit() {
        return myAllowUniqueMultiEdit;
    }

    /**
     * {@code false} when the editor decided its value when it was created ({@link GridCellEditorPresentation.Kind#NONE}): no
     * native widget shows the edit, so the controller commits it itself.
     */
    boolean isHosted() {
        return myHosted;
    }

    /**
     * The cells a multi-cell edit writes: the selection when the edit started, if the edited cell was in it, otherwise the edited
     * cell. They are recorded when the edit starts, because a frontend may move the selection while the editor is open.
     */
    ModelIndexSet<GridRow> getTargetRows() {
        return myTargetRows;
    }

    ModelIndexSet<GridColumn> getTargetColumns() {
        return myTargetColumns;
    }

    boolean isMultiCell() {
        return mySortedTargetRows.length * mySortedTargetColumns.length > 1;
    }

    boolean isTarget(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        return Arrays.binarySearch(mySortedTargetRows, row.asInteger()) >= 0
            && Arrays.binarySearch(mySortedTargetColumns, column.asInteger()) >= 0;
    }

    /**
     * The editor fired a value; the last one, {@link #getCommonValue()}, shows in the cells of a multi-cell edit.
     */
    boolean hasCommonValue() {
        return myHasCommonValue;
    }

    @Nullable
    Object getCommonValue() {
        return myCommonValue;
    }

    void setCommonValue(@Nullable Object value) {
        myHasCommonValue = true;
        myCommonValue = value;
    }

    void setError(UnparsedValue.@Nullable ParsingError error) {
        myError = error;
    }

    void setWaitingForAnswer(boolean waiting) {
        myWaitingForAnswer = waiting;
    }

    /**
     * The user answered Yes to "Ignore unsubmitted changes?": the next commit does not ask again.
     */
    void setIgnoreUnsubmittedChanges(boolean ignore) {
        myIgnoreUnsubmittedChanges = ignore;
    }

    boolean isIgnoreUnsubmittedChanges() {
        return myIgnoreUnsubmittedChanges;
    }

    private static int[] sorted(int[] values) {
        int[] result = values.clone();
        Arrays.sort(result);
        return result;
    }
}
