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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.util.TextRange;
import consulo.disposer.Disposable;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * @author yole
 */
public class TextComponentDocument extends UserDataHolderBase implements Document {
  private final JTextComponent myTextComponent;

  public TextComponentDocument(final JTextComponent textComponent) {
    myTextComponent = textComponent;
  }

  @Nonnull
  @Override
  public String getText() {
    try {
      final javax.swing.text.Document document = myTextComponent.getDocument();
      return document.getText(0, document.getLength());
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  @Override
  public String getText(@Nonnull TextRange range) {
    try {
      final javax.swing.text.Document document = myTextComponent.getDocument();
      return document.getText(range.getStartOffset(), range.getLength());
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Nonnull
  public CharSequence getCharsSequence() {
    return getText();
  }

  @Nonnull
  @Override
  public CharSequence getImmutableCharSequence() {
    return getText();
  }

  @Override
  @Nonnull
  public char[] getChars() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getTextLength() {
    return myTextComponent.getDocument().getLength();
  }

  @Override
  public int getLineCount() {
    return 1;
  }

  @Override
  public int getLineNumber(final int offset) {
    return 0;
  }

  @Override
  public int getLineStartOffset(final int line) {
    return 0;
  }

  @Override
  public int getLineEndOffset(final int line) {
    return getTextLength();
  }

  @Override
  public void insertString(final int offset, @Nonnull final CharSequence s) {
    try {
      myTextComponent.getDocument().insertString(offset, s.toString(), null);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteString(final int startOffset, final int endOffset) {
    try {
      myTextComponent.getDocument().remove(startOffset, endOffset - startOffset);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void replaceString(final int startOffset, final int endOffset, @Nonnull final CharSequence s) {
    final javax.swing.text.Document document = myTextComponent.getDocument();
    try {
      document.remove(startOffset, endOffset-startOffset);
      document.insertString(startOffset, s.toString(), null);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public long getModificationStamp() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void fireReadOnlyModificationAttempt() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addDocumentListener(@Nonnull final DocumentListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addDocumentListener(@Nonnull final DocumentListener listener, @Nonnull final Disposable parentDisposable) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removeDocumentListener(@Nonnull final DocumentListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nonnull
  public RangeMarker createRangeMarker(final int startOffset, final int endOffset) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nonnull
  public RangeMarker createRangeMarker(final int startOffset, final int endOffset, final boolean surviveOnExternalChange) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void addPropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removePropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setReadOnly(final boolean isReadOnly) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nonnull
  public RangeMarker createGuardedBlock(final int startOffset, final int endOffset) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void removeGuardedBlock(@Nonnull final RangeMarker block) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nullable
  public RangeMarker getOffsetGuard(final int offset) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nullable
  public RangeMarker getRangeGuard(final int start, final int end) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void startGuardedBlockChecking() {
  }

  @Override
  public void stopGuardedBlockChecking() {
  }

  @Override
  public void setCyclicBufferSize(final int bufferSize) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setText(@Nonnull final CharSequence text) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  @Nonnull
  public RangeMarker createRangeMarker(@Nonnull final TextRange textRange) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getLineSeparatorLength(final int line) {
    return 0;
  }
}