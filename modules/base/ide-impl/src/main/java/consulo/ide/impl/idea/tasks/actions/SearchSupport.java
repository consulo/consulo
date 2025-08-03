/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.ide.impl.idea.tasks.actions;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.VisualPosition;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.task.Task;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.SortedListModel;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public abstract class SearchSupport<T extends Task> {
    protected EditorTextField myTextField;
    protected JBPopup myCurrentPopup;
    protected JList<T> myList = new JBList<>();
    protected boolean myCancelled;

    private final ActionListener myCancelAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (myCurrentPopup != null) {
                myCancelled = true;
                hideCurrentPopup();
            }
        }
    };
    private T myResult;
    private final SortedListModel<T> myListModel;
    private boolean myAutoPopup;

    public SearchSupport(EditorTextField textField) {
        myTextField = textField;
        myTextField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent event) {
                onTextChanged();
            }
        });

        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(() -> myTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                processListSelection(e);
            }
        }));

        myList.setVisibleRowCount(10);
        myListModel = new SortedListModel<>(null);
        myList.setModel(myListModel);
    }

    public void setAutoPopup(boolean autoPopup) {
        myAutoPopup = autoPopup;
    }

    public void setListRenderer(ListCellRenderer<T> renderer) {
        myList.setCellRenderer(renderer);
    }

    protected String getText() {
        return myTextField.getText();
    }

    protected void onTextChanged() {
        if (myResult != null && !myResult.toString().equals(myTextField.getText())) {
            myResult = null;
        }
        if (myCancelled) {
            return;
        }
        if (isPopupShowing() || myAutoPopup) {
            showPopup(false);
        }
        else {
            hideCurrentPopup();
        }
    }

    protected abstract List<T> getItems(String text);

    @RequiredUIAccess
    private void processListSelection(KeyEvent e) {
        if (togglePopup(e)) {
            return;
        }

        if (!isPopupShowing()) {
            return;
        }

        InputMap map = myTextField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (map != null && map.get(KeyStroke.getKeyStrokeForEvent(e)) instanceof Action action && action.isEnabled()) {
            action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "action"));
            e.consume();
            return;
        }

        Object action = getAction(e, myList);

        if ("selectNextRow".equals(action)) {
            if (ensureSelectionExists()) {
                ScrollingUtil.moveDown(myList, e.getModifiersEx());
            }
        }
        else if ("selectPreviousRow".equals(action)) {
            ScrollingUtil.moveUp(myList, e.getModifiersEx());
        }
        else if ("scrollDown".equals(action)) {
            ScrollingUtil.movePageDown(myList);
        }
        else if ("scrollUp".equals(action)) {
            ScrollingUtil.movePageUp(myList);
        }
        else if ((e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) && e.getModifiers() == 0) {
            hideCurrentPopup();
            e.consume();
            myCancelled = true;
            processChosenFromCompletion();
        }
    }

    @Nullable
    public T getResult() {

        return myResult;
    }

    private boolean togglePopup(KeyEvent e) {
        KeyStroke stroke = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
        Object action = ((InputMap) UIManager.get("ComboBox.ancestorInputMap")).get(stroke);
        if ("selectNext".equals(action)) {
            if (!isPopupShowing() && myAutoPopup) {
                showPopup(true);
                return true;
            }
            else {
                return false;
            }
        }
        else if ("togglePopup".equals(action)) {
            if (isPopupShowing()) {
                hideCurrentPopup();
            }
            else {
                showPopup(true);
            }
            return true;
        }
        else {
            Keymap active = KeymapManager.getInstance().getActiveKeymap();
            String[] ids = active.getActionIds(stroke);
            if (ids.length > 0 && "CodeCompletion".equals(ids[0])) {
                showPopup(true);
            }
        }

        return false;
    }

    private void showPopup(boolean explicit) {
        myCancelled = false;

        List<T> list = getItems(myTextField.getText());
        myListModel.clear();
        myListModel.addAll(list);

        if (list.isEmpty()) {
            if (explicit) {
                showNoSuggestions();
            }
            else {
                hideCurrentPopup();
            }
            return;
        }

        ensureSelectionExists();

        myList.setPrototypeCellValue(null);
        if (isPopupShowing()) {
            adjustPopupSize();
            return;
        }

        hideCurrentPopup();

        PopupChooserBuilder builder = new PopupChooserBuilder<>(myList);
        builder.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(@Nonnull LightweightWindowEvent event) {
                myTextField.registerKeyboardAction(
                    myCancelAction,
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
                );
            }

            @Override
            public void onClosed(@Nonnull LightweightWindowEvent event) {
                myTextField.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
            }
        });
        myCurrentPopup = builder.setRequestFocus(false)
            .setAutoSelectIfEmpty(false)
            .setResizable(false)
            .setCancelCallback(() -> {
                int caret = myTextField.getCaretModel().getOffset();
                getEditor().getSelectionModel().setSelection(caret, caret);
                myTextField.setFocusTraversalKeysEnabled(true);
                Application.get().invokeLater(() -> myTextField.requestFocus());
                return Boolean.TRUE;
            })
            .setItemChoosenCallback(this::processChosenFromCompletion)
            .setCancelKeyEnabled(false)
            .setAlpha(0.1f)
            .setFocusOwners(new Component[]{myTextField}).
            createPopup();

        adjustPopupSize();
        showPopup();
    }

    private void adjustPopupSize() {
        Dimension size = myList.getPreferredSize();
        int cellHeight = myList.getCellRenderer().getListCellRendererComponent(myList, myList.getModel().getElementAt(0), 0, false, false)
            .getPreferredSize().height;
        int height = Math.min(size.height, Math.min(myList.getModel().getSize(), 12) * cellHeight);
        myCurrentPopup.setSize(new Dimension(size.width + 28, height + 12));
    }

    private void showPopup() {
        Point point = getPopupLocation();
        if (myCurrentPopup.isVisible()) {
            myCurrentPopup.setLocation(point);
        }
        else {
            myCurrentPopup.showInScreenCoordinates(myTextField, point);
        }
    }

    protected Point getPopupLocation() {
        VisualPosition visualPosition = getEditor().offsetToVisualPosition(getEditor().getCaretModel().getOffset());
        Point point = getEditor().visualPositionToXY(visualPosition);
        SwingUtilities.convertPointToScreen(point, getEditor().getComponent());
        point.y += 2;
        return point;
    }

    private void showNoSuggestions() {
        hideCurrentPopup();
        JComponent message = HintUtil.createErrorLabel(IdeLocalize.fileChooserCompletionNoSuggestions().get());
        myCurrentPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(message, message)
            .setRequestFocus(false)
            .setResizable(false)
            .setAlpha(0.1f)
            .setFocusOwners(new Component[]{myTextField})
            .createPopup();
        showPopup();
    }


    @RequiredUIAccess
    private void processChosenFromCompletion() {
        //noinspection unchecked
        myResult = myList.getSelectedValue();
        if (myResult != null) {
            onItemChosen(myResult);
        }
        hideCurrentPopup();
    }

    @RequiredUIAccess
    protected void onItemChosen(T result) {
        myTextField.setText(result.getPresentableName());
    }

    private void hideCurrentPopup() {
        if (myCurrentPopup != null) {
            myCurrentPopup.cancel();
            myCurrentPopup = null;
        }
    }

    private boolean ensureSelectionExists() {
        if (myList.getSelectedIndex() < 0 || myList.getSelectedIndex() >= myList.getModel().getSize()) {
            if (myList.getModel().getSize() >= 0) {
                myList.setSelectedIndex(0);
                return false;
            }
        }

        return true;
    }

    private boolean isPopupShowing() {
        return myCurrentPopup != null && myList != null && myList.isShowing();
    }

    private static Object getAction(KeyEvent e, JComponent comp) {
        KeyStroke stroke = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
        return comp.getInputMap().get(stroke);
    }

    protected Editor getEditor() {
        Editor editor = myTextField.getEditor();
        assert editor != null;
        return editor;
    }
}
