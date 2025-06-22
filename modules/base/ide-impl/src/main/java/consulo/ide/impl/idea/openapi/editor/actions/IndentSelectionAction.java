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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.EditorAction;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.language.editor.IndentStrategy;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.Presentation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class IndentSelectionAction extends EditorAction {
    public IndentSelectionAction() {
        super(new Handler());
    }

    private static class Handler extends EditorWriteActionHandler {
        public Handler() {
            super(true);
        }

        @Override
        @RequiredWriteAction
        public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            Project project = dataContext.getData(Project.KEY);
            if (isEnabled(editor, caret, dataContext)) {
                indentSelection(editor, project);
            }
        }
    }

    @Override
    public void update(Editor editor, Presentation presentation, DataContext dataContext) {
        presentation.setEnabled(originalIsEnabled(editor, true));
    }

    @Override
    public void updateForKeyboardAccess(Editor editor, Presentation presentation, DataContext dataContext) {
        presentation.setEnabled(isEnabled(editor, dataContext));
    }

    protected boolean isEnabled(Editor editor, DataContext dataContext) {
        return originalIsEnabled(editor, true);
    }

    protected static boolean originalIsEnabled(Editor editor, boolean wantSelection) {
        return (!wantSelection || hasSuitableSelection(editor)) && !editor.isOneLineMode();
    }

    /**
     * Returns true if there is a selection in the editor and it contains at least one non-whitespace character
     */
    private static boolean hasSuitableSelection(Editor editor) {
        if (!editor.getSelectionModel().hasSelection()) {
            return false;
        }
        Document document = editor.getDocument();
        int selectionStart = editor.getSelectionModel().getSelectionStart();
        int selectionEnd = editor.getSelectionModel().getSelectionEnd();
        return !CharArrayUtil.containsOnlyWhiteSpaces(document.getCharsSequence().subSequence(selectionStart, selectionEnd));
    }

    private static void indentSelection(Editor editor, Project project) {
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
        if (endIndex > 0 && document.getLineStartOffset(endIndex) == oldSelectionEnd && editor.getSelectionModel().hasSelection()) {
            endIndex--;
        }
        if (endIndex == -1) {
            endIndex = document.getLineCount() - 1;
        }

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        int blockIndent = CodeStyleSettingsManager.getSettings(project).getIndentOptionsByFile(file).INDENT_SIZE;
        doIndent(endIndex, startIndex, document, project, editor, blockIndent);
    }

    static void doIndent(int endIndex, int startIndex, Document document, Project project, Editor editor, int blockIndent) {
        int[] caretOffset = {editor.getCaretModel().getOffset()};

        boolean bulkMode = endIndex - startIndex > 50;
        DocumentUtil.executeInBulk(document, bulkMode, () -> {
            List<Integer> nonModifiableLines = new ArrayList<>();
            if (project != null) {
                PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
                IndentStrategy indentStrategy = IndentStrategy.forFile(file);
                // it's not default indent strategy
                if (indentStrategy.getLanguage() != Language.ANY) {
                    for (int i = startIndex; i <= endIndex; i++) {
                        if (!canIndent(document, file, i, indentStrategy)) {
                            nonModifiableLines.add(i);
                        }
                    }
                }
            }
            for (int i = startIndex; i <= endIndex; i++) {
                if (!nonModifiableLines.contains(i)) {
                    caretOffset[0] = EditorActionUtil.indentLine(project, editor, i, blockIndent, caretOffset[0]);
                }
            }
        });

        editor.getCaretModel().moveToOffset(caretOffset[0]);
    }

    @RequiredReadAction
    static boolean canIndent(Document document, PsiFile file, int line, @Nonnull IndentStrategy indentStrategy) {
        int offset = document.getLineStartOffset(line);
        if (file != null) {
            PsiElement element = file.findElementAt(offset);
            if (element != null) {
                return indentStrategy.canIndent(element);
            }
        }
        return true;
    }
}
