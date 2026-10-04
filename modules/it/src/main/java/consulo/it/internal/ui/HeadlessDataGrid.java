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
package consulo.it.internal.ui;

import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridAppearance;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridHitArea;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.internal.DataGridController;
import consulo.ui.internal.DataGridControllerOwner;
import consulo.ui.internal.DataGridEditSession;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/**
 * Dummy-but-creatable headless {@link DataGrid}. Every grid member is answered by its {@link DataGridController}; the
 * "native table" is plain state - the header texts, the row count, the selection the controller last pushed to its
 * view and the cell editor it shows - so an integration test can drive the grid like a user (select cells, click a header,
 * edit a cell) and check what a real frontend would show.
 * <p>
 * The cell editor follows the contract every frontend follows: a gesture of the user opens it through
 * {@link DataGridController#startEditing}, every change of its text goes to {@link DataGridEditSession#setText}, and Enter, a
 * click into another cell or Escape end it with {@link DataGridController#commitEditing()} or
 * {@link DataGridController#discardEditing()}. The {@link DataGrid} API reaches it through the view
 * ({@link DataGridController.View#editCellAt}, {@link DataGridController.View#stopCellEditor},
 * {@link DataGridController.View#cancelCellEditor}).
 * <p>
 * Like every {@code DataGrid} it must be created on the UI thread, the headless one included.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessDataGrid extends HeadlessComponentBase implements DataGridControllerOwner {
    private final DataGridController myController;
    private final Component myToolbar;

    private List<String> myShownHeaderTexts = List.of();
    private int myShownRowCount;
    private int[] myShownSelectedRows = new int[0];
    private int[] myShownSelectedColumns = new int[0];

    // the native cell editor: the edit it shows, the text of its field with the selection in it, and the highlighted option of a list
    private @Nullable DataGridEditSession myEditorSession;
    private String myEditorText = "";
    private int myEditorSelectionStart;
    private int myEditorSelectionEnd;
    private int myEditorOption = -1;
    private String myEditorSpeedSearch = "";

    private int myStructureChangedCount;
    private int myRowsChangedCount;
    private int myCellsChangedCount;
    private int myHeadersChangedCount;
    private int mySelectionChangedCount;
    private int myScrolledViewRow = -1;
    private int myScrolledViewColumn = -1;

    @RequiredUIAccess
    public HeadlessDataGrid(GridDataHookUp<GridRow, GridColumn> hookUp, BiConsumer<DataGrid, DataGridAppearance> configurator) {
        myController = new DataGridController(this, hookUp);
        configurator.accept(this, myController.getAppearance());

        myToolbar = myController.getToolbar();
        if (myToolbar instanceof HeadlessComponentBase toolbar) {
            toolbar.setParentComponent(this);
        }
        rebuildHeaders();
        myShownRowCount = myController.getViewRowCount();
        syncSelection();

        myController.setView(new HeadlessView());
        myController.start();
    }

    @Override
    public DataGridController getController() {
        return myController;
    }

    /**
     * Nothing holds the keyboard focus without a frontend.
     */
    @Override
    public void focus() {
    }

    /**
     * The paging bar of the controller, which a real frontend shows above its table.
     */
    public Component getToolbar() {
        return myToolbar;
    }

    /**
     * The column headers as the view last built them, sort markers included.
     */
    public List<String> getShownHeaderTexts() {
        return myShownHeaderTexts;
    }

    /**
     * The number of rows as the view last loaded them.
     */
    public int getShownRowCount() {
        return myShownRowCount;
    }

    /**
     * The selected view rows of the view, whether the user or the grid selected them.
     */
    public int[] getShownSelectedRows() {
        return myShownSelectedRows.clone();
    }

    /**
     * The selected view columns of the view, whether the user or the grid selected them.
     */
    public int[] getShownSelectedColumns() {
        return myShownSelectedColumns.clone();
    }

    /**
     * Renders a cell the way a frontend's cell renderer does, a {@code null} in the null style included, on the background of
     * its pending change ({@link HeadlessTextItemPresentation#getBackgroundColor()}: a theme color such as a {@link ComponentColors}
     * entry, which a frontend paints through its style, or any other color, painted as is).
     */
    @RequiredUIAccess
    public HeadlessTextItemPresentation renderCell(int viewRow, int viewColumn) {
        HeadlessTextItemPresentation presentation = new HeadlessTextItemPresentation();
        myController.renderCell(presentation, viewRow, viewColumn);
        return presentation;
    }

    @RequiredUIAccess
    public String renderCellText(int viewRow, int viewColumn) {
        return renderCell(viewRow, viewColumn).getText();
    }

    /**
     * @return the text of the row header, or {@code null} when the grid does not show row numbers
     */
    public @Nullable String getRowHeaderText(int viewRow) {
        return myController.isShowRowNumbers() ? myController.getRowNumberText(viewRow) : null;
    }

    /**
     * @return the background the row header of a row with pending changes shows, or {@code null} when it keeps its background
     * or the grid does not show row numbers
     */
    public @Nullable ColorValue getRowHeaderBackground(int viewRow) {
        return myController.isShowRowNumbers() ? myController.getRowHeaderBackground(viewRow) : null;
    }

    /**
     * Selects cells as the user would in a native table, and reports the selection to the controller.
     * <p>
     * While a cell editor is open this is a click into the table: the editor commits first, and when it refuses its text
     * (see {@link DataGridEditSession#getError()}) it stays open and the selection does not change.
     */
    @RequiredUIAccess
    public void userSelect(int[] viewRows, int[] viewColumns) {
        if (!stopEditor()) {
            return;
        }
        selectInTable(viewRows, viewColumns);
    }

    /**
     * Selects whole rows as the user would in a native table which only selects rows.
     */
    @RequiredUIAccess
    public void userSelectRows(int... viewRows) {
        userSelect(viewRows, IntStream.range(0, myController.getViewColumnCount()).toArray());
    }

    /**
     * Clicks the header of a column, as on every frontend: the column is selected - the open editor commits first, and the selection
     * stays when it refuses.
     *
     * @param extend the interval modifier (Shift)
     * @param toggle the exclusive modifier (Ctrl, or Meta on macOS)
     */
    @RequiredUIAccess
    public void clickHeader(int viewColumn, boolean extend, boolean toggle) {
        myController.onColumnHeaderClicked(viewColumn, extend, toggle);
    }

    /**
     * Clicks the sort marker of a column header; {@code additive} stands for the modifier which keeps the other sorted columns.
     */
    @RequiredUIAccess
    public void clickSortMarker(int viewColumn, boolean additive) {
        myController.onColumnHeaderSortClicked(viewColumn, additive);
    }

    /**
     * Asks for the context menu of a part of the grid, the way a right click does: the controller commits the open edit and selects
     * what the menu is opened on, then the {@link ContextMenuEvent} of the grid fires.
     *
     * @return whether the menu opened: {@code false} while the open edit could not be committed
     */
    @RequiredUIAccess
    public boolean requestContextMenu(GridHitArea area, int viewRow, int viewColumn) {
        if (!myController.onContextMenuRequested(area, viewRow, viewColumn)) {
            return false;
        }
        Point2D position = new Point2D(0, 0);
        MouseInputDetails details =
            new MouseInputDetails(position, position, EnumSet.noneOf(ModifiedInputDetails.Modifier.class), MouseInputDetails.MouseButton.RIGHT);
        getListenerDispatcher(ContextMenuEvent.class).onEvent(new ContextMenuEvent(this, details));
        return true;
    }

    /**
     * Starts editing a cell the way the user does in a native table.
     * <ul>
     * <li>{@code typed == null} - a double click: the click commits an open editor first (a refused commit keeps it open, and
     * nothing else happens), selects the cell, and opens its editor ({@link GridEditInitiator#MOUSE}).</li>
     * <li>otherwise - the key which types {@code typed}: it goes to the lead cell of the native table, so a cell outside the
     * selection is clicked first, and a cell inside it keeps the selection - the edit then writes into every selected cell. The
     * editor opens with the typed text in its field ({@link GridEditInitiator#typed}).</li>
     * </ul>
     * A cell without an editor does not open one. An editor which decides its value when it opens (a boolean set by a typed key,
     * or toggled in check box mode) commits at once and shows nothing.
     *
     * @return {@code true} when the cell editor is open now
     */
    @RequiredUIAccess
    public boolean userEdit(int viewRow, int viewColumn, @Nullable String typed) {
        if (typed == null) {
            // the first press of the double click
            if (!stopEditor()) {
                return false;
            }
            selectInTable(new int[]{viewRow}, new int[]{viewColumn});
            return openEditor(viewRow, viewColumn, GridEditInitiator.MOUSE);
        }

        if (typed.isEmpty()) {
            throw new IllegalArgumentException("A typed key types at least one character");
        }
        if (!contains(myShownSelectedRows, viewRow) || !contains(myShownSelectedColumns, viewColumn)) {
            if (!stopEditor()) {
                return false;
            }
            selectInTable(new int[]{viewRow}, new int[]{viewColumn});
        }
        return openEditor(viewRow, viewColumn, GridEditInitiator.typed(typed));
    }

    /**
     * Types into the open cell editor, the way a key or a paste does.
     * <ul>
     * <li>a text field replaces its selected text - the whole text right after it opened with
     * {@link GridCellEditorPresentation#selectAll()} - or inserts at the caret, and reports the new text to the edit; a read-only
     * field ignores it;</li>
     * <li>a list searches its options: the first option which starts with what was typed since it opened, ignoring the case, is
     * highlighted, and Enter chooses it.</li>
     * </ul>
     *
     * @throws IllegalStateException when no cell editor is open - the user cannot type into one
     */
    @RequiredUIAccess
    public void userType(String text) {
        DataGridEditSession session = getShownEditSession();
        if (session == null) {
            throw new IllegalStateException("No cell editor is open");
        }
        GridCellEditorPresentation presentation = session.getPresentation();
        if (presentation.kind() == GridCellEditorPresentation.Kind.LIST) {
            speedSearch(presentation.options(), text);
        }
        else if (!presentation.readOnly()) {
            typeIntoField(session, text);
        }
    }

    /**
     * Selects the whole text in the field of the open cell editor (Ctrl/Cmd+A), so that the next {@link #userType} replaces it.
     *
     * @throws IllegalStateException when no cell editor is open
     */
    @RequiredUIAccess
    public void userSelectAllInEditor() {
        if (getShownEditSession() == null) {
            throw new IllegalStateException("No cell editor is open");
        }
        myEditorSelectionStart = 0;
        myEditorSelectionEnd = myEditorText.length();
    }

    /**
     * Presses Enter.
     * <ul>
     * <li>with a cell editor open, the editor commits ({@link DataGridController#commitEditing()}); a list chooses its highlighted
     * option first ({@link DataGridEditSession#selectOption}), and without a highlighted option nothing happens. When the commit is
     * refused the editor stays open, with the reason in {@link DataGridEditSession#getError()}, or while the grid waits for the
     * user's answer ({@link DataGridEditSession#isWaitingForAnswer()});</li>
     * <li>otherwise the editor of the lead cell opens ({@link DataGridController#editSelectedCell()}), as Enter and F2 do in every
     * frontend when cells are edited; a grid without cell editing leaves Enter to the native table, which this one does not
     * emulate.</li>
     * </ul>
     *
     * @return {@code false} when the open editor stays open, otherwise {@code true}
     */
    @RequiredUIAccess
    public boolean userPressEnter() {
        DataGridEditSession session = getShownEditSession();
        if (session == null) {
            if (myController.isCellEditingAllowed()) {
                myController.editSelectedCell();
            }
            return true;
        }

        GridCellEditorPresentation presentation = session.getPresentation();
        if (presentation.kind() == GridCellEditorPresentation.Kind.LIST) {
            if (myEditorOption < 0 || myEditorOption >= presentation.options().size()) {
                // nothing is highlighted, nothing is chosen
                return false;
            }
            session.selectOption(myEditorOption);
        }
        return stopEditor();
    }

    /**
     * Presses Escape: the open cell editor closes without writing its value ({@link DataGridController#discardEditing()}).
     * Without an open editor nothing happens.
     */
    @RequiredUIAccess
    public void userPressEscape() {
        if (getShownEditSession() != null) {
            cancelEditor();
        }
    }

    /**
     * The open edit of the controller, which the cell editor shows - its presentation, its text, and the error of the last refused
     * commit.
     */
    public @Nullable DataGridEditSession getEditSession() {
        return myController.getEditSession();
    }

    /**
     * What the open cell editor shows: the text of its field, or the highlighted option of a list (empty when none is).
     *
     * @return the shown text, or {@code null} when no cell editor is open
     */
    public @Nullable String getShownEditorText() {
        DataGridEditSession session = myEditorSession;
        if (session == null) {
            return null;
        }
        GridCellEditorPresentation presentation = session.getPresentation();
        if (presentation.kind() == GridCellEditorPresentation.Kind.LIST) {
            List<String> options = presentation.options();
            return myEditorOption >= 0 && myEditorOption < options.size() ? options.get(myEditorOption) : "";
        }
        return myEditorText;
    }

    public int getStructureChangedCount() {
        return myStructureChangedCount;
    }

    public int getRowsChangedCount() {
        return myRowsChangedCount;
    }

    public int getCellsChangedCount() {
        return myCellsChangedCount;
    }

    public int getHeadersChangedCount() {
        return myHeadersChangedCount;
    }

    public int getSelectionChangedCount() {
        return mySelectionChangedCount;
    }

    /**
     * @return the view row the grid last scrolled to, or {@code -1}
     */
    public int getScrolledViewRow() {
        return myScrolledViewRow;
    }

    /**
     * @return the view column the grid last scrolled to, or {@code -1}
     */
    public int getScrolledViewColumn() {
        return myScrolledViewColumn;
    }

    @Override
    public void dispose() {
        // the native editor goes away with the table; the controller disposes the edit
        closeEditor();
        Disposer.dispose(myController);
    }

    private void rebuildHeaders() {
        int count = myController.getViewColumnCount();
        List<String> headers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            headers.add(myController.getColumnHeaderText(i));
        }
        myShownHeaderTexts = List.copyOf(headers);
    }

    /**
     * Takes the selection the controller holds without reporting it back as a user change.
     */
    private void syncSelection() {
        myShownSelectedRows = myController.getSelectedViewRows();
        myShownSelectedColumns = myController.getSelectedViewColumns();
    }

    @RequiredUIAccess
    private void selectInTable(int[] viewRows, int[] viewColumns) {
        int[] rows = sortedDistinct(viewRows);
        int[] columns = sortedDistinct(viewColumns);
        myShownSelectedRows = rows;
        myShownSelectedColumns = columns;
        myController.onNativeSelectionChanged(rows, columns, false);
    }

    /**
     * The edit the native editor shows. The controller closes the native editor before it closes an edit the editor shows
     * ({@link DataGridController.View#stopCellEditor}, {@link DataGridController.View#cancelCellEditor}); an edit closed or
     * replaced behind its back would leave a real frontend with an editor of a dead edit, so the gesture fails instead.
     */
    private @Nullable DataGridEditSession getShownEditSession() {
        DataGridEditSession session = myEditorSession;
        if (session != null && myController.getEditSession() != session) {
            throw new IllegalStateException("The cell editor at " + session.getViewRow() + ":" + session.getViewColumn()
                + " shows an edit which the grid closed without closing the editor");
        }
        return session;
    }

    /**
     * Opens the editor of a cell the way a native table does: an open editor is stopped first, and a refused commit keeps it
     * open; then the editor of the cell asks the controller for its edit, and shows it.
     */
    @RequiredUIAccess
    private boolean openEditor(int viewRow, int viewColumn, GridEditInitiator initiator) {
        if (!stopEditor()) {
            return false;
        }
        DataGridEditSession session = myController.startEditing(viewRow, viewColumn, initiator);
        if (session == null) {
            return false;
        }

        GridCellEditorPresentation presentation = session.getPresentation();
        myEditorSession = session;
        myEditorText = presentation.text();
        myEditorSelectionStart = 0;
        myEditorSelectionEnd = presentation.selectAll() ? myEditorText.length() : 0;
        myEditorOption = presentation.selectedOption();
        myEditorSpeedSearch = "";

        String typedText = session.getTypedText();
        if (!typedText.isEmpty()) {
            // the key which started the edit goes into the field, after its text
            myEditorSelectionStart = myEditorText.length();
            myEditorSelectionEnd = myEditorText.length();
            typeIntoField(session, typedText);
        }
        return true;
    }

    /**
     * Stops the open editor: it commits, and closes when the commit is accepted.
     *
     * @return {@code false} when the commit is refused and the editor stays open
     */
    @RequiredUIAccess
    private boolean stopEditor() {
        if (myEditorSession == null) {
            return true;
        }
        if (!myController.commitEditing()) {
            return false;
        }
        closeEditor();
        return true;
    }

    /**
     * Cancels the open editor: it closes without a commit.
     */
    @RequiredUIAccess
    private void cancelEditor() {
        if (myEditorSession == null) {
            return;
        }
        myController.discardEditing();
        closeEditor();
    }

    private void closeEditor() {
        myEditorSession = null;
        myEditorText = "";
        myEditorSelectionStart = 0;
        myEditorSelectionEnd = 0;
        myEditorOption = -1;
        myEditorSpeedSearch = "";
    }

    /**
     * The text replaces the selected text of the field, the caret goes after it, and the edit gets the new text of the field.
     */
    @RequiredUIAccess
    private void typeIntoField(DataGridEditSession session, String text) {
        int start = Math.min(myEditorSelectionStart, myEditorSelectionEnd);
        int end = Math.max(myEditorSelectionStart, myEditorSelectionEnd);
        myEditorText = myEditorText.substring(0, start) + text + myEditorText.substring(end);
        myEditorSelectionStart = start + text.length();
        myEditorSelectionEnd = myEditorSelectionStart;
        session.setText(myEditorText);
    }

    /**
     * The speed search of a list: the first option which starts with the typed pattern is highlighted; without one the highlight
     * stays.
     */
    private void speedSearch(List<String> options, String text) {
        myEditorSpeedSearch = myEditorSpeedSearch + text;
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (option.regionMatches(true, 0, myEditorSpeedSearch, 0, myEditorSpeedSearch.length())) {
                myEditorOption = i;
                return;
            }
        }
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    private static int[] sortedDistinct(int[] values) {
        return Arrays.stream(values).distinct().sorted().toArray();
    }

    private final class HeadlessView implements DataGridController.View {
        @Override
        @RequiredUIAccess
        public void structureChanged() {
            myStructureChangedCount++;
            rebuildHeaders();
            // a native table drops its selection when its columns are rebuilt; the controller has already reset its own
            syncSelection();
        }

        @Override
        @RequiredUIAccess
        public void rowsChanged() {
            myRowsChangedCount++;
            myShownRowCount = myController.getViewRowCount();
        }

        @Override
        @RequiredUIAccess
        public void cellsChanged(int firstViewRow, int lastViewRow) {
            myCellsChangedCount++;
        }

        @Override
        @RequiredUIAccess
        public void headersChanged() {
            myHeadersChangedCount++;
            rebuildHeaders();
        }

        @Override
        @RequiredUIAccess
        public void selectionChanged() {
            mySelectionChangedCount++;
            syncSelection();
        }

        @Override
        @RequiredUIAccess
        public void scrollToCell(int viewRow, int viewColumn) {
            myScrolledViewRow = viewRow;
            myScrolledViewColumn = viewColumn;
        }

        @Override
        @RequiredUIAccess
        public void editCellAt(int viewRow, int viewColumn, GridEditInitiator initiator) {
            openEditor(viewRow, viewColumn, initiator);
        }

        @Override
        @RequiredUIAccess
        public boolean stopCellEditor() {
            return stopEditor();
        }

        @Override
        @RequiredUIAccess
        public void cancelCellEditor() {
            cancelEditor();
        }
    }
}
