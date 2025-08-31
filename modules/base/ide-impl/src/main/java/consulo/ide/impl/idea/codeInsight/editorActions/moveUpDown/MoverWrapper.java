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

package consulo.ide.impl.idea.codeInsight.editorActions.moveUpDown;

import consulo.codeEditor.*;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.language.editor.moveUpDown.StatementUpDownMover;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class MoverWrapper {
  protected final boolean myIsDown;
  private final StatementUpDownMover myMover;
  private final StatementUpDownMover.MoveInfo myInfo;

  protected MoverWrapper(@Nonnull StatementUpDownMover mover, @Nonnull StatementUpDownMover.MoveInfo info, boolean isDown) {
    myMover = mover;
    myIsDown = isDown;

    myInfo = info;
  }

  public StatementUpDownMover.MoveInfo getInfo() {
    return myInfo;
  }

  public final void move(final Editor editor, PsiFile file) {
    assert myInfo.toMove2 != null;
    myMover.beforeMove(editor, myInfo, myIsDown);
    Document document = editor.getDocument();
    int start = StatementUpDownMover.getLineStartSafeOffset(document, myInfo.toMove.startLine);
    int end = StatementUpDownMover.getLineStartSafeOffset(document, myInfo.toMove.endLine);
    myInfo.range1 = document.createRangeMarker(start, end);

    String textToInsert = document.getCharsSequence().subSequence(start, end).toString();
    if (!StringUtil.endsWithChar(textToInsert, '\n')) textToInsert += '\n';

    int start2 = document.getLineStartOffset(myInfo.toMove2.startLine);
    int end2 = StatementUpDownMover.getLineStartSafeOffset(document,myInfo.toMove2.endLine);
    String textToInsert2 = document.getCharsSequence().subSequence(start2, end2).toString();
    if (!StringUtil.endsWithChar(textToInsert2,'\n')) textToInsert2 += '\n';
    myInfo.range2 = document.createRangeMarker(start2, end2);
    if (myInfo.range1.getStartOffset() < myInfo.range2.getStartOffset()) {
      myInfo.range1.setGreedyToLeft(true);
      myInfo.range1.setGreedyToRight(false);
      myInfo.range2.setGreedyToLeft(true);
      myInfo.range2.setGreedyToRight(true);
    }
    else {
      myInfo.range1.setGreedyToLeft(true);
      myInfo.range1.setGreedyToRight(true);
      myInfo.range2.setGreedyToLeft(true);
      myInfo.range2.setGreedyToRight(false);
    }

    CaretModel caretModel = editor.getCaretModel();
    int caretRelativePos = caretModel.getOffset() - start;
    SelectionModel selectionModel = editor.getSelectionModel();
    int selectionStart = selectionModel.getSelectionStart();
    int selectionEnd = selectionModel.getSelectionEnd();
    boolean hasSelection = selectionModel.hasSelection();

    // to prevent flicker
    caretModel.moveToOffset(0);

    // There is a possible case that the user performs, say, method move. It's also possible that one (or both) of moved methods
    // are folded. We want to preserve their states then. The problem is that folding processing is based on PSI element pointers
    // and the pointers behave as following during move up/down:
    //     method1() {}
    //     method2() {}
    // Pointer for the fold region from method1 points to 'method2()' now and vice versa (check range markers processing on
    // document change for further information). I.e. information about fold regions statuses holds the data swapped for
    // 'method1' and 'method2'. Hence, we want to apply correct 'collapsed' status.
    final FoldRegion topRegion = findTopLevelRegionInRange(editor, myInfo.range1);
    final FoldRegion bottomRegion = findTopLevelRegionInRange(editor, myInfo.range2);

    document.insertString(myInfo.range1.getStartOffset(), textToInsert2);
    document.deleteString(myInfo.range1.getStartOffset()+textToInsert2.length(), myInfo.range1.getEndOffset());

    document.insertString(myInfo.range2.getStartOffset(), textToInsert);
    int s = myInfo.range2.getStartOffset() + textToInsert.length();
    int e = myInfo.range2.getEndOffset();
    if (e > s) {
      document.deleteString(s, e);
    }

    Project project = file.getProject();
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    // Swap fold regions status if necessary.
    if (topRegion != null && bottomRegion != null) {
      CodeFoldingManager.getInstance(project).updateFoldRegions(editor);
      editor.getFoldingModel().runBatchFoldingOperation(new Runnable() {
        @Override
        public void run() {
          FoldRegion newTopRegion = findTopLevelRegionInRange(editor, myInfo.range1);
          if (newTopRegion != null) {
            newTopRegion.setExpanded(bottomRegion.isExpanded());
          }

          FoldRegion newBottomRegion = findTopLevelRegionInRange(editor, myInfo.range2);
          if (newBottomRegion != null) {
            newBottomRegion.setExpanded(topRegion.isExpanded());
          }
        }
      });
    }

    if (hasSelection) {
      restoreSelection(editor, selectionStart, selectionEnd, start, myInfo.range2.getStartOffset());
    }

    caretModel.moveToOffset(myInfo.range2.getStartOffset() + caretRelativePos);
    if (myInfo.indentTarget) {
      indentLinesIn(editor, file, document, project, myInfo.range2);
    }
    if (myInfo.indentSource) {
      indentLinesIn(editor, file, document, project, myInfo.range1);
    }

    myMover.afterMove(editor, file, myInfo, myIsDown);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
  }

  private static FoldRegion findTopLevelRegionInRange(Editor editor, RangeMarker range) {
    FoldRegion result = null;
    for (FoldRegion foldRegion : editor.getFoldingModel().getAllFoldRegions()) {
      if (foldRegion.isValid() && contains(range, foldRegion) && !contains(result, foldRegion)) {
        result = foldRegion;
      }
    }
    return result;
  }

  /**
   * Allows to check if text range defined by the given range marker completely contains text range of the given fold region.
   *
   * @param rangeMarker   range marker to check
   * @param foldRegion    fold region to check
   * @return              <code>true</code> if text range defined by the given range marker completely contains text range
   *                      of the given fold region; <code>false</code> otherwise
   */
  private static boolean contains(@Nonnull RangeMarker rangeMarker, @Nonnull FoldRegion foldRegion) {
    return rangeMarker.getStartOffset() <= foldRegion.getStartOffset() && rangeMarker.getEndOffset() >= foldRegion.getEndOffset();
  }

  /**
   * Allows to check if given <code>'region2'</code> is nested to <code>'region1'</code>
   *
   * @param region1   'outer' region candidate
   * @param region2   'inner' region candidate
   * @return          <code>true</code> if 'region2' is nested to 'region1'; <code>false</code> otherwise
   */
  private static boolean contains(@Nullable FoldRegion region1, @Nonnull FoldRegion region2) {
    if (region1 == null) {
      return false;
    }
    return region1.getStartOffset() <= region2.getStartOffset() && region1.getEndOffset() >= region2.getEndOffset();
  }

  private static void indentLinesIn(Editor editor, PsiFile file, Document document, Project project, RangeMarker range) {
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
    int line1 = editor.offsetToLogicalPosition(range.getStartOffset()).line;
    int line2 = editor.offsetToLogicalPosition(range.getEndOffset()).line;

    while (!lineContainsNonSpaces(document, line1) && line1 < line2) line1++;
    while (!lineContainsNonSpaces(document, line2) && line1 < line2) line2--;

    FileViewProvider provider = file.getViewProvider();
    PsiFile rootToAdjustIndentIn = provider.getPsi(provider.getBaseLanguage());
    codeStyleManager.adjustLineIndent(rootToAdjustIndentIn, new TextRange(document.getLineStartOffset(line1), document.getLineStartOffset(line2)));
  }

  private static boolean lineContainsNonSpaces(Document document, int line) {
    if (line >= document.getLineCount()) {
      return false;
    }
    int lineStartOffset = document.getLineStartOffset(line);
    int lineEndOffset = document.getLineEndOffset(line);
    @NonNls String text = document.getCharsSequence().subSequence(lineStartOffset, lineEndOffset).toString();
    return text.trim().length() != 0;
  }

  private static void restoreSelection(Editor editor, int selectionStart, int selectionEnd, int moveOffset, int insOffset) {
    int selectionRelativeOffset = selectionStart - moveOffset;
    int newSelectionStart = insOffset + selectionRelativeOffset;
    int newSelectionEnd = newSelectionStart + selectionEnd - selectionStart;
    editor.getSelectionModel().setSelection(newSelectionStart, newSelectionEnd);
  }
}
