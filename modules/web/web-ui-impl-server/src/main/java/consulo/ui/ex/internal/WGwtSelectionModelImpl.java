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
package consulo.ui.ex.internal;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.editor.markup.TextAttributes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-10
 */
public class WGwtSelectionModelImpl implements SelectionModel {
  @Override
  public int getSelectionStart() {
    return 0;
  }

  @Nullable
  @Override
  public VisualPosition getSelectionStartPosition() {
    return null;
  }

  @Override
  public int getSelectionEnd() {
    return 0;
  }

  @Nullable
  @Override
  public VisualPosition getSelectionEndPosition() {
    return null;
  }

  @Nullable
  @Override
  public String getSelectedText() {
    return null;
  }

  @Nullable
  @Override
  public String getSelectedText(boolean allCarets) {
    return null;
  }

  @Override
  public int getLeadSelectionOffset() {
    return 0;
  }

  @Nullable
  @Override
  public VisualPosition getLeadSelectionPosition() {
    return null;
  }

  @Override
  public boolean hasSelection() {
    return false;
  }

  @Override
  public boolean hasSelection(boolean anyCaret) {
    return false;
  }

  @Override
  public void setSelection(int startOffset, int endOffset) {

  }

  @Override
  public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {

  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {

  }

  @Override
  public void removeSelection() {

  }

  @Override
  public void removeSelection(boolean allCarets) {

  }

  @Override
  public void addSelectionListener(SelectionListener listener) {

  }

  @Override
  public void removeSelectionListener(SelectionListener listener) {

  }

  @Override
  public void selectLineAtCaret() {

  }

  @Override
  public void selectWordAtCaret(boolean honorCamelWordsSettings) {

  }

  @Override
  public void copySelectionToClipboard() {

  }

  @Override
  public void setBlockSelection(@Nonnull LogicalPosition blockStart, @Nonnull LogicalPosition blockEnd) {

  }

  @Nonnull
  @Override
  public int[] getBlockSelectionStarts() {
    return new int[0];
  }

  @Nonnull
  @Override
  public int[] getBlockSelectionEnds() {
    return new int[0];
  }

  @Override
  public TextAttributes getTextAttributes() {
    return null;
  }
}
