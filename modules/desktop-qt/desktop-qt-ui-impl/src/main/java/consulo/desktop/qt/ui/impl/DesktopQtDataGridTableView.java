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
package consulo.desktop.qt.ui.impl;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.internal.DataGridController;
import consulo.ui.internal.DataGridEditSession;
import consulo.ui.style.ComponentColors;
import io.qt.core.QAbstractItemModel;
import io.qt.core.QEvent;
import io.qt.core.QItemSelectionModel;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QContextMenuEvent;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QHideEvent;
import io.qt.gui.QInputMethodEvent;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QKeySequence;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QAbstractItemDelegate;
import io.qt.widgets.QHeaderView;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionHeader;
import io.qt.widgets.QTableView;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The table of {@link DesktopQtDataGridImpl}: a {@link QTableView} whose cell editors the {@link DataGridController} opens, commits
 * and cancels.
 * <p/>
 * A cell editor opens only through {@link #editCell}: the edit triggers of qt stay off, the gestures of the user are mapped here
 * (Enter and F2, a typed key, a double click), and the {@link DataGridController} opens the edit first - {@link #edit} shows the
 * widget of {@link DesktopQtDataGridItemDelegate} for it. Every way qt ends an edit by itself - the current cell moving away,
 * {@link #commitData}, {@link #closeEditor} - asks the controller instead: a commit it refuses keeps the editor open.
 * <p/>
 * The column header is mapped to the controller too: a press on a header selects the whole column, a click on the sort area at its
 * right edge ({@link #getSortAreaWidth()}) sorts, and a column dragged to another place is moved by the controller, or put back.
 *
 * @since 2026-10-04
 */
final class DesktopQtDataGridTableView extends QTableView {
    /**
     * The widest sort marker {@link DataGridController#getColumnHeaderText} gives a column: the room the sort area of a column header
     * keeps for it, so sorting a column does not take room from its name.
     */
    static final String SORT_MARKER_SAMPLE = " ▼9";
    /**
     * The marker a sortable column shows faintly in its sort area while the pointer is over its header: the order a first click sorts
     * in.
     */
    private static final String SORT_HINT = "▲";

    private final DataGridController myController;
    private final DesktopQtDataGridItemDelegate myDelegate;
    private final ColumnHeaderView myColumnHeader;
    private final QWidget myHorizontalHeaderViewport;
    private final QWidget myVerticalHeaderViewport;

    /**
     * Set while {@link #editCell} opens the editor - the only {@link #edit} with {@link EditTrigger#AllEditTriggers} let through.
     */
    private boolean myOpeningEditor;
    /**
     * Set while the editor is closed on behalf of the controller, so that {@link #closeEditor} and {@link #commitData} do not ask it
     * again.
     */
    private boolean myClosingEditor;
    /**
     * Set while the current cell is put back on the edited cell after a refused commit.
     */
    private boolean myRestoringCurrent;
    /**
     * A mouse press was dropped because the editor refused to stop: the release and the moves of that press are dropped too, and the
     * release shows the error again - qt hides a tooltip on every mouse release.
     */
    private boolean myPressDropped;
    /**
     * Set while qt delivers a context menu event of the keyboard (the menu key) to the table.
     */
    private boolean myKeyboardContextMenu;
    /**
     * Set while a column is put back where it was - qt reports that move like the one of the user.
     */
    private boolean myRevertingColumnMove;
    /**
     * The column whose sort area the last press on the column header hit, or {@code -1}: its click sorts instead of selecting.
     */
    private int mySortPressSection = -1;
    private @Nullable QPoint myColumnHeaderPressPosition;
    private @Nullable QPoint myColumnHeaderReleasePosition;

    @RequiredUIAccess
    DesktopQtDataGridTableView(QWidget parent, DataGridController controller) {
        super(parent);
        myController = controller;
        myDelegate = new DesktopQtDataGridItemDelegate(this, controller);

        setItemDelegate(myDelegate);
        // editors are opened by editCell alone - neither the keymap nor the triggers of qt compete with it, and a grid which edits
        // nothing behaves as a read-only table
        setEditTriggers(EditTrigger.NoEditTriggers);

        // the sorting of qt stays off, its default - the controller sorts. setSortingEnabled is never called: each call connects the
        // selection of whole columns of qt to the column header once more
        myColumnHeader = new ColumnHeaderView(this);
        setHorizontalHeader(myColumnHeader);
        // whole columns are selected through the controller: the selection qt makes on these two signals would answer the same press
        // a second time, and toggle a column back
        myColumnHeader.sectionPressed.disconnect();
        myColumnHeader.sectionEntered.disconnect();
        myColumnHeader.sectionPressed.connect(section -> onColumnHeaderPressed(section));
        myColumnHeader.sectionEntered.connect(section -> onColumnHeaderEntered(section));
        myColumnHeader.sectionClicked.connect(section -> onColumnHeaderClicked(section));
        myColumnHeader.sectionMoved.connect((section, oldVisual, newVisual) -> onColumnHeaderMoved(oldVisual, newVisual));

        // the editor stops before a press on a header sorts or selects
        myHorizontalHeaderViewport = myColumnHeader.viewport();
        myVerticalHeaderViewport = verticalHeader().viewport();
        myHorizontalHeaderViewport.installEventFilter(this);
        myVerticalHeaderViewport.installEventFilter(this);

        doubleClicked.connect(this::onDoubleClicked);
    }

    DesktopQtDataGridItemDelegate getGridDelegate() {
        return myDelegate;
    }

    // region column header

    /**
     * The width of the sort area at the right edge of a column header, which shows the sort marker: the room a column keeps for it
     * besides its name.
     */
    int getSortAreaWidth() {
        return myColumnHeader.fontMetrics().horizontalAdvance(SORT_MARKER_SAMPLE);
    }

    /**
     * The columns can be dragged to another place while the controller can move them ({@link DataGridController#canMoveColumns()}).
     */
    @RequiredUIAccess
    void updateColumnMoving() {
        myColumnHeader.setSectionsMovable(myController.canMoveColumns());
    }

    /**
     * Puts every column back at its place. A reset of the model keeps the sections of the header as long as their number stays, so a
     * column the user dragged would stay where it was dropped - over the columns the controller has in the new order already.
     */
    void resetColumnOrder() {
        if (!myColumnHeader.sectionsMoved()) {
            return;
        }

        myRevertingColumnMove = true;
        try {
            for (int logical = 0; logical < myColumnHeader.count(); logical++) {
                int visual = myColumnHeader.visualIndex(logical);
                if (visual >= 0 && visual != logical) {
                    myColumnHeader.moveSection(visual, logical);
                }
            }
        }
        finally {
            myRevertingColumnMove = false;
        }
    }

    /**
     * The name of a column - the column header shows its sort marker apart from it.
     */
    static String getColumnName(DataGridController controller, int viewColumn) {
        GridColumn column = controller.getColumn(viewColumn);
        return column == null ? "" : controller.getName(column);
    }

    /**
     * The sort marker {@link DataGridController#getColumnHeaderText} puts after the name of a sorted column, or an empty string.
     */
    static String getSortMarker(DataGridController controller, int viewColumn) {
        String name = getColumnName(controller, viewColumn);
        String text = controller.getColumnHeaderText(viewColumn);
        return text.length() > name.length() && text.startsWith(name) ? text.substring(name.length()).strip() : "";
    }

    /**
     * A press on a column header selects the whole column - Shift extends the selection from the lead column, Ctrl (Cmd on macOS)
     * toggles the column - unless it hits the sort area: that press sorts once it is released there.
     */
    @RequiredUIAccess
    private void onColumnHeaderPressed(int section) {
        // qt reads right after this signal whether the press may drag the column
        updateColumnMoving();

        mySortPressSection = isInSortArea(section, myColumnHeaderPressPosition) ? section : -1;
        if (mySortPressSection >= 0 || section < 0 || section >= myController.getViewColumnCount()) {
            return;
        }

        Qt.KeyboardModifiers modifiers = QGuiApplication.keyboardModifiers();
        myController.onColumnHeaderClicked(section, modifiers.testFlag(Qt.KeyboardModifier.ShiftModifier), isToggleModifier(modifiers));
    }

    /**
     * A drag over the column headers selects the columns it passes - qt reports it only while the columns cannot be dragged to
     * another place.
     */
    @RequiredUIAccess
    private void onColumnHeaderEntered(int section) {
        if (mySortPressSection >= 0
            || section < 0
            || section >= myController.getViewColumnCount()
            || !QGuiApplication.mouseButtons().testFlag(Qt.MouseButton.LeftButton)) {
            return;
        }

        myController.onColumnHeaderClicked(section, true, isToggleModifier(QGuiApplication.keyboardModifiers()));
    }

    /**
     * A click pressed and released on the sort area of the same column toggles its sorting; Shift or Alt keeps the other sorted
     * columns.
     */
    @RequiredUIAccess
    private void onColumnHeaderClicked(int section) {
        int pressed = mySortPressSection;
        mySortPressSection = -1;
        if (pressed < 0 || pressed != section || !isInSortArea(section, myColumnHeaderReleasePosition)) {
            return;
        }

        Qt.KeyboardModifiers modifiers = QGuiApplication.keyboardModifiers();
        boolean additive = modifiers.testFlag(Qt.KeyboardModifier.AltModifier) || modifiers.testFlag(Qt.KeyboardModifier.ShiftModifier);
        myController.onColumnHeaderSortClicked(section, additive);
    }

    /**
     * The user dropped a dragged column at another place: the controller moves it in the data source, and the next change of the
     * structure shows the columns in their new order. A move the controller does not take is put back at once.
     */
    @RequiredUIAccess
    private void onColumnHeaderMoved(int oldVisualIndex, int newVisualIndex) {
        if (myRevertingColumnMove) {
            return;
        }

        // the controller takes a move within its own order of the columns; a header whose columns were moved already - a move still
        // on its way to the data source - does not show that order, and its move is put back
        if (!isSingleMove(oldVisualIndex, newVisualIndex) || !myController.onColumnMoved(oldVisualIndex, newVisualIndex)) {
            myRevertingColumnMove = true;
            try {
                myColumnHeader.moveSection(newVisualIndex, oldVisualIndex);
            }
            finally {
                myRevertingColumnMove = false;
            }
        }
    }

    /**
     * Whether the columns are in the order of the controller, but for the one just moved.
     */
    private boolean isSingleMove(int from, int to) {
        int count = myColumnHeader.count();
        if (from < 0 || from >= count || to < 0 || to >= count) {
            return false;
        }

        List<Integer> expected = new ArrayList<>(count);
        for (int logical = 0; logical < count; logical++) {
            expected.add(logical);
        }
        expected.add(to, expected.remove(from));

        for (int visual = 0; visual < count; visual++) {
            if (myColumnHeader.logicalIndex(visual) != expected.get(visual)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a point of the column header viewport is on the sort area of a sortable column: the room for the sort marker at its
     * right edge, with the margin after it.
     */
    private boolean isInSortArea(int section, @Nullable QPoint position) {
        if (position == null || !isSortable(section)) {
            return false;
        }

        int left = myColumnHeader.sectionViewportPosition(section);
        int right = left + myColumnHeader.sectionSize(section);
        int sortLeft = Math.max(left, right - getSortAreaWidth() - myColumnHeader.getMargin());
        return position.x() >= sortLeft && position.x() < right;
    }

    private boolean isSortable(int section) {
        return section >= 0 && section < myController.getViewColumnCount() && myController.isColumnSortable(section);
    }

    /**
     * The exclusive modifier of a selection: Ctrl, which is Cmd on macOS, or Meta.
     */
    private static boolean isToggleModifier(Qt.KeyboardModifiers modifiers) {
        return modifiers.testAnyFlags(Qt.KeyboardModifier.ControlModifier, Qt.KeyboardModifier.MetaModifier);
    }

    // endregion

    // region context menu

    /**
     * Whether the context menu asked for right now comes from the keyboard (the menu key) rather than from the pointer: it is
     * opened on the lead cell then.
     */
    boolean isKeyboardContextMenu() {
        return myKeyboardContextMenu;
    }

    /**
     * A context menu event of the keyboard reaches the table itself - one of the pointer reaches its viewport - and the table asks
     * for its menu in both cases ({@code customContextMenuRequested}), which tells neither apart: the kind of the event is kept
     * while it is delivered.
     */
    @Override
    public boolean event(@Nullable QEvent event) {
        if (event != null
            && event.type() == QEvent.Type.ContextMenu
            && event instanceof QContextMenuEvent menuEvent
            && menuEvent.reason() == QContextMenuEvent.Reason.Keyboard) {
            myKeyboardContextMenu = true;
            try {
                return super.event(event);
            }
            finally {
                myKeyboardContextMenu = false;
            }
        }
        return super.event(event);
    }

    // endregion

    /**
     * Opens the editor of a cell: an open editor stops first and stays when it refuses; the controller opens the edit, and qt shows
     * the widget for it, which takes the focus.
     *
     * @return whether an editor is open for the cell
     */
    @RequiredUIAccess
    boolean editCell(int viewRow, int viewColumn, GridEditInitiator initiator) {
        QAbstractItemModel model = model();
        if (model == null) {
            return false;
        }

        QModelIndex index = model.index(viewRow, viewColumn);
        if (!index.isValid()) {
            return false;
        }

        QWidget editor = myDelegate.getEditor();
        if (editor != null) {
            if (myDelegate.isEditing(index)) {
                // the editor of this cell is open already - it only lost the focus
                myDelegate.focusEditor();
                return true;
            }

            if (!stopCellEditor()) {
                return false;
            }
        }

        DataGridEditSession session = myController.startEditing(viewRow, viewColumn, initiator);
        if (session == null) {
            // the cell is not edited, or its value was decided and committed without an editor (a boolean set by a typed key)
            return false;
        }

        scrollTo(index);

        boolean opened;
        myOpeningEditor = true;
        try {
            opened = edit(index, EditTrigger.AllEditTriggers, null);
        }
        finally {
            myOpeningEditor = false;
        }

        if (!opened || myDelegate.getEditor() == null) {
            // qt refused to open the widget, which the slot edit(QModelIndex) would not even tell - the open edit would block every
            // later one
            if (myController.getEditSession() == session) {
                myController.discardEditing();
            }
            return false;
        }

        myDelegate.editorOpened();
        return true;
    }

    /**
     * Stops the editor: the controller commits the edit, and the editor closes when it is accepted.
     * A refused commit keeps the editor open, showing why.
     *
     * @return {@code false} when the editor stays open
     */
    @RequiredUIAccess
    boolean stopCellEditor() {
        QWidget editor = myDelegate.getEditor();
        if (editor == null) {
            return true;
        }

        DataGridEditSession session = myDelegate.getSession();
        if (session != null && myController.getEditSession() == session && !myController.commitEditing()) {
            // nothing is shown while the grid asks whether to ignore unsubmitted changes - its answer goes on with the edit
            myDelegate.showError();
            return false;
        }

        closeEditorWidget();
        return true;
    }

    /**
     * Cancels the editor: the controller drops the edit, and the editor closes.
     */
    @RequiredUIAccess
    void cancelCellEditor() {
        QWidget editor = myDelegate.getEditor();
        if (editor == null) {
            return;
        }

        DataGridEditSession session = myDelegate.getSession();
        if (session != null && myController.getEditSession() == session) {
            myController.discardEditing();
        }

        closeEditorWidget();
    }

    /**
     * The editor stops, or is cancelled when it refuses - except while the grid asks whether to ignore unsubmitted changes: the
     * answer goes on with the edit ({@link DataGridEditSession#isWaitingForAnswer()}).
     */
    @RequiredUIAccess
    void removeCellEditor() {
        if (myDelegate.getEditor() != null && !stopCellEditor() && !isWaitingForAnswer()) {
            cancelCellEditor();
        }
    }

    private boolean isWaitingForAnswer() {
        DataGridEditSession session = myDelegate.getSession();
        return session != null && myController.getEditSession() == session && session.isWaitingForAnswer();
    }

    /**
     * The widget is gone without a commit (the grid is torn down): the edit it showed is dropped.
     */
    @RequiredUIAccess
    void discardEditor() {
        DataGridEditSession session = myDelegate.getSession();
        myDelegate.releaseEditor();
        if (session != null && myController.getEditSession() == session) {
            myController.discardEditing();
        }
    }

    /**
     * Tab in the editor: the editor stops, and the current cell moves to the next or the previous one, as Tab does without an
     * editor.
     */
    @RequiredUIAccess
    void stopCellEditorAndMove(boolean forward) {
        if (!stopCellEditor()) {
            return;
        }

        QItemSelectionModel selectionModel = selectionModel();
        if (selectionModel == null) {
            return;
        }

        CursorAction action = forward ? CursorAction.MoveNext : CursorAction.MovePrevious;
        QModelIndex next = moveCursor(action, Qt.KeyboardModifier.NoModifier.asFlags());
        if (next.isValid()) {
            selectionModel.setCurrentIndex(next, QItemSelectionModel.SelectionFlag.ClearAndSelect);
        }
    }

    /**
     * The editor asks for new bounds - a multi-line editor grows with its lines.
     */
    void refreshEditorGeometry() {
        updateEditorGeometries();
    }

    private void closeEditorWidget() {
        QWidget editor = myDelegate.releaseEditor();
        if (editor == null || editor.isDisposed()) {
            return;
        }

        myClosingEditor = true;
        try {
            closeEditor(editor, QAbstractItemDelegate.EndEditHint.NoHint);
        }
        finally {
            myClosingEditor = false;
        }
    }

    @RequiredUIAccess
    private void onDoubleClicked(QModelIndex index) {
        if (!index.isValid() || !myController.isCellEditingAllowed()) {
            return;
        }

        // a double click edits the cell
        editCell(index.row(), index.column(), GridEditInitiator.MOUSE);
    }

    /**
     * qt edits only through {@link #editCell}: a forced edit of its own - the next cell after {@link #closeEditor} with
     * {@code EditNextItem}, or the public slot {@code edit(QModelIndex)} - has no session of the controller behind it.
     */
    @Override
    protected boolean edit(QModelIndex index, EditTrigger trigger, @Nullable QEvent event) {
        if (trigger == EditTrigger.AllEditTriggers && !myOpeningEditor) {
            return false;
        }
        return super.edit(index, trigger, event);
    }

    /**
     * qt commits and closes the editor whenever the current cell moves. Here the editor stops before the current cell moves, and
     * both stay where they are when it refuses.
     */
    @Override
    protected void currentChanged(QModelIndex current, QModelIndex previous) {
        if (!myRestoringCurrent && myDelegate.getEditor() != null && !myDelegate.isEditing(current) && !stopCellEditor()) {
            restoreCurrent();
            return;
        }
        super.currentChanged(current, previous);
    }

    private void restoreCurrent() {
        QModelIndex edited = myDelegate.getEditedIndex();
        QItemSelectionModel selectionModel = selectionModel();
        if (edited == null || selectionModel == null) {
            return;
        }

        myRestoringCurrent = true;
        try {
            selectionModel.setCurrentIndex(edited, QItemSelectionModel.SelectionFlag.NoUpdate);
        }
        finally {
            myRestoringCurrent = false;
        }
    }

    /**
     * The controller writes the value, and {@code setModelData} of the delegate does nothing - a commit qt asks for by itself is a
     * stop of the editor.
     */
    @Override
    protected void commitData(@Nullable QWidget editor) {
        if (!myClosingEditor && editor != null && editor == myDelegate.getEditor()) {
            stopCellEditor();
            return;
        }
        super.commitData(editor);
    }

    /**
     * An editor qt closes by itself is ended through the controller, which may refuse: then it stays open.
     */
    @Override
    protected void closeEditor(@Nullable QWidget editor, QAbstractItemDelegate.EndEditHint hint) {
        if (!myClosingEditor && editor != null && editor == myDelegate.getEditor()) {
            if (hint == QAbstractItemDelegate.EndEditHint.RevertModelCache) {
                cancelCellEditor();
            }
            else {
                stopCellEditor();
            }
            return;
        }
        super.closeEditor(editor, hint);
    }

    /**
     * A press anywhere in the table but on the editor stops the editor first; when it refuses, the press is
     * dropped and the editor keeps the focus.
     */
    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (myDelegate.getEditor() != null && !myDelegate.isEditing(indexAt(event.position().toPoint())) && !stopCellEditor()) {
            myPressDropped = true;
            event.accept();
            return;
        }
        super.mousePressEvent(event);
    }

    @Override
    protected void mouseMoveEvent(QMouseEvent event) {
        if (myPressDropped) {
            event.accept();
            return;
        }
        super.mouseMoveEvent(event);
    }

    @Override
    protected void mouseReleaseEvent(QMouseEvent event) {
        if (myPressDropped) {
            myPressDropped = false;
            myDelegate.showError();
            event.accept();
            return;
        }
        super.mouseReleaseEvent(event);
    }

    /**
     * A press on a header sorts or selects, so it stops the editor first, as a press on a cell does. The points of the presses and
     * releases on the column header are kept, so the click they make can be told to hit the sort area or not, and the column under
     * the pointer shows its sort hint.
     */
    @Override
    public boolean eventFilter(@Nullable QObject watched, @Nullable QEvent event) {
        if (event == null || watched != myHorizontalHeaderViewport && watched != myVerticalHeaderViewport) {
            return super.eventFilter(watched, event);
        }

        QEvent.Type type = event.type();
        if (watched == myHorizontalHeaderViewport) {
            if (type == QEvent.Type.MouseMove && event instanceof QMouseEvent mouseEvent) {
                myColumnHeader.setHoveredSection(myColumnHeader.logicalIndexAt(mouseEvent.position().toPoint()));
            }
            else if (type == QEvent.Type.Leave) {
                myColumnHeader.setHoveredSection(-1);
            }
        }

        if (type == QEvent.Type.MouseButtonPress && myDelegate.getEditor() != null && !stopCellEditor()) {
            myPressDropped = true;
            event.accept();
            return true;
        }
        if (myPressDropped && (type == QEvent.Type.MouseMove || type == QEvent.Type.MouseButtonRelease)) {
            if (type == QEvent.Type.MouseButtonRelease) {
                myPressDropped = false;
                myDelegate.showError();
            }
            event.accept();
            return true;
        }

        if (watched == myHorizontalHeaderViewport && event instanceof QMouseEvent mouseEvent) {
            if (type == QEvent.Type.MouseButtonPress) {
                myColumnHeaderPressPosition = mouseEvent.position().toPoint();
            }
            else if (type == QEvent.Type.MouseButtonRelease) {
                myColumnHeaderReleasePosition = mouseEvent.position().toPoint();
            }
        }
        return super.eventFilter(watched, event);
    }

    @Override
    protected void keyPressEvent(QKeyEvent event) {
        if (event.matches(QKeySequence.StandardKey.Copy)) {
            // the copy of the IDE copies the selected cells through the copy provider of the grid - the item view of qt would copy the
            // text of the current cell instead
            event.ignore();
            return;
        }

        if (myController.isCellEditingAllowed() && handleEditingKey(event)) {
            event.accept();
            return;
        }
        super.keyPressEvent(event);
    }

    /**
     * @return whether the key was taken
     */
    @RequiredUIAccess
    private boolean handleEditingKey(QKeyEvent event) {
        int key = event.key();
        Qt.KeyboardModifiers modifiers = event.modifiers();
        boolean plain = !modifiers.testAnyFlags(Qt.KeyboardModifier.ControlModifier, Qt.KeyboardModifier.AltModifier,
            Qt.KeyboardModifier.MetaModifier, Qt.KeyboardModifier.ShiftModifier);

        if (myDelegate.getEditor() != null) {
            if (myDelegate.isEditorFocused()) {
                // a key the editor left alone - an arrow up in a line edit moves to the row above once the editor stops (the
                // navigation of the table); any other is dropped, so a read-only editor does not search the table for what is typed into it
                return !isCursorKey(key) || !stopCellEditor();
            }

            // the editor stays open while the focus is in the table
            if (key == Qt.Key.Key_Escape.value()) {
                cancelCellEditor();
                return true;
            }
            if (isCursorKey(key)) {
                return !stopCellEditor();
            }
            if ((isEnter(key) || key == Qt.Key.Key_F2.value()) && plain) {
                myDelegate.focusEditor();
                return true;
            }
            if (typedText(event) != null) {
                myDelegate.focusEditor();
                myDelegate.redeliver(event);
                return true;
            }
            return false;
        }

        if ((isEnter(key) || key == Qt.Key.Key_F2.value()) && plain) {
            // Enter and F2 edit the lead cell, instead of what the table does with them
            myController.editSelectedCell();
            return true;
        }

        String typed = typedText(event);
        if (typed != null) {
            QModelIndex current = currentIndex();
            if (current.isValid() && myController.isCellEditable(current.row(), current.column())) {
                // a really typed key starts the edit and goes into the field
                editCell(current.row(), current.column(), GridEditInitiator.typed(typed));
                return true;
            }
        }
        return false;
    }

    /**
     * An input method commits text the way a key types it.
     */
    @Override
    protected void inputMethodEvent(QInputMethodEvent event) {
        String commit = event.commitString();
        QModelIndex current = currentIndex();
        if (myController.isCellEditingAllowed()
            && myDelegate.getEditor() == null
            && !commit.isEmpty()
            && current.isValid()
            && myController.isCellEditable(current.row(), current.column())) {
            editCell(current.row(), current.column(), GridEditInitiator.typed(commit));
            event.accept();
            return;
        }
        super.inputMethodEvent(event);
    }

    /**
     * The editor of a table which is hidden stops, or is cancelled. A window which is
     * minimized hides the table too, spontaneously - that keeps the editor.
     */
    @Override
    protected void hideEvent(QHideEvent event) {
        super.hideEvent(event);
        if (!event.spontaneous() && myDelegate.getEditor() != null) {
            removeCellEditor();
        }
    }

    /**
     * @return the text of a really typed key - a printable character, typed without Ctrl, Alt or Meta (Cmd on macOS) - or
     * {@code null}
     */
    static @Nullable String typedText(QKeyEvent event) {
        String text = event.text();
        if (text.isEmpty() || Character.isISOControl(text.charAt(0))) {
            return null;
        }

        Qt.KeyboardModifiers modifiers = event.modifiers();
        boolean commandModifier = modifiers.testAnyFlags(Qt.KeyboardModifier.ControlModifier, Qt.KeyboardModifier.AltModifier,
            Qt.KeyboardModifier.MetaModifier);
        return commandModifier ? null : text;
    }

    static boolean isEnter(int key) {
        return key == Qt.Key.Key_Return.value() || key == Qt.Key.Key_Enter.value();
    }

    private static boolean isCursorKey(int key) {
        return key == Qt.Key.Key_Up.value()
            || key == Qt.Key.Key_Down.value()
            || key == Qt.Key.Key_Left.value()
            || key == Qt.Key.Key_Right.value()
            || key == Qt.Key.Key_PageUp.value()
            || key == Qt.Key.Key_PageDown.value()
            || key == Qt.Key.Key_Home.value()
            || key == Qt.Key.Key_End.value()
            || key == Qt.Key.Key_Tab.value()
            || key == Qt.Key.Key_Backtab.value();
    }

    static boolean isFocusIn(@Nullable QWidget focus, QWidget widget) {
        return focus != null && (focus == widget || widget.isAncestorOf(focus));
    }

    /**
     * The column header: a sortable column shows its sort marker at its right edge, in the sort area, and its name is cut before
     * it. The model gives the name alone; the marker is read from the controller. A sortable column which is not sorted shows a
     * faint marker there while the pointer is over its header, so the place a click sorts at can be found.
     */
    private final class ColumnHeaderView extends QHeaderView {
        private int myHoveredSection = -1;

        ColumnHeaderView(QWidget parent) {
            super(Qt.Orientation.Horizontal, parent);
            setSectionsClickable(true);
            // the moves of the pointer without a button show the sort hint
            setMouseTracking(true);
        }

        void setHoveredSection(int section) {
            if (section == myHoveredSection) {
                return;
            }

            myHoveredSection = section;
            viewport().update();
        }

        int getMargin() {
            return style().pixelMetric(QStyle.PixelMetric.PM_HeaderMargin);
        }

        @Override
        protected void initStyleOptionForIndex(@Nullable QStyleOptionHeader option, int logicalIndex) {
            super.initStyleOptionForIndex(option, logicalIndex);
            if (option == null || !isSortable(logicalIndex)) {
                return;
            }

            // the name is cut before the sort area, so the marker does not overlap it
            int available = sectionSize(logicalIndex) - 2 * getMargin() - getSortAreaWidth();
            option.setText(available > 0 ? fontMetrics().elidedText(option.text(), Qt.TextElideMode.ElideRight, available) : "");
        }

        @Override
        protected void paintSection(@Nullable QPainter painter, QRect rect, int logicalIndex) {
            super.paintSection(painter, rect, logicalIndex);
            if (painter == null || !rect.isValid() || !isSortable(logicalIndex)) {
                return;
            }

            String marker = getSortMarker(myController, logicalIndex);
            boolean hint = marker.isEmpty() && logicalIndex == myHoveredSection;
            if (marker.isEmpty() && !hint) {
                return;
            }

            int width = getSortAreaWidth();
            int left = Math.max(rect.left(), rect.right() + 1 - getMargin() - width);
            QRect area = new QRect(left, rect.top(), rect.right() + 1 - getMargin() - left, rect.height());
            if (area.width() <= 0) {
                return;
            }

            painter.save();
            try {
                painter.setFont(font());
                painter.setPen(hint ? TargetQt.to(ComponentColors.DISABLED_TEXT) : palette().color(QPalette.ColorRole.ButtonText));
                int flags = Qt.AlignmentFlag.AlignRight.value() | Qt.AlignmentFlag.AlignVCenter.value();
                painter.drawText(area, flags, hint ? SORT_HINT : marker);
            }
            finally {
                painter.restore();
            }
        }
    }
}
