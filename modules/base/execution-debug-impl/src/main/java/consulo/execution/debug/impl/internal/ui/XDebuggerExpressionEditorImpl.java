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
import consulo.dataContext.UiDataProvider;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.ui.XDebuggerExpressionEditor;
import consulo.language.Language;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilder;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
public class XDebuggerExpressionEditorImpl extends XDebuggerEditorBase implements XDebuggerExpressionEditor {
    private final EditorBox myEditorBox;
    private XExpression myExpression;

    @RequiredUIAccess
    public XDebuggerExpressionEditorImpl(
        Project project,
        XDebuggerEditorsProvider debuggerEditorsProvider,
        @Nullable String historyId,
        @Nullable XSourcePosition sourcePosition,
        XExpression text,
        boolean multiline,
        boolean editorFont
    ) {
        super(project, debuggerEditorsProvider, multiline ? EvaluationMode.CODE_FRAGMENT : EvaluationMode.EXPRESSION, historyId, sourcePosition);
        myExpression = XExpression.changeMode(text, getMode());

        EditorBoxBuilder builder = project.getApplication().getInstance(EditorBoxBuilderFactory.class).create(project)
            .document(createDocument(myExpression))
            .fileType(debuggerEditorsProvider.getFileType())
            .customize(this::prepareEditor);
        if (multiline) {
            builder.multiline();
        }
        if (editorFont) {
            builder.editorFont();
        }
        myEditorBox = builder.build();

        myEditorBox.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(LangDataKeys.CONTEXT_LANGUAGES, new Language[]{myExpression.getLanguage()});
            sink.lazy(PsiFile.KEY, () -> PsiDocumentManager.getInstance(getProject()).getPsiFile(myEditorBox.getDocument()));
        });

        ActionGroup.Builder actions = ActionGroup.newImmutableBuilder();
        addActions(actions, multiline);

        if (!actions.isEmpty()) {
            ActionToolbar toolbar = ActionToolbarFactory.getInstance()
                .createActionToolbar("XDebuggerExpressionEditor", actions.build(), ActionToolbar.Style.INPLACE);
            toolbar.setTargetUIComponent(myEditorBox);
            toolbar.updateActionsAsync();

            myEditorBox.setSuffixComponent(toolbar.getUIComponent());
        }
    }

    @Override
    public Component getUIComponent() {
        return myEditorBox;
    }

    @Override
    public JComponent getComponent() {
        return (JComponent) TargetAWT.to(myEditorBox);
    }

    @Override
    public JComponent getEditorComponent() {
        return getComponent();
    }

    @RequiredUIAccess
    @Override
    protected void doSetText(XExpression text) {
        myExpression = text;
        myEditorBox.setDocument(createDocument(text), getFileType(text));
    }

    @Override
    public XExpression getExpression() {
        return getEditorsProvider().createExpression(getProject(), myEditorBox.getDocument(), myExpression.getLanguage(), myExpression.getMode());
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        Editor editor = myEditorBox.getEditor();
        return editor != null ? editor.getContentComponent() : null;
    }

    @RequiredUIAccess
    @Override
    public void requestFocusInEditor() {
        myEditorBox.focus();
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean enable) {
        myEditorBox.setEnabled(enable);
    }

    @Override
    public @Nullable Editor getEditor() {
        return myEditorBox.getEditor();
    }

    @RequiredUIAccess
    @Override
    public void selectAll() {
        myEditorBox.selectAll();
    }
}
