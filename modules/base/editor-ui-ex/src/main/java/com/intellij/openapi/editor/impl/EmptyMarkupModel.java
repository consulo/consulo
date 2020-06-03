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
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.MarkupIterator;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.progress.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is mock implementation to be used in null-object pattern where necessary.
 *
 * @author max
 */
public class EmptyMarkupModel implements MarkupModelEx {
  private final Document myDocument;

  public EmptyMarkupModel(final Document document) {
    myDocument = document;
  }

  @Override
  @Nonnull
  public Document getDocument() {
    return myDocument;
  }

  @Override
  @Nonnull
  public RangeHighlighter addRangeHighlighter(int startOffset, int endOffset, int layer, @Nullable TextAttributes textAttributes, @Nonnull HighlighterTargetArea targetArea) {
    throw new ProcessCanceledException();
  }

  @Nonnull
  @Override
  public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                                   int endOffset,
                                                                   int layer,
                                                                   TextAttributes textAttributes,
                                                                   @Nonnull HighlighterTargetArea targetArea,
                                                                   boolean isPersistent,
                                                                   Consumer<? super RangeHighlighterEx> changeAttributesAction) {
    throw new ProcessCanceledException();
  }

  @Override
  public void changeAttributesInBatch(@Nonnull RangeHighlighterEx highlighter, @Nonnull Consumer<? super RangeHighlighterEx> changeAttributesAction) {
  }

  @Override
  @Nonnull
  public RangeHighlighter addLineHighlighter(int line, int layer, @Nullable TextAttributes textAttributes) {
    throw new ProcessCanceledException();
  }

  @Override
  public void removeHighlighter(@Nonnull RangeHighlighter rangeHighlighter) {
  }

  @Override
  public void removeAllHighlighters() {
  }

  @Override
  @Nonnull
  public RangeHighlighter[] getAllHighlighters() {
    return RangeHighlighter.EMPTY_ARRAY;
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return null;
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, T value) {
  }

  @Override
  public void dispose() {
  }

  @Override
  public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
    return null;
  }

  @Override
  public boolean containsHighlighter(@Nonnull RangeHighlighter highlighter) {
    return false;
  }

  @Override
  public void addMarkupModelListener(@Nonnull Disposable parentDisposable, @Nonnull MarkupModelListener listener) {
  }

  @Override
  public void setRangeHighlighterAttributes(@Nonnull final RangeHighlighter highlighter, @Nonnull final TextAttributes textAttributes) {

  }

  @Override
  public boolean processRangeHighlightersOverlappingWith(int start, int end, @Nonnull Processor<? super RangeHighlighterEx> processor) {
    return false;
  }

  @Override
  public boolean processRangeHighlightersOutside(int start, int end, @Nonnull Processor<? super RangeHighlighterEx> processor) {
    return false;
  }

  @Nonnull
  @Override
  public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
    return MarkupIterator.EMPTY;
  }

  @Nonnull
  @Override
  public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset, boolean onlyRenderedInGutter, boolean onlyRenderedInScrollBar) {
    return MarkupIterator.EMPTY;
  }

  @Override
  public void fireAttributesChanged(@Nonnull RangeHighlighterEx segmentHighlighter, boolean renderersChanged, boolean fontStyleChanged) {

  }

  @Override
  public void fireAfterAdded(@Nonnull RangeHighlighterEx segmentHighlighter) {

  }

  @Override
  public void fireBeforeRemoved(@Nonnull RangeHighlighterEx segmentHighlighter) {

  }

  @Override
  public void addRangeHighlighter(@Nonnull RangeHighlighterEx marker, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {

  }
}
