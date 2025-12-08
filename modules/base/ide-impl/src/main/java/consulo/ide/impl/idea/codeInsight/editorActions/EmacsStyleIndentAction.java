/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.codeInsight.CodeInsightUtilBase;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.EmacsProcessingHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

@ActionImpl(id = "EmacsStyleIndent")
public class EmacsStyleIndentAction extends BaseCodeInsightAction implements DumbAware {
    private static class MyHandler implements CodeInsightActionHandler {
        @Override
        @RequiredUIAccess
        public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
            if (!CodeInsightUtilBase.prepareEditorForWrite(editor)) {
                return;
            }
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
                return;
            }

            EmacsProcessingHandler emacsProcessingHandler = EmacsProcessingHandler.forLanguage(file.getLanguage());
            EmacsProcessingHandler.Result result = emacsProcessingHandler.changeIndent(project, editor, file);
            if (result == EmacsProcessingHandler.Result.STOP) {
                return;
            }

            Document document = editor.getDocument();
            int startOffset = editor.getCaretModel().getOffset();
            int line = editor.offsetToLogicalPosition(startOffset).line;
            int col = editor.getCaretModel().getLogicalPosition().column;
            int lineStart = document.getLineStartOffset(line);
            int initLineEnd = document.getLineEndOffset(line);
            try {
                CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
                int newPos = codeStyleManager.adjustLineIndent(file, lineStart);
                int newCol = newPos - lineStart;
                int lineInc = document.getLineEndOffset(line) - initLineEnd;
                if (newCol >= col + lineInc && newCol >= 0) {
                    LogicalPosition pos = new LogicalPosition(line, newCol);
                    editor.getCaretModel().moveToLogicalPosition(pos);
                    editor.getSelectionModel().removeSelection();
                    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                }
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }

        @Override
        public boolean startInWriteAction() {
            return true;
        }
    }

    private static final Logger LOG = Logger.getInstance(EmacsStyleIndentAction.class);

    public EmacsStyleIndentAction() {
        super(ActionLocalize.actionEmacsstyleindentText(), LocalizeValue.absent());
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MyHandler();
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        PsiElement context = file.findElementAt(editor.getCaretModel().getOffset());
        return context != null && FormattingModelBuilder.forContext(context) != null;
    }
}
