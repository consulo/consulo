/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.EditorEx;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.annotation.access.RequiredWriteAction;

/**
 * @author max
 * @since 2002-05-13
 */
public class UnindentSelectionAction extends EditorAction {
    public UnindentSelectionAction() {
        super(new Handler());
    }

    private static class Handler extends EditorWriteActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, DataContext dataContext) {
            Project project = dataContext.getData(Project.KEY);
            unindentSelection(editor, project);
        }

        @Override
        public boolean isEnabled(Editor editor, DataContext dataContext) {
            return !editor.isOneLineMode() && !((EditorEx) editor).isEmbeddedIntoDialogWrapper();
        }
    }

    private static void unindentSelection(Editor editor, Project project) {
        int oldSelectionStart = editor.getSelectionModel().getSelectionStart();
        int oldSelectionEnd = editor.getSelectionModel().getSelectionEnd();
        if (!editor.getSelectionModel().hasSelection()) {
            oldSelectionStart = editor.getCaretModel().getOffset();
            oldSelectionEnd = oldSelectionStart;
        }

        Document document = editor.getDocument();
        int startIndex = document.getLineNumber(oldSelectionStart);
        if (startIndex == -1) {
            startIndex = document.getLineCount() - 1;
        }
        int endIndex = document.getLineNumber(oldSelectionEnd);
        if (endIndex > 0 && document.getLineStartOffset(endIndex) == oldSelectionEnd && endIndex > startIndex) {
            endIndex--;
        }
        if (endIndex == -1) {
            endIndex = document.getLineCount() - 1;
        }

        if (startIndex < 0 || endIndex < 0) {
            return;
        }

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);

        int blockIndent = CodeStyleSettingsManager.getSettings(project).getIndentOptionsByFile(file).INDENT_SIZE;
        IndentSelectionAction.doIndent(endIndex, startIndex, document, project, editor, -blockIndent);
    }
}
