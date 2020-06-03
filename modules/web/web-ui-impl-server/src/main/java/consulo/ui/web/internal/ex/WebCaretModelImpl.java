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
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-10
 */
public class WebCaretModelImpl implements CaretModel {
  private WebCaretImpl myPrimaryCaret;
  private WebEditorImpl myEditor;

  public WebCaretModelImpl(WebEditorImpl editor) {
    myEditor = editor;
    myPrimaryCaret = new WebCaretImpl(this, editor);
  }

  @Override
  public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean blockSelection, boolean scrollToCaret) {

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
  public void addCaretListener(@Nonnull CaretListener listener) {

  }

  @Override
  public void removeCaretListener(@Nonnull CaretListener listener) {

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
  public TextAttributes getTextAttributes() {
    return null;
  }

  @Override
  public boolean supportsMultipleCarets() {
    return false;
  }

  @Nonnull
  @Override
  public Caret getCurrentCaret() {
    return myPrimaryCaret;
  }

  @Nonnull
  @Override
  public Caret getPrimaryCaret() {
    return myPrimaryCaret;
  }

  @Override
  public int getCaretCount() {
    return 1;
  }

  @Nonnull
  @Override
  public List<Caret> getAllCarets() {
    return Collections.singletonList(myPrimaryCaret);
  }

  @Nullable
  @Override
  public Caret getCaretAt(@Nonnull VisualPosition pos) {
    return null;
  }

  @Nullable
  @Override
  public Caret addCaret(@Nonnull VisualPosition pos) {
    return null;
  }

  @Nullable
  @Override
  public Caret addCaret(@Nonnull VisualPosition pos, boolean makePrimary) {
    return null;
  }

  @Override
  public boolean removeCaret(@Nonnull Caret caret) {
    return false;
  }

  @Override
  public void removeSecondaryCarets() {

  }

  @Override
  public void setCaretsAndSelections(@Nonnull List<? extends CaretState> caretStates) {

  }

  @Override
  public void setCaretsAndSelections(@Nonnull List<? extends CaretState> caretStates, boolean updateSystemSelection) {

  }

  @Nonnull
  @Override
  public List<CaretState> getCaretsAndSelections() {
    return Collections.emptyList();
  }

  @Override
  public void runForEachCaret(@Nonnull CaretAction action) {

  }

  @Override
  public void runForEachCaret(@Nonnull CaretAction action, boolean reverseOrder) {

  }

  @Override
  public void addCaretActionListener(@Nonnull CaretActionListener listener, @Nonnull Disposable disposable) {

  }

  @Override
  public void runBatchCaretOperation(@Nonnull Runnable runnable) {

  }
}
