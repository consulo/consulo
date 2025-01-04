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
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.ui.XDebuggerExpressionEditor;
import consulo.language.Language;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
public class XDebuggerExpressionEditorImpl extends XDebuggerEditorBase implements XDebuggerExpressionEditor {
    private final EditorTextField myEditorTextField;
    private XExpression myExpression;

    @RequiredUIAccess
    public XDebuggerExpressionEditorImpl(
        Project project,
        @Nonnull XDebuggerEditorsProvider debuggerEditorsProvider,
        @Nullable String historyId,
        @Nullable XSourcePosition sourcePosition,
        @Nonnull XExpression text,
        final boolean multiline,
        boolean editorFont
    ) {
        super(project, debuggerEditorsProvider, multiline ? EvaluationMode.CODE_FRAGMENT : EvaluationMode.EXPRESSION, historyId, sourcePosition);
        myExpression = XExpression.changeMode(text, getMode());
        myEditorTextField = new EditorTextField(createDocument(myExpression), project, debuggerEditorsProvider.getFileType(), false, !multiline) {
            @Override
            protected EditorEx createEditor() {
                final EditorEx editor = super.createEditor();
                editor.setVerticalScrollbarVisible(multiline);
                editor.getColorsScheme().setEditorFontName(getFont().getFontName());
                editor.getColorsScheme().setEditorFontSize(getFont().getSize());
                return editor;
            }

            @Override
            public Object getData(@Nonnull Key dataId) {
                if (LangDataKeys.CONTEXT_LANGUAGES == dataId) {
                    return new Language[]{myExpression.getLanguage()};
                }
                else if (PsiFile.KEY == dataId) {
                    return PsiDocumentManager.getInstance(getProject()).getPsiFile(getDocument());
                }
                return super.getData(dataId);
            }
        };

        if (editorFont) {
            myEditorTextField.setFontInheritedFromLAF(false);
            myEditorTextField.setFont(AWTLanguageEditorUtil.getEditorFont());
        }

        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
        addActions(builder, multiline);

        if (!builder.isEmpty()) {
            ActionToolbar toolbar = ActionToolbarFactory.getInstance()
                .createActionToolbar("XDebuggerExpressionEditor", builder.build(), ActionToolbar.Style.INPLACE);
            toolbar.setTargetComponent(myEditorTextField);
            toolbar.updateActionsImmediately();

            myEditorTextField.setSuffixComponent(toolbar.getComponent());
        }
    }

    @Override
    public JComponent getComponent() {
        return myEditorTextField;
    }

    @Override
    public JComponent getEditorComponent() {
        return myEditorTextField;
    }

    @Override
    protected void doSetText(XExpression text) {
        myExpression = text;
        myEditorTextField.setNewDocumentAndFileType(getFileType(text), createDocument(text));
    }

    @Override
    public XExpression getExpression() {
        return getEditorsProvider().createExpression(getProject(), myEditorTextField.getDocument(), myExpression.getLanguage(), myExpression.getMode());
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        final Editor editor = myEditorTextField.getEditor();
        return editor != null ? editor.getContentComponent() : null;
    }

    @Override
    public void setEnabled(boolean enable) {
        myEditorTextField.setEnabled(enable);
    }

    @Nullable
    @Override
    public Editor getEditor() {
        return myEditorTextField.getEditor();
    }

    @Override
    public void selectAll() {
        myEditorTextField.selectAll();
    }
}
