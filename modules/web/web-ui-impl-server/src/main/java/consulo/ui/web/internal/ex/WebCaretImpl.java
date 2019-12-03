/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.web.internal.ex;

import com.intellij.openapi.editor.*;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-10
 */
public class WebCaretImpl implements Caret {
  private WebCaretModelImpl myCaretModel;
  private WebEditorImpl myEditor;

  public WebCaretImpl(WebCaretModelImpl caretModel, WebEditorImpl editor) {
    myCaretModel = caretModel;
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
    return myCaretModel;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean scrollToCaret) {

  }

  @Override
  public void moveToLogicalPosition(@Nonnull LogicalPosition pos) {

  }

  @Override
  public void moveToVisualPosition(@Nonnull VisualPosition pos) {

  }

  @Override
  public void moveToOffset(int offset) {

  }

  @Override
  public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {

  }

  @Override
  public boolean isUpToDate() {
    return false;
  }

  @Nonnull
  @Override
  public LogicalPosition getLogicalPosition() {
    return null;
  }

  @Nonnull
  @Override
  public VisualPosition getVisualPosition() {
    return null;
  }

  @Override
  public int getOffset() {
    return 0;
  }

  @Override
  public int getVisualLineStart() {
    return 0;
  }

  @Override
  public int getVisualLineEnd() {
    return 0;
  }

  @Override
  public int getSelectionStart() {
    return 0;
  }

  @Nonnull
  @Override
  public VisualPosition getSelectionStartPosition() {
    return null;
  }

  @Override
  public int getSelectionEnd() {
    return 0;
  }

  @Nonnull
  @Override
  public VisualPosition getSelectionEndPosition() {
    return null;
  }

  @Nullable
  @Override
  public String getSelectedText() {
    return null;
  }

  @Override
  public int getLeadSelectionOffset() {
    return 0;
  }

  @Nonnull
  @Override
  public VisualPosition getLeadSelectionPosition() {
    return null;
  }

  @Override
  public boolean hasSelection() {
    return false;
  }

  @Override
  public void setSelection(int startOffset, int endOffset) {

  }

  @Override
  public void setSelection(int startOffset, int endOffset, boolean updateSystemSelection) {

  }

  @Override
  public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {

  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {

  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset, boolean updateSystemSelection) {

  }

  @Override
  public void removeSelection() {

  }

  @Override
  public void selectLineAtCaret() {

  }

  @Override
  public void selectWordAtCaret(boolean honorCamelWordsSettings) {

  }

  @Nullable
  @Override
  public Caret clone(boolean above) {
    return null;
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
    return null;
  }

  @Override
  public void setVisualAttributes(@Nonnull CaretVisualAttributes attributes) {

  }

  @Override
  public void dispose() {

  }

  @Nonnull
  @Override
  public <T> T putUserDataIfAbsent(@Nonnull Key<T> key, @Nonnull T value) {
    return null;
  }

  @Override
  public <T> boolean replace(@Nonnull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
    return false;
  }

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return null;
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {

  }
}
