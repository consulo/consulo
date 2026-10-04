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

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.util.TextAttributeUtil;
import consulo.ui.font.Font;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.internal.DataGridController;
import consulo.ui.internal.DataGridEditSession;
import consulo.ui.style.ComponentColors;
import io.qt.core.QAbstractItemModel;
import io.qt.core.QCoreApplication;
import io.qt.core.QEvent;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFocusEvent;
import io.qt.gui.QFont;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QInputMethodEvent;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QKeySequence;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QTextCharFormat;
import io.qt.gui.QTextCursor;
import io.qt.widgets.QApplication;
import io.qt.widgets.QComboBox;
import io.qt.widgets.QFrame;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QPlainTextEdit;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionViewItem;
import io.qt.widgets.QTextEdit;
import io.qt.widgets.QToolTip;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The cells of {@link DesktopQtDataGridImpl}: painted as every item view of the qt frontend paints them - a row of several lines
 * ({@link DataGridController#getRowLines()}) shows the lines of a value one under the other - and edited in the widget this
 * delegate builds for the {@link DataGridEditSession} the {@link DataGridController} opened - a frameless {@link QLineEdit}, a
 * {@link QPlainTextEdit} for a multi-line text, or a {@link QComboBox} for a list of options. The widget is a child of the table and
 * inherits its font, the plain font of the editor colours scheme.
 * <p/>
 * The widget only exchanges text and gestures with the edit: every change of its text goes to {@link DataGridEditSession#setText},
 * and the keys and the focus are mapped to the table, which asks the controller to commit or to cancel. The commit of
 * {@link io.qt.widgets.QStyledItemDelegate} is not used: {@link #setModelData} does nothing, since the controller writes the
 * value, and {@link #setEditorData} does nothing either - the widget is filled once, when it is created, because qt calls it again
 * on every change of the row, which would take back what the user typed.
 *
 * @since 2026-10-04
 */
public class DesktopQtDataGridItemDelegate extends DesktopQtTextItemDelegate {
    /**
     * A multi-line editor grows with its text over at most this many rows.
     */
    private static final int MAX_EDITOR_ROWS = 4;
    /**
     * The object name of the editor, which its error style sheet selects - so the rule reaches neither its children nor the cell.
     */
    private static final String EDITOR_NAME = "consuloGridCellEditor";

    private final DesktopQtDataGridTableView myTable;
    private final DataGridController myController;

    private @Nullable QWidget myEditor;
    private @Nullable DataGridEditSession mySession;
    private @Nullable QWidget myPopup;
    /**
     * The popup of the list was shown while a mouse button was down: the release of that button is not a choice.
     */
    private boolean myIgnorePopupRelease;
    private int mySelectedOptionOnOpen = -1;
    private boolean mySelectAllOnOpen;
    private boolean myTypedOnOpen;
    private boolean myOptionChosen;
    private boolean myErrorShown;
    private int myEditorRows = 1;
    /**
     * The height of a line of text in a row, or {@code 0} while the table did not set it.
     */
    private int myLineHeight;

    DesktopQtDataGridItemDelegate(DesktopQtDataGridTableView table, DataGridController controller) {
        super(table);
        myTable = table;
        myController = controller;
    }

    /**
     * The height of a line of text in a row: the lines of a value are painted this far apart, and a row is as many of them tall as
     * it shows lines, besides its padding.
     */
    void setLineHeight(int lineHeight) {
        myLineHeight = lineHeight;
    }

    @Nullable
    QWidget getEditor() {
        QWidget editor = myEditor;
        return editor != null && !editor.isDisposed() ? editor : null;
    }

    @Nullable
    DataGridEditSession getSession() {
        return mySession;
    }

    /**
     * Whether the open editor edits this cell.
     */
    boolean isEditing(QModelIndex index) {
        DataGridEditSession session = mySession;
        return session != null
            && getEditor() != null
            && index.isValid()
            && index.row() == session.getViewRow()
            && index.column() == session.getViewColumn();
    }

    @Nullable
    QModelIndex getEditedIndex() {
        DataGridEditSession session = mySession;
        QAbstractItemModel model = myTable.model();
        if (session == null || model == null) {
            return null;
        }

        QModelIndex index = model.index(session.getViewRow(), session.getViewColumn());
        return index.isValid() ? index : null;
    }

    boolean isEditorFocused() {
        QWidget editor = getEditor();
        return editor != null && DesktopQtDataGridTableView.isFocusIn(QApplication.focusWidget(), editor);
    }

    void focusEditor() {
        QWidget editor = getEditor();
        if (editor != null) {
            editor.setFocus();
        }
    }

    /**
     * A key typed into the table while the editor is open, but has no focus, goes into the editor.
     */
    void redeliver(QKeyEvent event) {
        QWidget editor = getEditor();
        if (editor != null) {
            QCoreApplication.sendEvent(editor, event);
        }
    }

    /**
     * Forgets the editor, which the table is about to close - when qt destroys it afterwards, the edit is not dropped again.
     *
     * @return the editor, if one was open
     */
    @Nullable
    QWidget releaseEditor() {
        QWidget editor = getEditor();
        if (myErrorShown && editor != null) {
            QToolTip.hideText();
        }

        myEditor = null;
        mySession = null;
        myPopup = null;
        myIgnorePopupRelease = false;
        myErrorShown = false;
        myOptionChosen = false;
        myEditorRows = 1;
        return editor;
    }

    // region painting

    /**
     * A row of several lines shows the lines of a value - the presentation the controller renders holds them separated by
     * {@code \n} - one under the other, as a block of as many lines as the row shows, so the first lines of the cells of a row stand
     * side by side. A line is cut at the edge of its cell; a selected cell is painted in the selected text colour. A row of one line
     * is painted as every item view of the qt frontend paints it.
     */
    @Override
    public void paint(@Nullable QPainter painter, QStyleOptionViewItem option, QModelIndex index) {
        DesktopQtTextItemPresentation presentation = presentationOf(index);
        if (painter == null
            || presentation == null
            || presentation.hasSuffix()
            || presentation.getImage() != null
            || myController.getRowLines() <= 1 && presentation.toString().indexOf('\n') < 0) {
            super.paint(painter, option, index);
            return;
        }

        QStyleOptionViewItem styleOption = new QStyleOptionViewItem(option);
        initStyleOption(styleOption, index);

        QWidget widget = styleOption.widget();
        QStyle style = widget != null ? widget.style() : QApplication.style();
        QRect textRect = style.subElementRect(QStyle.SubElement.SE_ItemViewItemText, styleOption, widget);

        // the background, the selection and the focus of the cell, without its text
        styleOption.setText("");
        style.drawControl(QStyle.ControlElement.CE_ItemViewItem, styleOption, painter, widget);

        QStyle.State state = styleOption.state();
        boolean selected = state.testFlag(QStyle.StateFlag.State_Selected);
        QPalette.ColorGroup group = !state.testFlag(QStyle.StateFlag.State_Enabled)
            ? QPalette.ColorGroup.Disabled
            : state.testFlag(QStyle.StateFlag.State_Active) ? QPalette.ColorGroup.Active : QPalette.ColorGroup.Inactive;
        // the text colour of the cell (its foreground role), or the selected text colour
        QColor defaultColor = styleOption.palette().color(group, selected ? QPalette.ColorRole.HighlightedText : QPalette.ColorRole.Text);
        QFont baseFont = styleOption.font();

        List<List<DesktopQtTextFragment>> lines = splitLines(presentation.getFragments());
        int lineHeight = myLineHeight > 0 ? myLineHeight : new QFontMetrics(baseFont).height();
        int rowLines = Math.max(lines.size(), myController.getRowLines());
        int top = textRect.top() + Math.max(0, (textRect.height() - rowLines * lineHeight) / 2);

        QRect cell = option.rect();
        painter.save();
        try {
            // a line does not reach into the next row
            painter.setClipRect(cell, Qt.ClipOperation.IntersectClip);
            for (int line = 0; line < lines.size(); line++) {
                int y = top + line * lineHeight;
                if (y > cell.bottom()) {
                    break;
                }

                paintLine(painter, lines.get(line), styleOption.displayAlignment(), baseFont, defaultColor, selected,
                    textRect.left(), textRect.right(), y, lineHeight);
            }
        }
        finally {
            painter.restore();
        }
    }

    private static @Nullable DesktopQtTextItemPresentation presentationOf(QModelIndex index) {
        if (!index.isValid()) {
            return null;
        }

        return index.data(PRESENTATION_ROLE) instanceof DesktopQtTextItemPresentation presentation ? presentation : null;
    }

    /**
     * @return the fragments of every line of the presentation, split at {@code \n}
     */
    private static List<List<DesktopQtTextFragment>> splitLines(List<DesktopQtTextFragment> fragments) {
        List<List<DesktopQtTextFragment>> lines = new ArrayList<>();
        List<DesktopQtTextFragment> line = new ArrayList<>();
        lines.add(line);
        for (DesktopQtTextFragment fragment : fragments) {
            String text = fragment.text().get();
            int start = 0;
            while (true) {
                int end = text.indexOf('\n', start);
                String part = end < 0 ? text.substring(start) : text.substring(start, end);
                if (!part.isEmpty()) {
                    line.add(new DesktopQtTextFragment(LocalizeValue.of(part), fragment.attribute()));
                }
                if (end < 0) {
                    break;
                }

                line = new ArrayList<>();
                lines.add(line);
                start = end + 1;
            }
        }
        return lines;
    }

    private static void paintLine(
        QPainter painter,
        List<DesktopQtTextFragment> line,
        Qt.Alignment alignment,
        QFont baseFont,
        QColor defaultColor,
        boolean selected,
        int left,
        int right,
        int top,
        int height
    ) {
        int x = left;

        boolean alignRight = alignment.testFlag(Qt.AlignmentFlag.AlignRight);
        if (alignRight || alignment.testFlag(Qt.AlignmentFlag.AlignHCenter)) {
            int free = right - left + 1 - lineWidth(baseFont, line);
            if (free > 0) {
                x += alignRight ? free : free / 2;
            }
        }

        for (DesktopQtTextFragment fragment : line) {
            int available = right - x + 1;
            if (available <= 0) {
                break;
            }

            String text = fragment.text().get();
            TextAttribute attribute = fragment.attribute();
            QFont font = fragmentFont(baseFont, attribute);
            QFontMetrics metrics = new QFontMetrics(font);

            int width = metrics.horizontalAdvance(text);
            boolean elided = width > available;
            if (elided) {
                text = metrics.elidedText(text, Qt.TextElideMode.ElideRight, available);
                width = metrics.horizontalAdvance(text);
            }

            QRect rect = new QRect(x, top, width, height);

            ColorValue background = attribute.getBackgroundColor();
            if (background != null && !selected) {
                painter.fillRect(rect, TargetQt.to(background));
            }

            ColorValue foreground = attribute.getForegroundColor();
            painter.setFont(font);
            painter.setPen(selected || foreground == null ? defaultColor : TargetQt.to(foreground));
            painter.drawText(rect, Qt.AlignmentFlag.AlignLeft.value() | Qt.AlignmentFlag.AlignVCenter.value(), text);

            x += width;
            if (elided) {
                break;
            }
        }
    }

    private static int lineWidth(QFont baseFont, List<DesktopQtTextFragment> line) {
        int width = 0;
        for (DesktopQtTextFragment fragment : line) {
            width += new QFontMetrics(fragmentFont(baseFont, fragment.attribute())).horizontalAdvance(fragment.text().get());
        }
        return width;
    }

    private static QFont fragmentFont(QFont baseFont, TextAttribute attribute) {
        int fontStyle = TextAttributeUtil.getFontStyle(attribute);
        Set<TextEffect> effects = TextAttributeUtil.getEffects(attribute);

        QFont font = new QFont(baseFont);
        if ((fontStyle & Font.BOLD) != 0) {
            font.setBold(true);
        }
        if ((fontStyle & Font.ITALIC) != 0) {
            font.setItalic(true);
        }
        if (effects.contains(TextEffect.STRIKEOUT)) {
            font.setStrikeOut(true);
        }
        if (effects.contains(TextEffect.UNDERLINE) || effects.contains(TextEffect.WAVED)) {
            font.setUnderline(true);
        }
        return font;
    }

    // endregion

    // region the editor of qt

    /**
     * The widget of the edit the controller opened for this cell, filled with its text - and with the text typed to start it: the
     * edit is opened without the key event, so qt has none to forward to the widget.
     */
    @Override
    @RequiredUIAccess
    public @Nullable QWidget createEditor(QWidget parent, QStyleOptionViewItem option, QModelIndex index) {
        DataGridEditSession session = myController.getEditSession();
        if (session == null || session.getViewRow() != index.row() || session.getViewColumn() != index.column()) {
            // every edit is opened by the controller first: there is nothing to edit without its session
            return null;
        }

        GridCellEditorPresentation presentation = session.getPresentation();
        String typed = session.getTypedText();

        QWidget editor = switch (presentation.kind()) {
            case TEXT -> createTextField(parent, presentation, typed);
            case MULTILINE_TEXT -> createMultilineTextField(parent, presentation, typed);
            case LIST -> createList(parent, presentation);
            case NONE -> null;
        };
        if (editor == null) {
            return null;
        }

        editor.setObjectName(EDITOR_NAME);

        myEditor = editor;
        mySession = session;
        myPopup = null;
        myIgnorePopupRelease = false;
        mySelectedOptionOnOpen = presentation.selectedOption();
        mySelectAllOnOpen = presentation.selectAll();
        myTypedOnOpen = !typed.isEmpty();
        myOptionChosen = false;
        myErrorShown = false;

        if (editor instanceof QLineEdit field) {
            field.textChanged.connect(text -> onTextChanged(session, field, text));
            if (!typed.isEmpty()) {
                session.setText(field.text());
            }
        }
        else if (editor instanceof QPlainTextEdit field) {
            field.textChanged.connect(() -> onTextChanged(session, field, getText(field)));
            if (!typed.isEmpty()) {
                session.setText(getText(field));
            }
        }
        else if (editor instanceof QComboBox list) {
            list.activated.connect(chosen -> onOptionChosen(session, chosen));
        }

        editor.destroyed.connect(() -> onEditorGone(editor));
        return editor;
    }

    /**
     * Does nothing: the editor was filled when it was created. qt calls this again for every change of its row - a multi-cell edit
     * changes the rows it writes on every key - and would take back what the user typed.
     */
    @Override
    public void setEditorData(QWidget editor, QModelIndex index) {
    }

    /**
     * Does nothing: the controller writes the value of the edit.
     */
    @Override
    public void setModelData(QWidget editor, QAbstractItemModel model, QModelIndex index) {
    }

    /**
     * The cell; a multi-line editor covers the rows below it as well, as many as it has lines, up to {@link #MAX_EDITOR_ROWS} - it
     * overlays them rather than making its row taller. A row of several lines holds as many lines of the editor, which reaches below
     * it only for more. It ends with the table, where it scrolls its lines instead - its first line stays on the row it edits.
     */
    @Override
    public void updateEditorGeometry(QWidget editor, QStyleOptionViewItem option, QModelIndex index) {
        QRect rect = option.rect();
        if (editor != myEditor || !(editor instanceof QPlainTextEdit)) {
            editor.setGeometry(rect);
            return;
        }

        int height = myController.getRowLines() > 1 && myLineHeight > 0
            ? Math.max(rect.height(), myEditorRows * myLineHeight + DesktopQtDataGridImpl.ROW_TEXT_PADDING + 1)
            : rect.height() * myEditorRows;
        QWidget viewport = editor.parentWidget();
        if (viewport != null) {
            height = Math.max(rect.height(), Math.min(height, viewport.height() - rect.top()));
        }
        editor.setGeometry(rect.left(), rect.top(), rect.width(), height);
    }

    /**
     * qt destroys an editor which it releases by itself - for a reset of the model, or with the table: the edit it showed is dropped.
     */
    @Override
    public void destroyEditor(QWidget editor, QModelIndex index) {
        onEditorGone(editor);
        super.destroyEditor(editor, index);
    }

    @RequiredUIAccess
    private void onEditorGone(QWidget editor) {
        if (editor != myEditor) {
            return;
        }

        DataGridEditSession session = mySession;
        releaseEditor();
        if (session != null && myController.getEditSession() == session) {
            myController.discardEditing();
        }
    }

    // endregion

    // region widgets

    private static QLineEdit createTextField(QWidget parent, GridCellEditorPresentation presentation, String typed) {
        QLineEdit field = new QLineEdit(parent);
        field.setFrame(false);
        field.setAlignment(DesktopQtColumnSupport.toAlignment(presentation.alignment()));
        // the placeholder shows while the field has the focus too, which a line edit of qt does
        field.setPlaceholderText(presentation.placeholder());
        // a value which cannot be edited is still shown in the field, read-only
        field.setReadOnly(presentation.readOnly());
        field.setText(presentation.text() + typed);
        return field;
    }

    private static QPlainTextEdit createMultilineTextField(QWidget parent, GridCellEditorPresentation presentation, String typed) {
        QPlainTextEdit field = new QPlainTextEdit(parent);
        field.setFrameShape(QFrame.Shape.NoFrame);
        field.setLineWrapMode(QPlainTextEdit.LineWrapMode.NoWrap);
        field.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
        field.setVerticalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAsNeeded);
        field.document().setDocumentMargin(2);
        field.setPlaceholderText(presentation.placeholder());
        field.setReadOnly(presentation.readOnly());
        field.setPlainText(presentation.text() + typed);
        return field;
    }

    /**
     * The cell keeps showing its value while the options are open under it: nothing is chosen in the list, and the value of the cell
     * is its placeholder, in the colour of a text. The list paints its background, over the text of the cell.
     */
    private static QComboBox createList(QWidget parent, GridCellEditorPresentation presentation) {
        QComboBox list = new QComboBox(parent);
        list.setFrame(false);
        list.setAutoFillBackground(true);
        list.addItems(presentation.options());
        list.setPlaceholderText(presentation.text());
        list.setCurrentIndex(-1);

        QPalette palette = list.palette();
        palette.setColor(QPalette.ColorRole.PlaceholderText, palette.color(QPalette.ColorRole.Text));
        list.setPalette(palette);
        return list;
    }

    /**
     * The text of a multi-line field. Shift+Enter puts a line separator (U+2028) into the document and the blocks are split by
     * paragraph separators (U+2029) - both are a new line of the value. {@code toPlainText()} would map them too, but it also turns
     * a non-breaking space into a space.
     */
    private static String getText(QPlainTextEdit field) {
        return field.document().toRawText().replace('\u2028', '\n').replace('\u2029', '\n');
    }

    private static int countRows(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return Math.min(lines, MAX_EDITOR_ROWS);
    }

    // endregion

    /**
     * The editor is shown and has the focus: the caret and the selection a new field starts with, or the list of options opened at
     * once.
     */
    @RequiredUIAccess
    void editorOpened() {
        QWidget editor = getEditor();
        DataGridEditSession session = mySession;
        if (editor == null || session == null) {
            return;
        }

        // qt selected the whole text of a line edit when it created it
        if (editor instanceof QLineEdit field) {
            if (mySelectAllOnOpen) {
                field.selectAll();
            }
            else {
                field.deselect();
                // a typed key went into the empty field, and the caret stays after it; otherwise the caret starts at the beginning
                field.setCursorPosition(myTypedOnOpen ? field.text().length() : 0);
            }
        }
        else if (editor instanceof QPlainTextEdit field) {
            myEditorRows = countRows(getText(field));
            myTable.refreshEditorGeometry();
            if (mySelectAllOnOpen) {
                field.selectAll();
            }
            else {
                field.moveCursor(myTypedOnOpen ? QTextCursor.MoveOperation.End : QTextCursor.MoveOperation.Start);
            }
        }
        else if (editor instanceof QComboBox list) {
            // closing the list without a choice cancels the edit, whichever way it closes - Escape, a press outside of it, the window
            // losing the focus - so the hiding of its popup is watched rather than hidePopup, which only some of them call
            QWidget popup = list.view().window();
            popup.installEventFilter(this);
            myPopup = popup;
            // opened by a double click, the release of its button comes after the popup is shown, outside of the options, and
            // would close the popup at once
            myIgnorePopupRelease = QGuiApplication.mouseButtons().value() != 0;
            list.showPopup();

            int selected = mySelectedOptionOnOpen;
            if (selected >= 0 && selected < list.count()) {
                list.view().setCurrentIndex(list.model().index(selected, 0));
            }
        }

        if (session.getError() != null) {
            showError();
        }
    }

    @RequiredUIAccess
    private void onTextChanged(DataGridEditSession session, QWidget field, String text) {
        if (mySession != session || myEditor != field) {
            return;
        }

        // every change of the text: the error goes away, and the edit gets the new value
        session.setText(text);

        if (myErrorShown) {
            // not from within the change of the text: a line edit is still busy with it
            QTimer.singleShot(0, field, () -> clearError(field));
        }

        if (field instanceof QPlainTextEdit) {
            int rows = countRows(text);
            if (rows != myEditorRows) {
                myEditorRows = rows;
                myTable.refreshEditorGeometry();
            }
        }
    }

    /**
     * An option was chosen: its value is applied, and the edit stops.
     */
    @RequiredUIAccess
    private void onOptionChosen(DataGridEditSession session, int option) {
        if (mySession != session || option < 0) {
            return;
        }

        myOptionChosen = true;
        session.selectOption(option);
        myTable.stopCellEditor();
    }

    @RequiredUIAccess
    private void onPopupHidden() {
        QWidget editor = getEditor();
        DataGridEditSession session = mySession;
        if (editor == null || session == null) {
            return;
        }

        // the popup hides before the option is activated - the choice is looked at once both are done
        QTimer.singleShot(0, editor, () -> {
            if (getEditor() == editor && mySession == session && !myOptionChosen) {
                myTable.cancelCellEditor();
            }
        });
    }

    // region keys and focus

    /**
     * Stands in for the editor event filter of {@link io.qt.widgets.QStyledItemDelegate}, which commits on its own: the keys and the
     * focus of the editor are mapped to the table, which asks the controller.
     */
    @Override
    public boolean eventFilter(@Nullable QObject watched, @Nullable QEvent event) {
        if (event == null) {
            return false;
        }

        QWidget popup = myPopup;
        if (popup != null && watched == popup) {
            if (event.type() == QEvent.Type.Show) {
                myOptionChosen = false;
            }
            else if (event.type() == QEvent.Type.Hide) {
                myIgnorePopupRelease = false;
                onPopupHidden();
            }
            else if (event.type() == QEvent.Type.MouseButtonPress) {
                myIgnorePopupRelease = false;
            }
            else if (event.type() == QEvent.Type.MouseButtonRelease && myIgnorePopupRelease) {
                myIgnorePopupRelease = false;
                return true;
            }
            return false;
        }

        QWidget editor = getEditor();
        if (editor == null || watched != editor) {
            // a stale editor: it is closed, and no event of it commits anything any more
            return false;
        }

        QEvent.Type type = event.type();
        if (type == QEvent.Type.KeyPress && event instanceof QKeyEvent keyEvent) {
            return onKeyPressed(editor, keyEvent);
        }
        if (type == QEvent.Type.ShortcutOverride
            && event instanceof QKeyEvent keyEvent
            && keyEvent.matches(QKeySequence.StandardKey.Cancel)) {
            // Escape cancels the edit - no shortcut takes it away from the editor
            event.accept();
            return true;
        }
        if (type == QEvent.Type.FocusOut && event instanceof QFocusEvent focusEvent) {
            onFocusLost(editor, focusEvent);
        }
        return false;
    }

    @RequiredUIAccess
    private boolean onKeyPressed(QWidget editor, QKeyEvent event) {
        DataGridEditSession session = mySession;
        if (session == null) {
            return false;
        }

        if (event.matches(QKeySequence.StandardKey.Cancel)) {
            myTable.cancelCellEditor();
            return true;
        }

        int key = event.key();
        Qt.KeyboardModifiers modifiers = event.modifiers();
        boolean commandModifier = modifiers.testAnyFlags(Qt.KeyboardModifier.ControlModifier, Qt.KeyboardModifier.MetaModifier);

        if ((key == Qt.Key.Key_Tab.value() || key == Qt.Key.Key_Backtab.value())
            && !commandModifier
            && !modifiers.testFlag(Qt.KeyboardModifier.AltModifier)) {
            // Tab and Shift+Tab stop the edit and run the Tab actions of the table
            boolean forward = key == Qt.Key.Key_Tab.value() && !modifiers.testFlag(Qt.KeyboardModifier.ShiftModifier);
            myTable.stopCellEditorAndMove(forward);
            return true;
        }

        if (DesktopQtDataGridTableView.isEnter(key)) {
            // Enter and Ctrl+Enter stop the edit, except where a multi-line editor takes them for a new line
            if (editor instanceof QPlainTextEdit field) {
                if (commandModifier) {
                    // a multi-line editor gets a new line from Ctrl+Enter (Cmd+Enter on macOS), which a plain text edit ignores
                    if (!field.isReadOnly()) {
                        field.insertPlainText("\n");
                    }
                    return true;
                }
                if (modifiers.testFlag(Qt.KeyboardModifier.ShiftModifier)) {
                    // Shift+Enter starts a new line, as in the code editor: qt puts a line separator, read as a new line
                    return false;
                }
            }

            if (editor instanceof QComboBox list) {
                // the list was closed without a choice: Enter chooses its current option
                onOptionChosen(session, list.currentIndex());
                return true;
            }

            myTable.stopCellEditor();
            return true;
        }

        GridCellEditorPresentation presentation = session.getPresentation();
        if (presentation.readOnly() && !event.text().isEmpty() && !presentation.readOnlyHint().get().isEmpty()) {
            // the hint tells why a read-only field does not change
            showToolTip(editor, presentation.readOnlyHint().get());
        }
        return false;
    }

    /**
     * The focus going to another component of the window stops the editor, or cancels it when it refuses; the focus staying in the
     * table, or going to another window (a dialog - also the question whether to ignore unsubmitted changes), to a popup or nowhere
     * keeps it.
     */
    @RequiredUIAccess
    private void onFocusLost(QWidget editor, QFocusEvent event) {
        // a combo box gets a focus out with PopupFocusReason, and another one when its list takes the focus
        if (event.reason() == Qt.FocusReason.PopupFocusReason || QApplication.activePopupWidget() != null) {
            return;
        }

        QWidget focus = QApplication.focusWidget();
        if (focus == null || DesktopQtDataGridTableView.isFocusIn(focus, editor) || focus.window() != myTable.window()) {
            return;
        }

        if (DesktopQtDataGridTableView.isFocusIn(focus, myTable)) {
            // the focus remains inside the table, and so does the editor - a click into another cell stops it with its press, and the
            // keys typed into the table meanwhile go to it
            return;
        }

        myTable.removeCellEditor();
    }

    // endregion

    // region errors

    /**
     * The error of a refused value: a red outline, the text from the offset of the error to its end highlighted as an error, and the
     * message as a tooltip - also shown at once. Nothing is shown for a commit refused while the grid waits for an answer.
     */
    @RequiredUIAccess
    void showError() {
        QWidget editor = getEditor();
        DataGridEditSession session = mySession;
        UnparsedValue.ParsingError error = session == null ? null : session.getError();
        if (editor == null || error == null) {
            return;
        }

        myErrorShown = true;

        String border = TargetQt.to(ComponentColors.ERROR_BORDER).name();
        editor.setStyleSheet("#" + EDITOR_NAME + " { border: 1px solid " + border + "; }");
        setErrorHighlighting(editor, error.offset());
        editor.setToolTip(error.message());

        // a focus in hides a tooltip, so the focus comes first
        if (!isEditorFocused()) {
            editor.setFocus();
        }
        showToolTip(editor, error.message());
    }

    private void clearError(QWidget editor) {
        if (!myErrorShown || editor != getEditor()) {
            return;
        }

        myErrorShown = false;
        editor.setStyleSheet("");
        editor.setToolTip("");
        setErrorHighlighting(editor, -1);
        QToolTip.hideText();
    }

    /**
     * The wavy error underline of the text from the offset of the error to its end: extra selections of a plain text edit; the
     * formats of an input method event for a line edit, which has no other way to format a part of its text.
     *
     * @param offset where the highlighted text starts, or {@code -1} to remove the highlighting
     */
    private static void setErrorHighlighting(QWidget editor, int offset) {
        QTextCharFormat format = new QTextCharFormat();
        format.setUnderlineStyle(QTextCharFormat.UnderlineStyle.WaveUnderline);
        format.setUnderlineColor(TargetQt.to(ComponentColors.ERROR_FOREGROUND));

        if (editor instanceof QPlainTextEdit field) {
            if (offset < 0) {
                field.setExtraSelections(List.of());
                return;
            }

            QTextCursor cursor = new QTextCursor(field.document());
            int end = Math.max(0, field.document().characterCount() - 1);
            cursor.setPosition(Math.min(offset, end));
            cursor.setPosition(end, QTextCursor.MoveMode.KeepAnchor);
            field.setExtraSelections(List.of(new QTextEdit.ExtraSelection(cursor, format)));
        }
        else if (editor instanceof QLineEdit field) {
            if (field.isReadOnly()) {
                // a read-only line edit ignores input method events
                return;
            }

            List<QInputMethodEvent.Attribute> attributes;
            if (offset < 0) {
                attributes = List.of();
            }
            else {
                // the formats are relative to the caret
                int start = Math.min(offset, field.text().length());
                attributes = List.of(new QInputMethodEvent.Attribute(QInputMethodEvent.AttributeType.TextFormat,
                    start - field.cursorPosition(), field.text().length() - start, format));
            }
            QCoreApplication.sendEvent(field, new QInputMethodEvent("", attributes));
        }
    }

    private static void showToolTip(QWidget editor, String text) {
        QToolTip.showText(editor.mapToGlobal(new QPoint(0, editor.height())), text, editor);
    }

    // endregion
}
