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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.EditorNavigationDelegate;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(order = "first")
public class EndHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;

  public EndHandler() {
    super(true);
  }

  @Override
  protected void doExecute(@Nonnull Editor editor, Caret caret, DataContext dataContext) {
    CodeInsightSettings settings = CodeInsightSettings.getInstance();
    if (!settings.SMART_END_ACTION) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      return;
    }

    Project project = DataManager.getInstance().getDataContext(editor.getComponent()).getData(Project.KEY);
    if (project == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      return;
    }
    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);

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

    CaretModel caretModel = editor.getCaretModel();
    int caretOffset = caretModel.getOffset();
    CharSequence chars = editor.getDocument().getCharsSequence();
    int length = editor.getDocument().getTextLength();

    if (caretOffset < length) {
      int offset1 = CharArrayUtil.shiftBackward(chars, caretOffset - 1, " \t");
      if (offset1 < 0 || chars.charAt(offset1) == '\n' || chars.charAt(offset1) == '\r') {
        int offset2 = CharArrayUtil.shiftForward(chars, offset1 + 1, " \t");
        boolean isEmptyLine = offset2 >= length || chars.charAt(offset2) == '\n' || chars.charAt(offset2) == '\r';
        if (isEmptyLine) {

          // There is a possible case that indent string is not calculated for particular document (that is true at least for plain text
          // documents). Hence, we check that and don't finish processing in case we have such a situation.
          boolean stopProcessing = true;
          PsiDocumentManager.getInstance(project).commitAllDocuments();
          CodeStyleManager styleManager = CodeStyleManager.getInstance(project);
          String lineIndent = styleManager.getLineIndent(file, caretOffset);
          if (lineIndent != null) {
            int col = calcColumnNumber(lineIndent, editor.getSettings().getTabSize(project));
            int line = caretModel.getVisualPosition().line;
            caretModel.moveToVisualPosition(new VisualPosition(line, col));

            if (caretModel.getLogicalPosition().column != col){
              if (!project.getApplication().isWriteAccessAllowed() &&
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

  private static int calcColumnNumber(String lineIndent, int tabSize) {
    int result = 0;
    for (char c : lineIndent.toCharArray()) {
      if (c == ' ') result++;
      if (c == '\t') result += tabSize;
    }
    return result;
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_MOVE_LINE_END;
  }
}