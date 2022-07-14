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

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.application.Application;
import consulo.language.editor.CodeInsightSettings;
import consulo.dataContext.DataManager;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.document.FileDocumentManager;
import consulo.codeEditor.*;
import consulo.language.editor.EditorNavigationDelegate;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.document.Document;

import javax.annotation.Nonnull;

public class EndHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public EndHandler(EditorActionHandler originalHandler) {
    super(true);
    myOriginalHandler = originalHandler;
  }

  @Override
  protected void doExecute(@Nonnull final Editor editor, Caret caret, DataContext dataContext) {
    CodeInsightSettings settings = CodeInsightSettings.getInstance();
    if (!settings.SMART_END_ACTION) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      return;
    }

    final Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(CommonDataKeys.PROJECT);
    if (project == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      return;
    }
    final Document document = editor.getDocument();
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);

    if (file == null) {
      if (myOriginalHandler != null){
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      return;
    }

    for (EditorNavigationDelegate delegate : Application.get().getExtensionPoint(EditorNavigationDelegate.class)) {
      if (delegate.navigateToLineEnd(editor, dataContext) == EditorNavigationDelegate.Result.STOP) {
        return;
      }
    }

    final CaretModel caretModel = editor.getCaretModel();
    final int caretOffset = caretModel.getOffset();
    CharSequence chars = editor.getDocument().getCharsSequence();
    int length = editor.getDocument().getTextLength();

    if (caretOffset < length) {
      final int offset1 = CharArrayUtil.shiftBackward(chars, caretOffset - 1, " \t");
      if (offset1 < 0 || chars.charAt(offset1) == '\n' || chars.charAt(offset1) == '\r') {
        final int offset2 = CharArrayUtil.shiftForward(chars, offset1 + 1, " \t");
        boolean isEmptyLine = offset2 >= length || chars.charAt(offset2) == '\n' || chars.charAt(offset2) == '\r';
        if (isEmptyLine) {

          // There is a possible case that indent string is not calculated for particular document (that is true at least for plain text
          // documents). Hence, we check that and don't finish processing in case we have such a situation.
          boolean stopProcessing = true;
          PsiDocumentManager.getInstance(project).commitAllDocuments();
          CodeStyleManager styleManager = CodeStyleManager.getInstance(project);
          final String lineIndent = styleManager.getLineIndent(file, caretOffset);
          if (lineIndent != null) {
            int col = calcColumnNumber(lineIndent, editor.getSettings().getTabSize(project));
            int line = caretModel.getVisualPosition().line;
            caretModel.moveToVisualPosition(new VisualPosition(line, col));

            if (caretModel.getLogicalPosition().column != col){
              if (!ApplicationManager.getApplication().isWriteAccessAllowed() &&
                  !FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
                return;
              }
              editor.getSelectionModel().removeSelection();
              WriteAction.run(() -> document.replaceString(offset1 + 1, offset2, lineIndent));
            }
          }
          else {
            stopProcessing = false;
          }

          editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
          editor.getSelectionModel().removeSelection();
          if (stopProcessing) {
            return;
          }
        }
      }
    }

    if (myOriginalHandler != null){
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }

  private static int calcColumnNumber(final String lineIndent, final int tabSize) {
    int result = 0;
    for (char c : lineIndent.toCharArray()) {
      if (c == ' ') result++;
      if (c == '\t') result += tabSize;
    }
    return result;
  }
}