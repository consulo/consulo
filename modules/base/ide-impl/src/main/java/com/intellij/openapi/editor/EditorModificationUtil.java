/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.editor;

import com.intellij.util.Producer;
import consulo.application.util.LineTokenizer;
import consulo.codeEditor.*;
import consulo.document.FileDocumentManager;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.util.LanguageEditorUtil;
import consulo.ui.ex.awt.CopyPasteManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

public class EditorModificationUtil {
  private EditorModificationUtil() {
  }

  public static void deleteSelectedText(Editor editor) {
    consulo.codeEditor.util.EditorModificationUtil.deleteSelectedText(editor);
  }

  public static void deleteSelectedTextForAllCarets(@Nonnull final Editor editor) {
    editor.getCaretModel().runForEachCaret(caret -> deleteSelectedText(editor));
  }

  public static void zeroWidthBlockSelectionAtCaretColumn(final Editor editor, final int startLine, final int endLine) {
    int caretColumn = editor.getCaretModel().getLogicalPosition().column;
    editor.getSelectionModel().setBlockSelection(new LogicalPosition(startLine, caretColumn), new LogicalPosition(endLine, caretColumn));
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
    return consulo.codeEditor.util.EditorModificationUtil.insertStringAtCaret(editor, s, toProcessOverwriteMode, toMoveCaret, caretShift);
  }

  public static void pasteTransferableAsBlock(Editor editor, @Nullable Producer<Transferable> producer) {
    Transferable content = getTransferable(producer);
    if (content == null) return;
    String text = getStringContent(content);
    if (text == null) return;

    int caretLine = editor.getCaretModel().getLogicalPosition().line;

    LogicalPosition caretToRestore = editor.getCaretModel().getLogicalPosition();

    String[] lines = LineTokenizer.tokenize(text.toCharArray(), false);
    int longestLineLength = 0;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      longestLineLength = Math.max(longestLineLength, line.length());
      editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(caretLine + i, caretToRestore.column));
      insertStringAtCaret(editor, line, false, true);
    }
    caretToRestore = new LogicalPosition(caretLine, caretToRestore.column + longestLineLength);

    editor.getCaretModel().moveToLogicalPosition(caretToRestore);
    zeroWidthBlockSelectionAtCaretColumn(editor, caretLine, caretLine);
  }

  @Nullable
  public static Transferable getContentsToPasteToEditor(@Nullable Producer<Transferable> producer) {
    if (producer == null) {
      CopyPasteManager manager = CopyPasteManager.getInstance();
      return manager.areDataFlavorsAvailable(DataFlavor.stringFlavor) ? manager.getContents() : null;
    }
    else {
      return producer.produce();
    }
  }

  @Nullable
  public static String getStringContent(@Nonnull Transferable content) {
    RawText raw = RawText.fromTransferable(content);
    if (raw != null) return raw.rawText;

    try {
      return (String)content.getTransferData(DataFlavor.stringFlavor);
    }
    catch (UnsupportedFlavorException | IOException ignore) {
    }

    return null;
  }

  private static Transferable getTransferable(Producer<Transferable> producer) {
    Transferable content = null;
    if (producer != null) {
      content = producer.produce();
    }
    else {
      CopyPasteManager manager = CopyPasteManager.getInstance();
      if (manager.areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
        content = manager.getContents();
      }
    }
    return content;
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
    return consulo.codeEditor.util.EditorModificationUtil.calcAfterLineEnd(editor);
  }

  public static String calcStringToFillVirtualSpace(Editor editor) {
    return consulo.codeEditor.util.EditorModificationUtil.calcStringToFillVirtualSpace(editor);
  }

  public static String calcStringToFillVirtualSpace(Editor editor, int afterLineEnd) {
    return consulo.codeEditor.util.EditorModificationUtil.calcStringToFillVirtualSpace(editor, afterLineEnd);
  }

  public static void typeInStringAtCaretHonorMultipleCarets(final Editor editor, @Nonnull final String str) {
    consulo.codeEditor.util.EditorModificationUtil.typeInStringAtCaretHonorMultipleCarets(editor, str, true, str.length());
  }

  public static void typeInStringAtCaretHonorMultipleCarets(final Editor editor, @Nonnull final String str, final int caretShift) {
    consulo.codeEditor.util.EditorModificationUtil.typeInStringAtCaretHonorMultipleCarets(editor, str, true, caretShift);
  }

  public static void typeInStringAtCaretHonorMultipleCarets(final Editor editor, @Nonnull final String str, final boolean toProcessOverwriteMode) {
    consulo.codeEditor.util.EditorModificationUtil.typeInStringAtCaretHonorMultipleCarets(editor, str, toProcessOverwriteMode, str.length());
  }

  public static void moveAllCaretsRelatively(@Nonnull Editor editor, final int caretShift) {
    consulo.codeEditor.util.EditorModificationUtil.moveAllCaretsRelatively(editor, caretShift);
  }

  public static void moveCaretRelatively(@Nonnull Editor editor, final int caretShift) {
    CaretModel caretModel = editor.getCaretModel();
    caretModel.moveToOffset(caretModel.getOffset() + caretShift);
  }

  /**
   * This method is safe to run both in and out of {@link CaretModel#runForEachCaret(CaretAction)} context.
   * It scrolls to primary caret in both cases, and, in the former case, avoids performing excessive scrolling in case of large number
   * of carets.
   */
  public static void scrollToCaret(@Nonnull Editor editor) {
    consulo.codeEditor.util.EditorModificationUtil.scrollToCaret(editor);
  }

  @Nonnull
  public static List<CaretState> calcBlockSelectionState(@Nonnull Editor editor, @Nonnull LogicalPosition blockStart, @Nonnull LogicalPosition blockEnd) {
    return consulo.codeEditor.util.EditorModificationUtil.calcBlockSelectionState(editor, blockStart, blockEnd);
  }

  public static boolean requestWriting(@Nonnull Editor editor) {
    if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), editor.getProject())) {
      HintManager.getInstance().showInformationHint(editor, EditorBundle.message("editing.read.only.file.hint"));
      return false;
    }
    return true;
  }

  /**
   * @return true when not viewer
   * false otherwise, additionally information hint with warning would be shown
   */
  public static boolean checkModificationAllowed(Editor editor) {
    return LanguageEditorUtil.checkModificationAllowed(editor);
  }
}
