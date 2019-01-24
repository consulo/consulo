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
package com.intellij.openapi.editor.textarea;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.TextAttributes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class TextComponentCaretModel implements CaretModel {
  private final JTextComponent myTextComponent;
  private final TextComponentEditorImpl myEditor;
  private final Caret myCaret;

  public TextComponentCaretModel(@Nonnull JTextComponent textComponent, @Nonnull TextComponentEditorImpl editor) {
    myTextComponent = textComponent;
    myEditor = editor;
    myCaret = new TextComponentCaret(editor);
  }

  @Override
  public void moveCaretRelatively(final int columnShift,
                                  final int lineShift,
                                  final boolean withSelection, final boolean blockSelection, final boolean scrollToCaret) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void moveToLogicalPosition(@Nonnull final LogicalPosition pos) {
    moveToOffset(myEditor.logicalPositionToOffset(pos), false);
  }

  @Override
  public void moveToVisualPosition(@Nonnull final VisualPosition pos) {
    moveToLogicalPosition(myEditor.visualToLogicalPosition(pos));
  }

  @Override
  public void moveToOffset(int offset) {
    moveToOffset(offset, false);
  }

  @Override
  public void moveToOffset(final int offset, boolean locateBeforeSoftWrap) {
    int targetOffset = Math.min(offset, myTextComponent.getText().length());
    int currentPosition = myTextComponent.getCaretPosition();
    // We try to preserve selection, to match EditorImpl behaviour.
    // It's only possible though, if target offset is located at either end of existing selection.
    if (targetOffset != currentPosition) {
      if (targetOffset == myTextComponent.getCaret().getMark()) {
        myTextComponent.setCaretPosition(currentPosition);
        myTextComponent.moveCaretPosition(targetOffset);
      }
      else {
        myTextComponent.setCaretPosition(targetOffset);
      }
    }
  }

  @Override
  public boolean isUpToDate() {
    return true;
  }

  @Override
  @Nonnull
  public LogicalPosition getLogicalPosition() {
    int caretPos = myTextComponent.getCaretPosition();
    int line;
    int lineStart;
    if (myTextComponent instanceof JTextArea) {
      final JTextArea textArea = (JTextArea)myTextComponent;
      try {
        line = textArea.getLineOfOffset(caretPos);
        lineStart = textArea.getLineStartOffset(line);
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      line = 0;
      lineStart = 0;
    }
    return new LogicalPosition(line, caretPos - lineStart);
  }

  @Override
  @Nonnull
  public VisualPosition getVisualPosition() {
    LogicalPosition pos = getLogicalPosition();
    return new VisualPosition(pos.line, pos.column);
  }

  @Override
  public int getOffset() {
    return myTextComponent.getCaretPosition();
  }

  @Override
  public void addCaretListener(@Nonnull final CaretListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removeCaretListener(@Nonnull final CaretListener listener) {
    throw new UnsupportedOperationException("Not implemented");
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
    return myCaret;
  }

  @Nonnull
  @Override
  public Caret getPrimaryCaret() {
    return myCaret;
  }

  @Override
  public int getCaretCount() {
    return 1;
  }

  @Nonnull
  @Override
  public List<Caret> getAllCarets() {
    return Collections.singletonList(myCaret);
  }

  @Nullable
  @Override
  public Caret getCaretAt(@Nonnull VisualPosition pos) {
    return myCaret.getVisualPosition().equals(pos) ? myCaret : null;
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
  public void setCaretsAndSelections(@Nonnull List<CaretState> caretStates) {
    throw new UnsupportedOperationException("Multiple carets are not supported");
  }

  @Override
  public void setCaretsAndSelections(@Nonnull List<CaretState> caretStates, boolean updateSystemSelection) {
    throw new UnsupportedOperationException("Multiple carets are not supported");
  }

  @Nonnull
  @Override
  public List<CaretState> getCaretsAndSelections() {
    throw new UnsupportedOperationException("Multiple carets are not supported");
  }

  @Override
  public void runForEachCaret(@Nonnull Consumer<Caret> action, boolean reverseOrder) {
    action.accept(myCaret);
  }

  @Override
  public void runBatchCaretOperation(@Nonnull Runnable runnable) {
    runnable.run();
  }
}
