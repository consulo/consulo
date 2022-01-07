/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.editor.textarea;

import com.intellij.openapi.editor.*;
import consulo.util.dataholder.UserDataHolderBase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TextComponentCaret extends UserDataHolderBase implements Caret {
  private final Editor myEditor;

  public TextComponentCaret(Editor editor) {
    myEditor = editor;
  }

  @Nonnull
  @Override
  public Editor getEditor() {
    return myEditor;
  }

  @Nonnull
  @Override
  public CaretModel getCaretModel() {
    return myEditor.getCaretModel();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean scrollToCaret) {
    getCaretModel().moveCaretRelatively(columnShift, lineShift, withSelection, false, scrollToCaret);
  }

  @Override
  public void moveToLogicalPosition(@Nonnull LogicalPosition pos) {
    getCaretModel().moveToLogicalPosition(pos);
  }

  @Override
  public void moveToVisualPosition(@Nonnull VisualPosition pos) {
    getCaretModel().moveToVisualPosition(pos);
  }

  @Override
  public void moveToOffset(int offset) {
    getCaretModel().moveToOffset(offset);
  }

  @Override
  public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
    getCaretModel().moveToOffset(offset, locateBeforeSoftWrap);
  }

  @Override
  public boolean isUpToDate() {
    return getCaretModel().isUpToDate();
  }

  @Nonnull
  @Override
  public LogicalPosition getLogicalPosition() {
    return getCaretModel().getLogicalPosition();
  }

  @Nonnull
  @Override
  public VisualPosition getVisualPosition() {
    return getCaretModel().getVisualPosition();
  }

  @Override
  public int getOffset() {
    return getCaretModel().getOffset();
  }

  @Override
  public int getVisualLineStart() {
    return getCaretModel().getVisualLineStart();
  }

  @Override
  public int getVisualLineEnd() {
    return getCaretModel().getVisualLineEnd();
  }

  @Override
  public int getSelectionStart() {
    return getSelectionModel().getSelectionStart();
  }

  @Nonnull
  @Override
  public VisualPosition getSelectionStartPosition() {
    return myEditor.offsetToVisualPosition(getSelectionModel().getSelectionStart());
  }

  @Override
  public int getSelectionEnd() {
    return getSelectionModel().getSelectionEnd();
  }

  @Nonnull
  @Override
  public VisualPosition getSelectionEndPosition() {
    return myEditor.offsetToVisualPosition(getSelectionModel().getSelectionEnd());
  }

  @Nullable
  @Override
  public String getSelectedText() {
    return getSelectionModel().getSelectedText();
  }

  @Override
  public int getLeadSelectionOffset() {
    return getSelectionModel().getLeadSelectionOffset();
  }

  @Nonnull
  @Override
  public VisualPosition getLeadSelectionPosition() {
    return myEditor.offsetToVisualPosition(getSelectionModel().getLeadSelectionOffset());
  }

  @Override
  public boolean hasSelection() {
    return getSelectionModel().hasSelection();
  }

  @Override
  public void setSelection(int startOffset, int endOffset) {
    getSelectionModel().setSelection(startOffset, endOffset);
  }

  @Override
  public void setSelection(int startOffset, int endOffset, boolean updateSystemSelection) {
    // updating system selection is not supported currently for TextComponentEditor
    setSelection(startOffset, endOffset);
  }

  @Override
  public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    getSelectionModel().setSelection(startOffset, endPosition, endOffset);
  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    getSelectionModel().setSelection(startPosition, startOffset, endPosition, endOffset);
  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset,
                           boolean updateSystemSelection) {
    // updating system selection is not supported currently for TextComponentEditor
    setSelection(startPosition, startOffset, endPosition, endOffset);
  }

  @Override
  public void removeSelection() {
    getSelectionModel().removeSelection();
  }

  @Override
  public void selectLineAtCaret() {
    getSelectionModel().selectLineAtCaret();
  }

  @Override
  public void selectWordAtCaret(boolean honorCamelWordsSettings) {
    getSelectionModel().selectWordAtCaret(honorCamelWordsSettings);
  }

  @Nullable
  @Override
  public Caret clone(boolean above) {
    return null;
  }

  @Override
  public void dispose() {
  }

  @Override
  public boolean isAtRtlLocation() {
    return false;
  }

  @Override
  public boolean isAtBidiRunBoundary() {
    return false;
  }

  @Nonnull
  @Override
  public CaretVisualAttributes getVisualAttributes() {
    return CaretVisualAttributes.DEFAULT;
  }

  @Override
  public void setVisualAttributes(@Nonnull CaretVisualAttributes attributes) {
  }

  private SelectionModel getSelectionModel() {
    return myEditor.getSelectionModel();
  }
}
