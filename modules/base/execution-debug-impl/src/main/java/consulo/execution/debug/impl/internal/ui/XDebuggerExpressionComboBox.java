/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.impl.internal.XDebuggerHistoryManagerImpl;
import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.language.editor.ui.awt.EditorComboBoxEditor;
import consulo.language.editor.ui.awt.EditorComboBoxRenderer;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @author nik
 */
public class XDebuggerExpressionComboBox extends XDebuggerEditorBase {
    private final JComponent myComponent;
    private final ComboBox<XExpression> myComboBox;
    private XDebuggerComboBoxEditor myEditor;
    private XExpression myExpression;

    public XDebuggerExpressionComboBox(@Nonnull Project project,
                                       @Nonnull XDebuggerEditorsProvider debuggerEditorsProvider,
                                       @Nullable String historyId,
                                       @Nullable XSourcePosition sourcePosition,
                                       boolean showEditor) {
        super(project, debuggerEditorsProvider, EvaluationMode.EXPRESSION, historyId, sourcePosition);
        myComboBox = new ComboBox<>(100);
        myComboBox.setEditable(true);
        myExpression = XExpression.EMPTY_EXPRESSION;
        Dimension minimumSize = new Dimension(myComboBox.getMinimumSize());
        minimumSize.width = 100;
        myComboBox.setMinimumSize(minimumSize);
        initEditor(showEditor);
        fillComboBox();
        myComponent = JBUI.Panels.simplePanel().addToTop(myComboBox);
        setExpression(myExpression);
    }

    public ComboBox getComboBox() {
        return myComboBox;
    }

    @Override
    public JComponent getComponent() {
        return myComponent;
    }

    @Nullable
    public Editor getEditor() {
        return myEditor.getEditorTextField().getEditor();
    }

    public JComponent getEditorComponent() {
        return myEditor.getEditorTextField();
    }

    public void setEnabled(boolean enable) {
        if (enable == myComboBox.isEnabled()) {
            return;
        }

        UIUtil.setEnabled(myComponent, enable, true);
        //myComboBox.setEditable(enable);

        if (enable) {
            //initEditor();
        }
        else {
            myExpression = getExpression();
        }
    }

    private void initEditor(boolean showEditor) {
        myEditor = new XDebuggerComboBoxEditor(showEditor);
        myComboBox.setEditor(myEditor);
        //myEditor.setItem(myExpression);
        myComboBox.setRenderer(new EditorComboBoxRenderer(myEditor));
        myComboBox.setMaximumRowCount(XDebuggerHistoryManagerImpl.MAX_RECENT_EXPRESSIONS);
    }

    @Override
    protected void onHistoryChanged() {
        fillComboBox();
    }

    private void fillComboBox() {
        myComboBox.removeAllItems();
        for (XExpression expression : getRecentExpressions()) {
            myComboBox.addItem(expression);
        }
        if (myComboBox.getItemCount() > 0) {
            myComboBox.setSelectedIndex(0);
        }
    }

    @Override
    protected void doSetText(XExpression text) {
        if (myComboBox.getItemCount() > 0) {
            myComboBox.setSelectedIndex(0);
        }

        //if (myComboBox.isEditable()) {
        myEditor.setItem(text);
        //}
        myExpression = text;
    }

    @Override
    public XExpression getExpression() {
        XExpression item = myEditor.getItem();
        return item != null ? item : myExpression;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myEditor.getEditorTextField();
    }

    @Override
    public void selectAll() {
        myComboBox.getEditor().selectAll();
    }

    @Override
    protected void prepareEditor(EditorEx editor) {
        super.prepareEditor(editor);
        editor.getColorsScheme().setEditorFontSize(Math.min(myComboBox.getFont().getSize(), AWTLanguageEditorUtil.getEditorFont().getSize()));
    }

    private class XDebuggerComboBoxEditor implements ComboBoxEditor {
        private final JComponent myPanel;
        private final EditorComboBoxEditor myDelegate;

        public XDebuggerComboBoxEditor(boolean showMultiline) {
            myDelegate = new EditorComboBoxEditor(getProject(), getEditorsProvider().getFileType()) {
                @Override
                protected void onEditorCreate(EditorEx editor) {
                    editor.putUserData(DebuggerCopyPastePreprocessor.REMOVE_NEWLINES_ON_PASTE, true);
                    prepareEditor(editor);
                    if (showMultiline) {
                        setExpandable(editor);
                    }
                    XDebuggerEditorBase.foldNewLines(editor);
                    editor.getColorsScheme().setEditorFontSize(myComboBox.getFont().getSize());
                }
            };
            myDelegate.getEditorComponent().setFontInheritedFromLAF(false);

            JComponent comp = myDelegate.getEditorComponent();
            comp = addChooser(comp);
            if (showMultiline) {
                comp = addExpand(comp, true);
            }
            myPanel = comp;
        }

        public EditorTextField getEditorTextField() {
            return myDelegate.getEditorComponent();
        }

        @Override
        public JComponent getEditorComponent() {
            return myPanel;
        }

        @Override
        public void setItem(Object anObject) {
            if (anObject == null) {
                anObject = XExpression.EMPTY_EXPRESSION;
            }
            XExpression expression = (XExpression) anObject;
            myDelegate.getEditorComponent().setNewDocumentAndFileType(getFileType(expression), createDocument(expression));
        }

        @Override
        public XExpression getItem() {
            Object document = myDelegate.getItem();
            if (document instanceof Document) { // sometimes null on Mac
                return getEditorsProvider().createExpression(getProject(), (Document) document, myExpression.getLanguage(), myExpression.getMode());
            }
            return null;
        }

        @Override
        public void selectAll() {
            myDelegate.selectAll();
        }

        @Override
        public void addActionListener(ActionListener l) {
            myDelegate.addActionListener(l);
        }

        @Override
        public void removeActionListener(ActionListener l) {
            myDelegate.removeActionListener(l);
        }
    }
}
