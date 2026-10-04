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
package consulo.desktop.awt.ui.impl;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.AbstractTableCellEditor;
import consulo.ui.ex.awt.ComponentWithEmptyText;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBTextArea;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.internal.DataGridController;
import consulo.ui.internal.DataGridEditSession;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;
import java.util.List;
import java.util.function.Predicate;

/**
 * The table cell editor of a {@link DesktopDataGridImpl} cell.
 * <p/>
 * The {@link DataGridController} opens the edit ({@link DataGridEditSession}) and owns its {@code GridCellEditor}, which has no
 * widget of its own: this editor builds the widget its {@link GridCellEditorPresentation} describes, reports every change of the text
 * to the session, and maps {@link #stopCellEditing()} and {@link #cancelCellEditing()} to {@link DataGridController#commitEditing()}
 * and {@link DataGridController#discardEditing()}.
 * <p/>
 * What started the edit comes with {@link #isCellEditable(EventObject)}. The controller and its session keep the value an edit starts
 * with, decide whether the focus moves on, which cells a multi-cell edit writes and repaints
 * ({@link DataGridController.View#cellsChanged}), and refuse a change to a read-only grid. The editor keeps the width of its cell.
 * <p/>
 * The widgets take the font of the table: the plain font of the editor colours scheme.
 */
class GridTableCellEditor extends AbstractTableCellEditor {
    public static final String TABLE_CELL_EDITOR_PROPERTY = "tableCellEditor";

    /**
     * How the text which failed to parse is highlighted.
     */
    private static final TextAttributesKey GRID_ERROR_VALUE = TextAttributesKey.of("GRID_ERROR_VALUE", CodeInsightColors.ERRORS_ATTRIBUTES);
    /**
     * The client property the look and feel (FlatLaf among others) reads the outline of a component from.
     */
    private static final String OUTLINE_PROPERTY = "JComponent.outline";
    private static final String OUTLINE_ERROR = "error";
    /**
     * A multi-line editor grows over the rows below its cell up to this number of lines, and scrolls beyond.
     */
    private static final int MAX_VISIBLE_LINES = 4;

    private static final String STOP_EDITING_ACTION = "consulo.dataGrid.stopEditing";
    private static final String INSERT_NEW_LINE_ACTION = "consulo.dataGrid.insertNewLine";

    private final DataGridController myController;
    private final int myViewRow;
    private final int myViewColumn;
    private final GridEditInitiator myActionInitiator;

    private @Nullable DataGridEditSession mySession;
    private boolean myStartedByMouse;
    private @Nullable JComponent myComponent;
    private @Nullable JTextComponent myTextComponent;
    private @Nullable JComponent myOutlineComponent;
    private @Nullable GridCellEditorComponentWrapper myWrapper;
    private @Nullable Object myErrorHighlight;
    private @Nullable String myToolTipText;
    private @Nullable JBPopup myPopup;
    private boolean myPopupShown;
    private boolean myOptionChosen;
    /**
     * The last {@link #stopCellEditing()} was refused while the grid waits for the user's answer.
     */
    private boolean myStopWaitsForAnswer;

