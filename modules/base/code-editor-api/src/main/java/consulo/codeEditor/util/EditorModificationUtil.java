/*
 * Copyright 2013-2022 consulo.io
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
package consulo.codeEditor.util;

import consulo.application.WriteAction;
import consulo.codeEditor.*;
import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.document.Document;
import consulo.document.ReadOnlyFragmentModificationException;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
public class EditorModificationUtil {
  public static void fillVirtualSpaceUntilCaret(@Nonnull Editor editor) {
    final LogicalPosition position = editor.getCaretModel().getLogicalPosition();
    fillVirtualSpaceUntil(editor, position.column, position.line);
  }

  public static void fillVirtualSpaceUntil(@Nonnull final Editor editor, int columnNumber, int lineNumber) {
    final int offset = editor.logicalPositionToOffset(new LogicalPosition(lineNumber, columnNumber));
    final String filler = EditorModificationUtil.calcStringToFillVirtualSpace(editor);
    if (!filler.isEmpty()) {
      WriteAction.run(() -> {
        editor.getDocument().insertString(offset, filler);
        editor.getCaretModel().moveToOffset(offset + filler.length());
      });
    }
  }

  @Nonnull
  public static List<CaretState> calcBlockSelectionState(@Nonnull Editor editor, @Nonnull LogicalPosition blockStart, @Nonnull LogicalPosition blockEnd) {
    int startLine = Math.max(Math.min(blockStart.line, editor.getDocument().getLineCount() - 1), 0);
    int endLine = Math.max(Math.min(blockEnd.line, editor.getDocument().getLineCount() - 1), 0);
    int step = endLine < startLine ? -1 : 1;
    int count = 1 + Math.abs(endLine - startLine);
    List<CaretState> caretStates = new LinkedList<>();
    boolean hasSelection = false;
    for (int line = startLine, i = 0; i < count; i++, line += step) {
      int startColumn = blockStart.column;
      int endColumn = blockEnd.column;
      int lineEndOffset = editor.getDocument().getLineEndOffset(line);
      LogicalPosition lineEndPosition = editor.offsetToLogicalPosition(lineEndOffset);
      int lineWidth = lineEndPosition.column;
      if (startColumn > lineWidth && endColumn > lineWidth && !editor.isColumnMode()) {
        LogicalPosition caretPos = new LogicalPosition(line, Math.min(startColumn, endColumn));
        caretStates.add(new CaretState(caretPos, lineEndPosition, lineEndPosition));
      }
      else {
        LogicalPosition startPos = new LogicalPosition(line, editor.isColumnMode() ? startColumn : Math.min(startColumn, lineWidth));
        LogicalPosition endPos = new LogicalPosition(line, editor.isColumnMode() ? endColumn : Math.min(endColumn, lineWidth));
        int startOffset = editor.logicalPositionToOffset(startPos);
        int endOffset = editor.logicalPositionToOffset(endPos);
        caretStates.add(new CaretState(endPos, startPos, endPos));
        hasSelection |= startOffset != endOffset;
      }
    }
    if (hasSelection && !editor.isColumnMode()) { // filtering out lines without selection
      Iterator<CaretState> caretStateIterator = caretStates.iterator();
      while (caretStateIterator.hasNext()) {
        CaretState state = caretStateIterator.next();
        //noinspection ConstantConditions
        if (state.getSelectionStart().equals(state.getSelectionEnd())) {
          caretStateIterator.remove();
        }
      }
    }
    return caretStates;
  }

  public static void typeInStringAtCaretHonorMultipleCarets(final Editor editor, @Nonnull final String str) {
    typeInStringAtCaretHonorMultipleCarets(editor, str, true, str.length());
  }

  public static void typeInStringAtCaretHonorMultipleCarets(final Editor editor, @Nonnull final String str, final int caretShift) {
    typeInStringAtCaretHonorMultipleCarets(editor, str, true, caretShift);
  }

  public static void typeInStringAtCaretHonorMultipleCarets(final Editor editor, @Nonnull final String str, final boolean toProcessOverwriteMode) {
    typeInStringAtCaretHonorMultipleCarets(editor, str, toProcessOverwriteMode, str.length());
  }

  /**
   * Inserts given string at each caret's position. Effective caret shift will be equal to <code>caretShift</code> for each caret.
   */
  public static void typeInStringAtCaretHonorMultipleCarets(final Editor editor, @Nonnull final String str, final boolean toProcessOverwriteMode, final int caretShift)
          throws ReadOnlyFragmentModificationException {
    editor.getCaretModel().runForEachCaret(caret -> insertStringAtCaretNoScrolling(editor, str, toProcessOverwriteMode, true, caretShift));
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
  }

  public static void moveAllCaretsRelatively(@Nonnull Editor editor, final int caretShift) {
    editor.getCaretModel().runForEachCaret(caret -> caret.moveToOffset(caret.getOffset() + caretShift));
  }

  public static void insertStringAtCaret(Editor editor, @Nonnull String s) {
    insertStringAtCaret(editor, s, false, true);
  }

  public static int insertStringAtCaret(Editor editor, @Nonnull String s, boolean toProcessOverwriteMode) {
    return insertStringAtCaret(editor, s, toProcessOverwriteMode, s.length());
  }

  public static int insertStringAtCaret(Editor editor, @Nonnull String s, boolean toProcessOverwriteMode, boolean toMoveCaret) {
    return insertStringAtCaret(editor, s, toProcessOverwriteMode, toMoveCaret, s.length());
  }

  public static int insertStringAtCaret(Editor editor, @Nonnull String s, boolean toProcessOverwriteMode, int caretShift) {
    return insertStringAtCaret(editor, s, toProcessOverwriteMode, true, caretShift);
  }

  public static int insertStringAtCaret(Editor editor, @Nonnull String s, boolean toProcessOverwriteMode, boolean toMoveCaret, int caretShift) {
    int result = insertStringAtCaretNoScrolling(editor, s, toProcessOverwriteMode, toMoveCaret, caretShift);
    if (toMoveCaret) {
      scrollToCaret(editor);
    }
    return result;
  }

  private static int insertStringAtCaretNoScrolling(Editor editor, @Nonnull String s, boolean toProcessOverwriteMode, boolean toMoveCaret, int caretShift) {
    final SelectionModel selectionModel = editor.getSelectionModel();
    if (selectionModel.hasSelection()) {
      VisualPosition startPosition = selectionModel.getSelectionStartPosition();
      if (editor.isColumnMode() && editor.getCaretModel().supportsMultipleCarets() && startPosition != null) {
        editor.getCaretModel().moveToVisualPosition(startPosition);
      }
      else {
        editor.getCaretModel().moveToOffset(selectionModel.getSelectionStart(), true);
      }
    }

    // There is a possible case that particular soft wraps become hard wraps if the caret is located at soft wrap-introduced virtual
    // space, hence, we need to give editor a chance to react accordingly.
    editor.getSoftWrapModel().beforeDocumentChangeAtCaret();
    int oldOffset = editor.getCaretModel().getOffset();

    String filler = calcStringToFillVirtualSpace(editor);
    if (filler.length() > 0) {
      s = filler + s;
    }

    Document document = editor.getDocument();
    if (editor.isInsertMode() || !toProcessOverwriteMode) {
      if (selectionModel.hasSelection()) {
        oldOffset = selectionModel.getSelectionStart();
        document.replaceString(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), s);
      }
      else {
        document.insertString(oldOffset, s);
      }
    }
    else {
      deleteSelectedText(editor);
      int lineNumber = editor.getCaretModel().getLogicalPosition().line;
      if (lineNumber >= document.getLineCount()) {
        return insertStringAtCaretNoScrolling(editor, s, false, toMoveCaret, s.length());
      }

      int endOffset = document.getLineEndOffset(lineNumber);
      document.replaceString(oldOffset, Math.min(endOffset, oldOffset + s.length()), s);
    }

    int offset = oldOffset + filler.length() + caretShift;
    if (toMoveCaret) {
      editor.getCaretModel().moveToOffset(offset, true);
      selectionModel.removeSelection();
    }
    else if (editor.getCaretModel().getOffset() != oldOffset) { // handling the case when caret model tracks document changes
      editor.getCaretModel().moveToOffset(oldOffset);
    }

    return offset;
  }

  public static void deleteSelectedText(Editor editor) {
    SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection()) return;

    int selectionStart = selectionModel.getSelectionStart();
    int selectionEnd = selectionModel.getSelectionEnd();

    VisualPosition selectionStartPosition = selectionModel.getSelectionStartPosition();
    if (editor.isColumnMode() && editor.getCaretModel().supportsMultipleCarets() && selectionStartPosition != null) {
      editor.getCaretModel().moveToVisualPosition(selectionStartPosition);
    }
    else {
      editor.getCaretModel().moveToOffset(selectionStart);
    }
    selectionModel.removeSelection();
    editor.getDocument().deleteString(selectionStart, selectionEnd);
    scrollToCaret(editor);
  }


  /**
   * This method is safe to run both in and out of {@link CaretModel#runForEachCaret(CaretAction)} context.
   * It scrolls to primary caret in both cases, and, in the former case, avoids performing excessive scrolling in case of large number
   * of carets.
   */
  public static void scrollToCaret(@Nonnull Editor editor) {
    if (editor.getCaretModel().getCurrentCaret() == editor.getCaretModel().getPrimaryCaret()) {
      editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }
  }

  public static String calcStringToFillVirtualSpace(Editor editor) {
    int afterLineEnd = calcAfterLineEnd(editor);
    if (afterLineEnd > 0) {
      return calcStringToFillVirtualSpace(editor, afterLineEnd);
    }

    return "";
  }

  /**
   * Calculates difference in columns between current editor caret position and end of the logical line fragment displayed
   * on a current visual line.
   *
   * @param editor target editor
   * @return difference in columns between current editor caret position and end of the logical line fragment displayed
   * on a current visual line
   */
  public static int calcAfterLineEnd(Editor editor) {
    Document document = editor.getDocument();
    CaretModel caretModel = editor.getCaretModel();
    LogicalPosition logicalPosition = caretModel.getLogicalPosition();
    int lineNumber = logicalPosition.line;
    int columnNumber = logicalPosition.column;
    if (lineNumber >= document.getLineCount()) {
      return columnNumber;
    }

    int caretOffset = caretModel.getOffset();
    int anchorLineEndOffset = document.getLineEndOffset(lineNumber);
    List<? extends SoftWrap> softWraps = editor.getSoftWrapModel().getSoftWrapsForLine(logicalPosition.line);
    for (SoftWrap softWrap : softWraps) {
      if (!editor.getSoftWrapModel().isVisible(softWrap)) {
        continue;
      }

      int softWrapOffset = softWrap.getStart();
      if (softWrapOffset == caretOffset) {
        // There are two possible situations:
        //     *) caret is located on a visual line before soft wrap-introduced line feed;
        //     *) caret is located on a visual line after soft wrap-introduced line feed;
        VisualPosition position = editor.offsetToVisualPosition(caretOffset - 1);
        VisualPosition visualCaret = caretModel.getVisualPosition();
        if (position.line == visualCaret.line) {
          return visualCaret.column - position.column - 1;
        }
      }
      if (softWrapOffset > caretOffset) {
        anchorLineEndOffset = softWrapOffset;
        break;
      }

      // Same offset corresponds to all soft wrap-introduced symbols, however, current method should behave differently in
      // situations when the caret is located just before the soft wrap and at the next visual line.
      if (softWrapOffset == caretOffset) {
        boolean visuallyBeforeSoftWrap = caretModel.getVisualPosition().line < editor.offsetToVisualPosition(caretOffset).line;
        if (visuallyBeforeSoftWrap) {
          anchorLineEndOffset = softWrapOffset;
          break;
        }
      }
    }

    int lineEndColumnNumber = editor.offsetToLogicalPosition(anchorLineEndOffset).column;
    return columnNumber - lineEndColumnNumber;
  }

  public static String calcStringToFillVirtualSpace(Editor editor, int afterLineEnd) {
    final Project project = editor.getProject();
    StringBuilder buf = new StringBuilder();
    final Document doc = editor.getDocument();
    final int caretOffset = editor.getCaretModel().getOffset();
    boolean atLineStart = caretOffset >= doc.getTextLength() || doc.getLineStartOffset(doc.getLineNumber(caretOffset)) == caretOffset;
    if (atLineStart && project != null) {
      int offset = editor.getCaretModel().getOffset();
      String properIndent = CodeEditorInternalHelper.getInstance().getProperIndent(project, doc, offset);
      if (properIndent != null) {
        int tabSize = editor.getSettings().getTabSize(project);
        for (int i = 0; i < properIndent.length(); i++) {
          if (properIndent.charAt(i) == ' ') {
            afterLineEnd--;
          }
          else if (properIndent.charAt(i) == '\t') {
            if (afterLineEnd < tabSize) {
              break;
            }
            afterLineEnd -= tabSize;
          }
          buf.append(properIndent.charAt(i));
          if (afterLineEnd == 0) break;
        }
      }
    }

    for (int i = 0; i < afterLineEnd; i++) {
      buf.append(' ');
    }

    return buf.toString();
  }

  public static void moveCaretRelatively(@Nonnull Editor editor, final int caretShift) {
    CaretModel caretModel = editor.getCaretModel();
    caretModel.moveToOffset(caretModel.getOffset() + caretShift);
  }
}
