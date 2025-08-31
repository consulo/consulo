/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import consulo.application.util.registry.Registry;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.SelectionModel;
import consulo.document.util.TextRange;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * @author AG
 * @author yole
 */
@ExtensionImpl(id = "selectionQuoting")
public class SelectionQuotingTypedHandler extends TypedHandlerDelegate {
  private TextRange myReplacedTextRange;
  private boolean myRestoreStickySelection;
  private boolean myLtrSelection;

  @Override
  public Result checkAutoPopup(char c, Project project, Editor editor, PsiFile psiFile) {
    // TODO[oleg] provide adequate API not to use this hack
    // beforeCharTyped always works with removed selection
    SelectionModel selectionModel = editor.getSelectionModel();
    if (CodeInsightSettings.getInstance().SURROUND_SELECTION_ON_QUOTE_TYPED && selectionModel.hasSelection() && isDelimiter(c)) {
      String selectedText = selectionModel.getSelectedText();
      if (selectedText.length() < 1) {
        return super.checkAutoPopup(c, project, editor, psiFile);
      }

      int selectionStart = selectionModel.getSelectionStart();
      int selectionEnd = selectionModel.getSelectionEnd();
      if (selectedText.length() > 1) {
        char firstChar = selectedText.charAt(0);
        char lastChar = selectedText.charAt(selectedText.length() - 1);
        if (isSimilarDelimiters(firstChar, c) &&
            lastChar == getMatchingDelimiter(firstChar) &&
            (isQuote(firstChar) || firstChar != c) &&
            !shouldSkipReplacementOfQuotesOrBraces(psiFile, editor, selectedText, c) &&
            selectedText.indexOf(lastChar, 1) == selectedText.length() - 1) {
          selectedText = selectedText.substring(1, selectedText.length() - 1);
        }
      }
      int caretOffset = selectionModel.getSelectionStart();
      char c2 = getMatchingDelimiter(c);
      String newText = String.valueOf(c) + selectedText + c2;
      myLtrSelection = selectionModel.getLeadSelectionOffset() != selectionModel.getSelectionEnd();
      if (editor instanceof EditorEx) {
        myRestoreStickySelection = ((EditorEx)editor).isStickySelection();
      }
      else {
        myRestoreStickySelection = false;
      }
      selectionModel.removeSelection();
      editor.getDocument().replaceString(selectionStart, selectionEnd, newText);
      if (Registry.is("editor.smarterSelectionQuoting")) {
        myReplacedTextRange = new TextRange(caretOffset + 1, caretOffset + newText.length() - 1);
      }
      else {
        myReplacedTextRange = new TextRange(caretOffset, caretOffset + newText.length());
      }
      return Result.STOP;
    }
    return super.checkAutoPopup(c, project, editor, psiFile);
  }

  private static boolean shouldSkipReplacementOfQuotesOrBraces(PsiFile psiFile, Editor editor, String selectedText, char c) {
    for (DequotingFilter filter : Application.get().getExtensionPoint(DequotingFilter.class).getExtensionList()) {
      if (filter.skipReplacementQuotesOrBraces(psiFile, editor, selectedText, c)) return true;
    }
    return false;
  }

  private static char getMatchingDelimiter(char c) {
    if (c == '(') return ')';
    if (c == '[') return ']';
    if (c == '{') return '}';
    if (c == '<') return '>';
    return c;
  }

  private static boolean isDelimiter(char c) {
    return isBracket(c) || isQuote(c);
  }

  private static boolean isBracket(char c) {
    return c == '(' || c == '{' || c == '[' || c == '<';
  }

  private static boolean isQuote(char c) {
    return c == '"' || c == '\'' || c == '`';
  }

  private static boolean isSimilarDelimiters(char c1, char c2) {
    return (isBracket(c1) && isBracket(c2)) || (isQuote(c1) && isQuote(c2));
  }

  @Override
  public Result beforeCharTyped(char charTyped, Project project, Editor editor, PsiFile file, FileType fileType) {
    // TODO[oleg] remove this hack when API changes
    if (myReplacedTextRange != null) {
      if (myReplacedTextRange.getEndOffset() <= editor.getDocument().getTextLength()) {
        if (myRestoreStickySelection && editor instanceof EditorEx) {
          EditorEx editorEx = (EditorEx)editor;
          CaretModel caretModel = editorEx.getCaretModel();
          caretModel.moveToOffset(myLtrSelection ? myReplacedTextRange.getStartOffset() : myReplacedTextRange.getEndOffset());
          editorEx.setStickySelection(true);
          caretModel.moveToOffset(myLtrSelection ? myReplacedTextRange.getEndOffset() : myReplacedTextRange.getStartOffset());
        }
        else {
          if (myLtrSelection || editor instanceof EditorWindow) {
            editor.getSelectionModel().setSelection(myReplacedTextRange.getStartOffset(), myReplacedTextRange.getEndOffset());
          }
          else {
            editor.getSelectionModel().setSelection(myReplacedTextRange.getEndOffset(), myReplacedTextRange.getStartOffset());
          }
          if (Registry.is("editor.smarterSelectionQuoting")) {
            editor.getCaretModel().moveToOffset(myLtrSelection ? myReplacedTextRange.getEndOffset() : myReplacedTextRange.getStartOffset());
          }
        }
      }
      myReplacedTextRange = null;
      return Result.STOP;
    }
    return Result.CONTINUE;
  }
}
