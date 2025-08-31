// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.application.ApplicationManager;
import consulo.codeEditor.*;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.codeEditor.util.EditorUtil;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.logging.Logger;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Common part from desktop selection model
 */
public abstract class CodeEditorSelectionModelBase implements SelectionModel {
  private static final Logger LOG = Logger.getInstance(CodeEditorSelectionModelBase.class);

  private final List<SelectionListener> mySelectionListeners = Lists.newLockFreeCopyOnWriteList();
  private final CodeEditorBase myEditor;

  private TextAttributes myTextAttributes;

  public CodeEditorSelectionModelBase(CodeEditorBase editor) {
    myEditor = editor;
  }

  /**
   * @see Caret#setUnknownDirection(boolean)
   */
  @Override
  public boolean isUnknownDirection() {
    return myEditor.getCaretModel().getCurrentCaret().isUnknownDirection();
  }

  /**
   * @see Caret#setUnknownDirection(boolean)
   */
  @Override
  public void setUnknownDirection(boolean unknownDirection) {
    myEditor.getCaretModel().getCurrentCaret().setUnknownDirection(unknownDirection);
  }

  @Override
  public int getSelectionStart() {
    return myEditor.getCaretModel().getCurrentCaret().getSelectionStart();
  }

  @Nonnull
  @Override
  public VisualPosition getSelectionStartPosition() {
    return myEditor.getCaretModel().getCurrentCaret().getSelectionStartPosition();
  }

  @Override
  public int getSelectionEnd() {
    return myEditor.getCaretModel().getCurrentCaret().getSelectionEnd();
  }

  @Nonnull
  @Override
  public VisualPosition getSelectionEndPosition() {
    return myEditor.getCaretModel().getCurrentCaret().getSelectionEndPosition();
  }

  @Override
  public boolean hasSelection() {
    return hasSelection(false);
  }