    /**
     * @param viewRow         the row of the cell, a view row of the controller
     * @param viewColumn      the column of the cell, a view column of the controller
     * @param actionInitiator what started the editing when the table passes no event ({@code TableUtil.editCellAt})
     */
    GridTableCellEditor(DataGridController controller, int viewRow, int viewColumn, GridEditInitiator actionInitiator) {
        myController = controller;
        myViewRow = viewRow;
        myViewColumn = viewColumn;
        myActionInitiator = actionInitiator;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getTableCellEditorComponent(JTable table, @Nullable Object value, boolean isSelected, int row, int column) {
        DataGridEditSession session = mySession;
        if (session == null || myController.getEditSession() != session) {
            return null;
        }

        JComponent component = myComponent;
        if (component == null) {
            component = createComponent(table, session);
            myComponent = component;

            table.addPropertyChangeListener(TABLE_CELL_EDITOR_PROPERTY, new PropertyChangeListener() {
                @Override
                @RequiredUIAccess
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getOldValue() == GridTableCellEditor.this && evt.getNewValue() != GridTableCellEditor.this) {
                        table.removePropertyChangeListener(TABLE_CELL_EDITOR_PROPERTY, this);
                        // the controller owns the editor: an edit the table dropped without stopping or cancelling it is
                        // discarded
                        if (myController.getEditSession() == session) {
                            myController.discardEditing();
                        }
                        closePopup();
                    }
                }
            });
        }
        return component;
    }

    @Override
    public @Nullable Object getCellEditorValue() {
        DataGridEditSession session = mySession;
        return session != null && myController.getEditSession() == session ? session.getEditor().getValue() : null;
    }

    /**
     * A mouse starts the editing with a double click.
     * <p/>
     * The edit is opened here, right before the table asks for the component: an editor which decided its value when it was created
     * ({@link GridCellEditorPresentation.Kind#NONE} - a boolean set by a typed key, or toggled in check box mode) is committed by the
     * controller at once, and the table opens no editor for it.
     */
    @Override
    @RequiredUIAccess
    public boolean isCellEditable(@Nullable EventObject e) {
        if (e instanceof MouseEvent mouseEvent && mouseEvent.getClickCount() < 2) {
            return false;
        }

        DataGridEditSession session = mySession;
        if (session != null && myController.getEditSession() == session) {
            return true;
        }
        GridEditInitiator initiator = toInitiator(e);
        myStartedByMouse = initiator.kind() == GridEditInitiator.Kind.MOUSE;
        mySession = myController.startEditing(myViewRow, myViewColumn, initiator);
        return mySession != null;
    }

    /**
     * Commits through {@link DataGridController#commitEditing()}, which refuses a change to a read-only grid, lets
     * {@code GridCellEditor.stop()} refuse the text, and makes sure that no unsubmitted change is lost. A refused commit keeps the
     * editor open, and shows why.
     */
    @Override
    @RequiredUIAccess
    public boolean stopCellEditing() {
        myStopWaitsForAnswer = false;
        DataGridEditSession session = mySession;
        if (session != null && myController.getEditSession() == session && !myController.commitEditing()) {
            if (myController.getEditSession() == session) {
                myStopWaitsForAnswer = session.isWaitingForAnswer();
                setError(session.getError());
            }
            return false;
        }
        closePopup();
        return super.stopCellEditing();
    }

    /**
     * A cancel right after a stop which was refused because the grid asks whether to ignore unsubmitted changes is ignored: it is
     * the "stop, else cancel" of {@code JBTable.MyCellEditorRemover} as the focus leaves the table, and the answer goes on with the
     * edit ({@link DataGridEditSession#isWaitingForAnswer()}). {@link DesktopDataGridImpl} removes an editor which does not cede when
     * the grid itself cancels the edit.
     */
    @Override
    @RequiredUIAccess
    public void cancelCellEditing() {
        DataGridEditSession session = mySession;
        boolean waitsForAnswer = myStopWaitsForAnswer;
        myStopWaitsForAnswer = false;
        if (session != null && myController.getEditSession() == session) {
            if (waitsForAnswer && session.isWaitingForAnswer()) {
                return;
            }
            myController.discardEditing();
        }
        closePopup();
        super.cancelCellEditing();
    }

    private GridEditInitiator toInitiator(@Nullable EventObject e) {
        if (e instanceof KeyEvent keyEvent && keyEvent.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
            return GridEditInitiator.typed(String.valueOf(keyEvent.getKeyChar()));
        }
        if (e instanceof MouseEvent) {
            return GridEditInitiator.MOUSE;
        }
        return myActionInitiator;
    }

    // region widgets

    @RequiredUIAccess
    private JComponent createComponent(JTable table, DataGridEditSession session) {
        GridCellEditorPresentation presentation = session.getPresentation();
        return switch (presentation.kind()) {
            case TEXT -> createTextField(table, session, presentation);
            case MULTILINE_TEXT -> createTextArea(table, session, presentation);
            case LIST -> createListComponent(table, session, presentation);
            // the controller commits such an editor itself and opens no edit for it
            case NONE -> createLabel(table, presentation);
        };
    }

    @RequiredUIAccess
    private JComponent createTextField(JTable table, DataGridEditSession session, GridCellEditorPresentation presentation) {
        JBTextField field = new JBTextField();
        field.setHorizontalAlignment(DesktopDataGridImpl.toSwingAlignment(presentation.alignment()));
        // the text starts where the renderer puts it: its border and its insets
        field.setBorder(new CellEditorBorder(JBUI.insets(1, 6)));
        myOutlineComponent = field;
        setUpTextComponent(table, session, field, presentation, false);
        return wrap(session, field, field, null);
    }

    @RequiredUIAccess
    private JComponent createTextArea(JTable table, DataGridEditSession session, GridCellEditorPresentation presentation) {
        JBTextArea area = new JBTextArea();
        area.setBorder(JBUI.Borders.empty(2, 5));
        setUpTextComponent(table, session, area, presentation, true);

        // no horizontal scroll bar: in a box of one row it would cover the text; the viewport follows the caret anyway
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(area, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(new CellEditorBorder(JBUI.insets(1)));
        scrollPane.getViewport().setBackground(area.getBackground());
        myOutlineComponent = scrollPane;
        return wrap(session, scrollPane, area, area);
    }

    /**
     * An editor whose component may span columns is put into a {@link GridCellEditorComponentWrapper}. A multi-line editor is always
     * wrapped: the wrapper gives the focus to the text area inside the scroll pane, and grows it over the rows below.
     */
    private JComponent wrap(DataGridEditSession session, JComponent component, JTextComponent text, @Nullable JTextArea multilineArea) {
        myTextComponent = text;
        if (!session.getEditor().isColumnSpanAllowed() && multilineArea == null) {
            return component;
        }
        GridCellEditorComponentWrapper wrapper = new GridCellEditorComponentWrapper(component, text, multilineArea);
        myWrapper = wrapper;
        return wrapper;
    }

    /**
     * Sets up a text widget - its font, colours, text, caret, placeholder and keys - and listens to its document.
     */
    @RequiredUIAccess
    private void setUpTextComponent(JTable table,
                                    DataGridEditSession session,
                                    JTextComponent text,
                                    GridCellEditorPresentation presentation,
                                    boolean multiline) {
        text.setFont(table.getFont());
        text.setForeground(table.getForeground());
        text.setBackground(table.getBackground());
        text.setText(presentation.text());
        // a read-only cell shows its text, which can be selected and copied but not changed
        text.setEditable(!presentation.readOnly());
        if (presentation.readOnly() && !presentation.readOnlyHint().isEmpty()) {
            myToolTipText = presentation.readOnlyHint().get();
            text.setToolTipText(myToolTipText);
        }

        // the caret at the start, and the whole text selected when the editor is editable and single-line
        text.setCaretPosition(0);
        if (presentation.selectAll()) {
            selectAll(text);
            if (myStartedByMouse) {
                // BasicTableUI passes the double click on to the field, which puts the caret where it was clicked: the text is
                // selected again once the press is handled
                SwingUtilities.invokeLater(() -> {
                    if (myController.getEditSession() == session && text.isShowing()) {
                        selectAll(text);
                    }
                });
            }
        }

        // the display name of the null value is shown while the field is empty, also while it has the focus
        if (!presentation.placeholder().isEmpty() && text instanceof ComponentWithEmptyText withEmptyText) {
            // a value, not a text to translate
            withEmptyText.getEmptyText().setText(LocalizeValue.of(presentation.placeholder()));
            text.putClientProperty("StatusVisibleFunction", (Predicate<JTextComponent>) c -> c.getText().isEmpty());
        }

        registerEnterAction(text, multiline);
        registerTabAction(text, table);

        text.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            @RequiredUIAccess
            protected void textChanged(DocumentEvent e) {
                onTextChanged(session, text);
            }
        });
    }

    /**
     * The caret stays at the start, so a long text shows its beginning.
     */
    private static void selectAll(JTextComponent text) {
        text.setCaretPosition(text.getDocument().getLength());
        text.moveCaretPosition(0);
    }

    /**
     * A change of the text clears the error and passes the new text to the session.
     */
    @RequiredUIAccess
    private void onTextChanged(DataGridEditSession session, JTextComponent text) {
        if (myController.getEditSession() != session) {
            return;
        }
        setError(null);
        session.setText(text.getText());

        GridCellEditorComponentWrapper wrapper = myWrapper;
        if (wrapper != null) {
            wrapper.updateBounds();
        }
    }

    /**
     * Enter and Ctrl+Enter stop the editing - a multi-line editor inserts a line break on Ctrl+Enter instead. A {@link JTextComponent}
     * inserts no line break on Ctrl+Enter by itself, so it is inserted here - also on Cmd+Enter and Shift+Enter.
     */
    private void registerEnterAction(JTextComponent text, boolean multiline) {
        ActionMap actionMap = text.getActionMap();
        actionMap.put(STOP_EDITING_ACTION, new AbstractAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                myController.stopEditing();
            }
        });
        actionMap.put(INSERT_NEW_LINE_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (text.isEditable()) {
                    text.replaceSelection("\n");
                }
            }
        });

        InputMap inputMap = text.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), STOP_EDITING_ACTION);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
            multiline ? INSERT_NEW_LINE_ACTION : STOP_EDITING_ACTION);
        if (multiline) {
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK), INSERT_NEW_LINE_ACTION);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), INSERT_NEW_LINE_ACTION);
        }
    }

    /**
     * Tab and Shift+Tab perform the action the table binds to them, which stops the editing and moves to the next or the previous
     * cell.
     */
    private static void registerTabAction(JTextComponent text, JTable table) {
        // a text field gives Tab to the focus traversal otherwise
        text.setFocusTraversalKeysEnabled(false);
        registerTableAction(text, table, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        registerTableAction(text, table, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
    }

    private static void registerTableAction(JTextComponent text, JTable table, KeyStroke stroke) {
        String key = "consulo.dataGrid.tableAction " + stroke;
        text.getInputMap(JComponent.WHEN_FOCUSED).put(stroke, key);
        text.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object actionKey = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(stroke);
                Action tableAction = actionKey == null ? null : table.getActionMap().get(actionKey);
                if (tableAction != null) {
                    tableAction.actionPerformed(new ActionEvent(table, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers()));
                }
            }
        });
    }

    /**
     * The component shows the value of the cell, and the list of the options pops up under it when it gets the focus.
     */
    @RequiredUIAccess
    private JComponent createListComponent(JTable table, DataGridEditSession session, GridCellEditorPresentation presentation) {
        JBLabel label = createLabel(table, presentation);
        label.setFocusable(true);
        label.setRequestFocusEnabled(true);
        label.addFocusListener(new FocusAdapter() {
            @Override
            @RequiredUIAccess
            public void focusGained(FocusEvent e) {
                if (!myPopupShown) {
                    myPopupShown = true;
                    showPopup(table, label, session, presentation);
                }
            }
        });
        return label;
    }

    private static JBLabel createLabel(JTable table, GridCellEditorPresentation presentation) {
        JBLabel label = new JBLabel(presentation.text());
        label.setOpaque(true);
        label.setFont(table.getFont());
        label.setForeground(table.getSelectionForeground());
        label.setBackground(table.getSelectionBackground());
        label.setBorder(JBUI.Borders.merge(JBCurrentTheme.listCellBorder(), JBUI.Borders.empty(0, 2), false));
        label.setHorizontalAlignment(DesktopDataGridImpl.toSwingAlignment(presentation.alignment()));
        return label;
    }

    /**
     * Shows the options as a list of actions with speed search, under the cell. A chosen option applies its value and ends the
     * editing; a list closed without a choice cancels it.
     */
    @RequiredUIAccess
    private void showPopup(JTable table, JComponent owner, DataGridEditSession session, GridCellEditorPresentation presentation) {
        if (myController.getEditSession() != session) {
            return;
        }

        List<String> labels = presentation.options();
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        for (int i = 0; i < labels.size(); i++) {
            int index = i;
            group.add(DumbAwareAction.create(LocalizeValue.of(labels.get(i)), e -> onOptionChosen(table, session, index)));
        }

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null,
            group.build(),
            DataManager.getInstance().getDataContext(owner),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true);
        popup.addListener(new JBPopupListener() {
            @Override
            @RequiredUIAccess
            public void onClosed(LightweightWindowEvent event) {
                onPopupClosed(table, session, event.isOk());
            }
        });
        myPopup = popup;
        popup.showUnderneathOf(owner);
    }

    @RequiredUIAccess
    private void onOptionChosen(JTable table, DataGridEditSession session, int index) {
        if (myOptionChosen || myController.getEditSession() != session) {
            return;
        }
        myOptionChosen = true;
        session.selectOption(index);
        closePopup();

        // a refused commit cancels the edit - unless it waits for the user's answer, which goes on with it
        if (!myController.stopEditing() && myController.getEditSession() == session && !session.isWaitingForAnswer()) {
            myController.cancelEditing();
        }
        focusTable(table);
    }

    /**
     * @param chosen whether an option was chosen - its action runs once the list is closed
     */
    @RequiredUIAccess
    private void onPopupClosed(JTable table, DataGridEditSession session, boolean chosen) {
        myPopup = null;
        if (myOptionChosen || chosen) {
            return;
        }
        // closed without a choice: the edit is cancelled
        if (myController.getEditSession() == session) {
            myController.cancelEditing();
        }
        focusTable(table);
    }

    private void closePopup() {
        JBPopup popup = myPopup;
        myPopup = null;
        if (popup != null && !popup.isDisposed()) {
            popup.cancel();
        }
    }

    /**
     * The focus goes back to the table once the list is gone - unless the user put it elsewhere meanwhile.
     */
    private static void focusTable(JTable table) {
        IdeFocusManager focusManager = IdeFocusManager.getGlobalInstance();
        focusManager.doWhenFocusSettlesDown(() -> {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner == null || SwingUtilities.isDescendingFrom(focusOwner, table)) {
                focusManager.requestFocus(table, true);
            }
        });
    }

    // endregion

    // region error

    /**
     * The editor gets a red outline, and the text from the offset of the error to the end is highlighted as an error, with the
     * message as its tooltip.
     */
    private void setError(UnparsedValue.@Nullable ParsingError error) {
        JTextComponent text = myTextComponent;
        if (text == null) {
            return;
        }
        setEditorOutline(error == null ? null : OUTLINE_ERROR);
        setHighlighting(text, error);
        text.setToolTipText(error == null ? myToolTipText : error.message());
    }

    private void setEditorOutline(@Nullable String outline) {
        JComponent component = myOutlineComponent;
        if (component != null && component.getClientProperty(OUTLINE_PROPERTY) != outline) {
            component.putClientProperty(OUTLINE_PROPERTY, outline);
            component.repaint();
        }
    }

    private void setHighlighting(JTextComponent text, UnparsedValue.@Nullable ParsingError error) {
        Highlighter highlighter = text.getHighlighter();
        Object highlight = myErrorHighlight;
        myErrorHighlight = null;
        if (highlight != null) {
            highlighter.removeHighlight(highlight);
        }
        if (error == null) {
            return;
        }

        int length = text.getDocument().getLength();
        int start = Math.max(0, Math.min(error.offset(), length));
        try {
            TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(GRID_ERROR_VALUE);
            myErrorHighlight = highlighter.addHighlight(start, length, new ErrorHighlightPainter(attributes));
        }
        catch (BadLocationException ignored) {
            // the range is within the document
        }
    }

    /**
     * Paints a range the way the editor paints {@link #GRID_ERROR_VALUE}: its background, and its effect - a wave by default.
     */
    private static final class ErrorHighlightPainter extends LayeredHighlighter.LayerPainter {
        private final @Nullable Color myBackground;
        private final Color myEffectColor;
        private final boolean myWaved;

        ErrorHighlightPainter(@Nullable TextAttributes attributes) {
            ColorValue background = attributes == null ? null : attributes.getBackgroundColor();
            ColorValue effectColor = attributes == null ? null : attributes.getEffectColor();
            EffectType effectType = attributes == null ? null : attributes.getEffectType();
            myBackground = background == null ? null : TargetAWT.to(background);
            myEffectColor = TargetAWT.to(effectColor == null ? ComponentColors.ERROR_FOREGROUND : effectColor);
            myWaved = effectColor == null || effectType == null || effectType == EffectType.WAVE_UNDERSCORE;
        }

        @Override
        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
            // DefaultHighlighter paints a layered painter view by view, with paintLayer
        }

        @Override
        public @Nullable Shape paintLayer(Graphics g, int offs0, int offs1, Shape viewBounds, JTextComponent editor, View view) {
            if (offs0 >= offs1) {
                return null;
            }

            Rectangle r;
            try {
                r = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, viewBounds).getBounds();
            }
            catch (BadLocationException e) {
                return null;
            }

            if (myBackground != null) {
                g.setColor(myBackground);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
            g.setColor(myEffectColor);
            int effectHeight = JBUI.scale(3);
            if (myWaved && g instanceof Graphics2D g2) {
                UIUtil.drawWave(g2, new Rectangle(r.x, r.y + r.height - effectHeight, r.width, effectHeight));
            }
            else {
                g.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1);
            }
            return r;
        }
    }

    /**
     * The border of an editor in a cell: no frame, only the red outline set through {@link #OUTLINE_PROPERTY} while the text has an
     * error.
     */
    private static final class CellEditorBorder implements Border {
        private final Insets myInsets;

        CellEditorBorder(Insets insets) {
            myInsets = insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (c instanceof JComponent component && OUTLINE_ERROR.equals(component.getClientProperty(OUTLINE_PROPERTY))) {
                g.setColor(TargetAWT.to(ComponentColors.ERROR_BORDER));
                int thickness = Math.max(1, JBUI.scale(1));
                for (int i = 0; i < thickness; i++) {
                    g.drawRect(x + i, y + i, width - 1 - 2 * i, height - 1 - 2 * i);
                }
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(myInsets.top, myInsets.left, myInsets.bottom, myInsets.right);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    // endregion

    /**
     * Holds the component of the editor, gives the focus to its text component, and passes on to it the keys the table forwards
     * while it keeps the focus itself.
     * <p/>
     * A multi-line editor covers the rows below its cell while its text has more than one line, up to {@link #MAX_VISIBLE_LINES}
     * lines - the rows keep their height.
     */
    private static final class GridCellEditorComponentWrapper extends JComponent {
        private final JComponent myComponent;
        private final JTextComponent myFocusComponent;
        private final @Nullable JTextArea myMultilineArea;
        private final Rectangle myCellBounds = new Rectangle();

        private @Nullable KeyEvent myCurrentEvent;

        GridCellEditorComponentWrapper(JComponent component, JTextComponent focusComponent, @Nullable JTextArea multilineArea) {
            myComponent = component;
            myFocusComponent = focusComponent;
            myMultilineArea = multilineArea;
            setLayout(new BorderLayout());
            add(component, BorderLayout.CENTER);
            setFocusable(false);
        }

        /**
         * The table puts the editor into the bounds of its cell - when it opens it, and each time it paints the cell.
         */
        @Override
        public void setBounds(int x, int y, int width, int height) {
            myCellBounds.setBounds(x, y, width, height);
            Rectangle bounds = getEditorBounds();
            super.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        /**
         * The number of lines changed: a multi-line editor follows it.
         */
        void updateBounds() {
            if (myMultilineArea == null) {
                return;
            }
            Rectangle old = getBounds();
            setBounds(myCellBounds.x, myCellBounds.y, myCellBounds.width, myCellBounds.height);
            validate();
            Rectangle bounds = getBounds();
            if (!old.equals(bounds) && getParent() instanceof JComponent parent) {
                parent.scrollRectToVisible(bounds);
            }
        }

        /**
         * The cell, or for a multi-line text the cell grown down over the rows below it - or up, when the table ends below.
         */
        private Rectangle getEditorBounds() {
            Rectangle cell = new Rectangle(myCellBounds);
            JTextArea area = myMultilineArea;
            if (area == null) {
                return cell;
            }
            int lines = Math.min(area.getLineCount(), MAX_VISIBLE_LINES);
            if (lines <= 1) {
                return cell;
            }

            Insets outer = myComponent.getInsets();
            Insets inner = area.getInsets();
            int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
            int height = Math.max(cell.height, lines * lineHeight + inner.top + inner.bottom + outer.top + outer.bottom);
            int y = cell.y;
            Container parent = getParent();
            if (parent != null && y + height > parent.getHeight()) {
                y = Math.max(0, parent.getHeight() - height);
            }
            return new Rectangle(cell.x, y, cell.width, height);
        }

        @Override
        public void requestFocus() {
            IdeFocusManager.getGlobalInstance()
                .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myFocusComponent, true));
        }

        @Override
        protected final boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            if (condition == WHEN_FOCUSED && myCurrentEvent != e) {
                try {
                    myCurrentEvent = e;
                    myFocusComponent.dispatchEvent(e);
                }
                finally {
                    myCurrentEvent = null;
                }
            }
            return e.isConsumed() || super.processKeyBinding(ks, e, condition, pressed);
        }
    }
}
