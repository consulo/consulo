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

import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.action.EditorAction;
import consulo.project.Project;
import consulo.document.util.TextRange;
import jakarta.annotation.Nullable;

public class SelectNextOccurrenceAction extends EditorAction {
  public SelectNextOccurrenceAction() {
    super(new Handler());
  }

  static class Handler extends SelectOccurrencesActionHandler {
    @Override
    public boolean isEnabled(Editor editor, DataContext dataContext) {
      return super.isEnabled(editor, dataContext) && editor.getProject() != null && editor.getCaretModel().supportsMultipleCarets();
    }

    @Override
    public void doExecute(Editor editor, @Nullable Caret c, DataContext dataContext) {
      Caret caret = c == null ? editor.getCaretModel().getPrimaryCaret() : c;
      TextRange wordSelectionRange = getSelectionRange(editor, caret);
      boolean notFoundPreviously = getAndResetNotFoundStatus(editor);
      boolean wholeWordSearch = isWholeWordSearch(editor);
      if (caret.hasSelection()) {
        Project project = editor.getProject();
        String selectedText = caret.getSelectedText();
        if (project == null || selectedText == null) {
          return;
        }
        FindManager findManager = FindManager.getInstance(project);

        FindModel model = new FindModel();
        model.setStringToFind(selectedText);
        model.setCaseSensitive(true);
        model.setWholeWordsOnly(wholeWordSearch);

        int searchStartOffset = notFoundPreviously ? 0 : caret.getSelectionEnd();
        FindResult findResult = findManager.findString(editor.getDocument().getCharsSequence(), searchStartOffset, model);
        if (findResult.isStringFound()) {
          int newCaretOffset = caret.getOffset() - caret.getSelectionStart() + findResult.getStartOffset();
          EditorActionUtil.makePositionVisible(editor, newCaretOffset);
          Caret newCaret = editor.getCaretModel().addCaret(editor.offsetToVisualPosition(newCaretOffset));
          if (newCaret == null) {
            // this means that the found occurence is already selected
            if (notFoundPreviously) {
              setNotFoundStatus(editor); // to make sure we won't show hint anymore if there are no more occurrences
            }
          }
          else {
            setSelection(editor, newCaret, findResult);
          }
        }
        else {
          setNotFoundStatus(editor);
          showHint(editor);
        }
      }
      else {
        if (wordSelectionRange == null) {
          return;
        }
        setSelection(editor, caret, wordSelectionRange);
        setWholeWordSearch(editor, true);
      }
      editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }
  }
}