  @Override
  public boolean hasSelection(boolean anyCaret) {
    if (!anyCaret) {
      return myEditor.getCaretModel().getCurrentCaret().hasSelection();
    }
    for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
      if (caret.hasSelection()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void setSelection(int startOffset, int endOffset) {
    myEditor.getCaretModel().getCurrentCaret().setSelection(startOffset, endOffset);
  }

  @Override
  public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    myEditor.getCaretModel().getCurrentCaret().setSelection(startOffset, endPosition, endOffset);
  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    myEditor.getCaretModel().getCurrentCaret().setSelection(startPosition, startOffset, endPosition, endOffset);
  }

  public void fireSelectionChanged(SelectionEvent event) {
    TextRange[] oldRanges = event.getOldRanges();
    TextRange[] newRanges = event.getNewRanges();
    int count = Math.min(oldRanges.length, newRanges.length);
    for (int i = 0; i < count; i++) {
      TextRange oldRange = oldRanges[i];
      TextRange newRange = newRanges[i];
      int oldSelectionStart = oldRange.getStartOffset();
      int startOffset = newRange.getStartOffset();
      int oldSelectionEnd = oldRange.getEndOffset();
      int endOffset = newRange.getEndOffset();
      myEditor.repaint(Math.min(oldSelectionStart, startOffset), Math.max(oldSelectionStart, startOffset), false);
      myEditor.repaint(Math.min(oldSelectionEnd, endOffset), Math.max(oldSelectionEnd, endOffset), false);
    }
    TextRange[] remaining = oldRanges.length < newRanges.length ? newRanges : oldRanges;
    for (int i = count; i < remaining.length; i++) {
      TextRange range = remaining[i];
      myEditor.repaint(range.getStartOffset(), range.getEndOffset(), false);
    }

    broadcastSelectionEvent(event);
  }

  private void broadcastSelectionEvent(SelectionEvent event) {
    for (SelectionListener listener : mySelectionListeners) {
      try {
        listener.selectionChanged(event);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public void removeSelection() {
    removeSelection(false);
  }

  @Override
  public void removeSelection(boolean allCarets) {
    if (!allCarets) {
      myEditor.getCaretModel().getCurrentCaret().removeSelection();
    }
    else {
      for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
        caret.removeSelection();
      }
    }
  }

  @Override
  public void setBlockSelection(@Nonnull LogicalPosition blockStart, @Nonnull LogicalPosition blockEnd) {
    List<CaretState> caretStates = EditorModificationUtil.calcBlockSelectionState(myEditor, blockStart, blockEnd);
    myEditor.getCaretModel().setCaretsAndSelections(caretStates);
  }

  @Override
  @Nonnull
  public int[] getBlockSelectionStarts() {
    Collection<Caret> carets = myEditor.getCaretModel().getAllCarets();
    int[] result = new int[carets.size()];
    int i = 0;
    for (Caret caret : carets) {
      result[i++] = caret.getSelectionStart();
    }
    return result;
  }

  @Override
  @Nonnull
  public int[] getBlockSelectionEnds() {
    Collection<Caret> carets = myEditor.getCaretModel().getAllCarets();
    int[] result = new int[carets.size()];
    int i = 0;
    for (Caret caret : carets) {
      result[i++] = caret.getSelectionEnd();
    }
    return result;
  }

  @Override
  public void addSelectionListener(@Nonnull SelectionListener listener) {
    mySelectionListeners.add(listener);
  }

  @Override
  public void removeSelectionListener(@Nonnull SelectionListener listener) {
    boolean success = mySelectionListeners.remove(listener);
    LOG.assertTrue(success);
  }

  @Override
  public String getSelectedText() {
    return getSelectedText(false);
  }

  @Override
  public String getSelectedText(boolean allCarets) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    if (myEditor.getCaretModel().supportsMultipleCarets() && allCarets) {
      StringBuilder buf = new StringBuilder();
      String separator = "";
      for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
        buf.append(separator);
        String caretSelectedText = caret.getSelectedText();
        if (caretSelectedText != null) {
          buf.append(caretSelectedText);
        }
        separator = "\n";
      }
      return buf.toString();
    }
    else {
      return myEditor.getCaretModel().getCurrentCaret().getSelectedText();
    }
  }

  public static void doSelectLineAtCaret(Caret caret) {
    Editor editor = caret.getEditor();
    int lineNumber = caret.getLogicalPosition().line;
    Document document = editor.getDocument();
    if (lineNumber >= document.getLineCount()) {
      return;
    }

    Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcCaretLineRange(caret);
    LogicalPosition lineStart = lines.first;
    LogicalPosition nextLineStart = lines.second;

    int start = editor.logicalPositionToOffset(lineStart);
    int end = editor.logicalPositionToOffset(nextLineStart);

    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    caret.removeSelection();
    caret.setSelection(start, end);
  }

  @Override
  public int getLeadSelectionOffset() {
    return myEditor.getCaretModel().getCurrentCaret().getLeadSelectionOffset();
  }

  @Nonnull
  @Override
  public VisualPosition getLeadSelectionPosition() {
    return myEditor.getCaretModel().getCurrentCaret().getLeadSelectionPosition();
  }

  @Override
  public void selectLineAtCaret() {
    myEditor.getCaretModel().getCurrentCaret().selectLineAtCaret();
  }

  @Override
  public void selectWordAtCaret(boolean honorCamelWordsSettings) {
    myEditor.getCaretModel().getCurrentCaret().selectWordAtCaret(honorCamelWordsSettings);
  }

  @Override
  public void copySelectionToClipboard() {
    EditorCopyPasteHelper.getInstance().copySelectionToClipboard(myEditor);
  }

  @Override
  public TextAttributes getTextAttributes() {
    if (myTextAttributes == null) {
      TextAttributes textAttributes = new TextAttributes();
      EditorColorsScheme scheme = myEditor.getColorsScheme();
      textAttributes.setForegroundColor(scheme.getColor(EditorColors.SELECTION_FOREGROUND_COLOR));
      textAttributes.setBackgroundColor(scheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR));
      myTextAttributes = textAttributes;
    }

    return myTextAttributes;
  }

  public void reinitSettings() {
    myTextAttributes = null;
  }
}
