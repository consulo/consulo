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
package com.intellij.mock;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditReadOnlyListener;
import com.intellij.openapi.editor.ex.LineIterator;
import com.intellij.openapi.editor.ex.RangeMarkerEx;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.LocalTimeCounter;
import com.intellij.util.Processor;

import javax.annotation.Nonnull;

import kava.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDocument implements DocumentEx {
  private final Map myUserData = new HashMap();
  private StringBuffer myText = new StringBuffer();
  private long myModStamp = LocalTimeCounter.currentTime();

  public MockDocument() {
  }

  @Nonnull
  @Override
  public String getText() {
    return myText.toString();
  }

  @Nonnull
  @Override
  public String getText(@Nonnull TextRange range) {
    return range.substring(myText.toString());
  }

  @Override
  public void replaceText(@Nonnull CharSequence chars, long newModificationStamp) {
    myText = new StringBuffer();
    myText.append(chars);
    myModStamp = newModificationStamp;
  }

  public CharSequence textToCharArray() {
    return getText();
  }

  @Override
  @Nonnull
  public char[] getChars() {
    return getText().toCharArray();
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
  public int getTextLength() {
    return myText.length();
  }

  @Override
  public int getLineCount() {
    return 1;
  }

  @Override
  public int getLineNumber(int offset) {
    return 0;
  }

  @Override
  public int getLineStartOffset(int line) {
    return 0;
  }

  @Override
  public int getLineEndOffset(int line) {
    return myText.length();
  }

  @Override
  public void insertString(int offset, @Nonnull CharSequence s) {
    myText.insert(offset, s);
  }

  @Override
  public void deleteString(int startOffset, int endOffset) {
    myText.delete(startOffset, endOffset);
  }

  @Override
  public void replaceString(int startOffset, int endOffset, @Nonnull CharSequence s) {
    myText.replace(startOffset, endOffset, s.toString());
    myModStamp = LocalTimeCounter.currentTime();
  }

  @Override
  public void moveText(int srcStart, int srcEnd, int dstOffset) {
    // TODO den implement
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public long getModificationStamp() {
    return myModStamp;
  }

  @Override
  public void fireReadOnlyModificationAttempt() {
  }

  @Override
  public void addDocumentListener(@Nonnull DocumentListener listener) {
  }

  @Override
  public void addDocumentListener(@Nonnull DocumentListener listener, @Nonnull Disposable parentDisposable) {
  }

  @Override
  public void removeDocumentListener(@Nonnull DocumentListener listener) {
  }

  @Override
  @Nonnull
  public RangeMarker createRangeMarker(int startOffset, int endOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public RangeMarker createRangeMarker(int startOffset, int endOffset, boolean surviveOnExternalChange) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getUserData(@Nonnull Key<T> key) {
    return (T)myUserData.get(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> void putUserData(@Nonnull Key<T> key, T value) {
    myUserData.put(key, value);
  }

  @Override
  public void setStripTrailingSpacesEnabled(boolean isEnabled) {
  }

  @Override
  public int getLineSeparatorLength(int line) {
    return 0;
  }

  @Override
  @Nonnull
  public LineIterator createLineIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setModificationStamp(long modificationStamp) {
    myModStamp = modificationStamp;
  }

  @Override
  public void setReadOnly(boolean isReadOnly) {
  }

  @Override
  public RangeMarker getRangeGuard(int start, int end) {
    return null;
  }

  @Override
  public void startGuardedBlockChecking() {
  }

  @Override
  public void stopGuardedBlockChecking() {
  }

  @Override
  @Nonnull
  public RangeMarker createGuardedBlock(int startOffset, int endOffset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeGuardedBlock(@Nonnull RangeMarker block) {
  }

  @Override
  public RangeMarker getOffsetGuard(int offset) {
    return null;
  }

  @Override
  public void addEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
  }

  @Override
  public void removeEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
  }

  @Override
  public void suppressGuardedExceptions() {
  }

  @Override
  public void unSuppressGuardedExceptions() {

  }

  @Override
  public boolean isInEventsHandling() {
    return false;
  }

  @Override
  public void clearLineModificationFlags() {
  }

  @Override
  public boolean removeRangeMarker(@Nonnull RangeMarkerEx rangeMarker) {
    return false;
  }

  @Override
  public void registerRangeMarker(@Nonnull RangeMarkerEx rangeMarker,
                                  int start,
                                  int end,
                                  boolean greedyToLeft,
                                  boolean greedyToRight,
                                  int layer) {

  }

  @Override
  public boolean isInBulkUpdate() {
    return false;
  }

  @Override
  public void setInBulkUpdate(boolean value) {
  }

  @Override
  public void setCyclicBufferSize(int bufferSize) {
  }

  @Override
  public void setText(@Nonnull final CharSequence text) {
  }

  @Override
  @Nonnull
  public RangeMarker createRangeMarker(@Nonnull final TextRange textRange) {
    return createRangeMarker(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @Override
  @Nonnull
  public List<RangeMarker> getGuardedBlocks() {
    return Collections.emptyList();
  }

  @Override
  public boolean processRangeMarkers(@Nonnull Processor<? super RangeMarker> processor) {
    return false;
  }

  @Override
  public boolean processRangeMarkersOverlappingWith(int start, int end, @Nonnull Processor<? super RangeMarker> processor) {
    return false;
  }

  @Override
  public int getModificationSequence() {
    return 0;
  }
}
