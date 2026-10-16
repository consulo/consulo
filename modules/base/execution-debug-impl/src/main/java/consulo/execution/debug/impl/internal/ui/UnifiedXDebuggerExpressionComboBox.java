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
package consulo.execution.debug.impl.internal.ui;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.dataContext.UiDataProvider;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.language.Language;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.EditorComboBox;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Length;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

public class UnifiedXDebuggerExpressionComboBox extends XDebuggerEditorBase {
    private static final Length MINIMUM_WIDTH = Length.ofFont(24);

    private final EditorComboBox myComboBox;
    private XExpression myExpression;

    @RequiredUIAccess
    public UnifiedXDebuggerExpressionComboBox(
        Project project,
        XDebuggerEditorsProvider debuggerEditorsProvider,
        @Nullable String historyId,
        @Nullable XSourcePosition sourcePosition,
        boolean showEditor
    ) {
        super(project, debuggerEditorsProvider, EvaluationMode.EXPRESSION, historyId, sourcePosition);
        myExpression = XExpression.EMPTY_EXPRESSION;

        myComboBox = project.getApplication().getInstance(EditorBoxBuilderFactory.class).create(project)
            .document(createDocument(myExpression))
            .fileType(debuggerEditorsProvider.getFileType())
            .customize(this::prepareEditor)
            .buildComboBox();
        myComboBox.setMinWidth(MINIMUM_WIDTH);

        myComboBox.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(LangDataKeys.CONTEXT_LANGUAGES, new Language[]{myExpression.getLanguage()});
            sink.lazy(PsiFile.KEY, () -> PsiDocumentManager.getInstance(getProject()).getPsiFile(myComboBox.getDocument()));
        });

        ActionGroup.Builder actions = ActionGroup.newImmutableBuilder();
        addActions(actions, showEditor);

        if (!actions.isEmpty()) {
            ActionToolbar toolbar = ActionToolbarFactory.getInstance()
                .createActionToolbar("XDebuggerExpressionComboBox", actions.build(), ActionToolbar.Style.INPLACE);
            toolbar.setTargetUIComponent(myComboBox);
            toolbar.updateActionsAsync();

            myComboBox.setSuffixComponent(toolbar.getUIComponent());
        }

        fillComboBox();
    }

    public EditorComboBox getComboBox() {
        return myComboBox;
    }

    public Component getUIComponent() {
        return myComboBox;
    }

    @Override
    public JComponent getComponent() {
        return (JComponent) TargetAWT.to(myComboBox);
    }

    @Override
    public @Nullable Editor getEditor() {
        return myComboBox.getEditor();
    }

    @RequiredUIAccess
    public void setEnabled(boolean enable) {
        if (enable == myComboBox.isEnabled()) {
            return;
        }

        if (!enable) {
            myExpression = getExpression();
        }
        myComboBox.setEnabled(enable);
    }

    @Override
    protected void onHistoryChanged() {
        fillComboBox();
    }

    @RequiredUIAccess
    private void fillComboBox() {
        List<String> items = new ArrayList<>();
        for (XExpression expression : getRecentExpressions()) {
            String text = expression.getExpression();
            if (!items.contains(text)) {
                items.add(text);
            }
        }
        myComboBox.setItems(items);
    }

    @RequiredUIAccess
    @Override
    protected void doSetText(XExpression text) {
        myExpression = text;
        myComboBox.setDocument(createDocument(text), getFileType(text));
    }

    @Override
    public XExpression getExpression() {
        return getEditorsProvider().createExpression(getProject(), myComboBox.getDocument(), myExpression.getLanguage(), myExpression.getMode());
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        Editor editor = myComboBox.getEditor();
        return editor != null ? editor.getContentComponent() : null;
    }

    @RequiredUIAccess
    @Override
    public void requestFocusInEditor() {
        myComboBox.focus();
    }

    @RequiredUIAccess
    @Override
    public void selectAll() {
        myComboBox.selectAll();
    }

    @Override
    protected void prepareEditor(EditorEx editor) {
        super.prepareEditor(editor);
        editor.putUserData(DebuggerCopyPastePreprocessor.REMOVE_NEWLINES_ON_PASTE, true);
        XDebuggerEditorBase.foldNewLines(editor);
    }
}
