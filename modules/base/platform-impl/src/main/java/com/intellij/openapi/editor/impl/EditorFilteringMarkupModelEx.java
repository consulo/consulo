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
import com.intellij.openapi.util.Condition;
import com.intellij.util.Consumer;
import com.intellij.util.FilteringProcessor;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.editor.internal.EditorInternal;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class EditorFilteringMarkupModelEx implements MarkupModelEx {
  @Nonnull
  private final EditorInternal myEditor;
  @Nonnull
  private final MarkupModelEx myDelegate;

  private final Condition<RangeHighlighter> IS_AVAILABLE = this::isAvailable;

  public EditorFilteringMarkupModelEx(@Nonnull EditorInternal editor, @Nonnull MarkupModelEx delegate) {
    myEditor = editor;
    myDelegate = delegate;
  }

  @Nonnull
  public MarkupModelEx getDelegate() {
    return myDelegate;
  }

  private boolean isAvailable(@Nonnull RangeHighlighter highlighter) {
    return highlighter.getEditorFilter().avaliableIn(myEditor) && myEditor.isHighlighterAvailable(highlighter);
  }

  @Override
  public boolean containsHighlighter(@Nonnull RangeHighlighter highlighter) {
    return isAvailable(highlighter) && myDelegate.containsHighlighter(highlighter);
  }

  @Override
  public boolean processRangeHighlightersOverlappingWith(int start, int end, @Nonnull Processor<? super RangeHighlighterEx> processor) {
    //noinspection unchecked
    FilteringProcessor<? super RangeHighlighterEx> filteringProcessor = new FilteringProcessor(IS_AVAILABLE, processor);
    return myDelegate.processRangeHighlightersOverlappingWith(start, end, filteringProcessor);
  }

  @Override
  public boolean processRangeHighlightersOutside(int start, int end, @Nonnull Processor<? super RangeHighlighterEx> processor) {
    //noinspection unchecked
    FilteringProcessor<? super RangeHighlighterEx> filteringProcessor = new FilteringProcessor(IS_AVAILABLE, processor);
    return myDelegate.processRangeHighlightersOutside(start, end, filteringProcessor);
  }

  @Override
  @Nonnull
  public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset) {
    return new FilteringMarkupIterator<>(myDelegate.overlappingIterator(startOffset, endOffset), this::isAvailable);
  }

  @Nonnull
  @Override
  public MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset, boolean onlyRenderedInGutter, boolean onlyRenderedInScrollBar) {
    return new FilteringMarkupIterator<>(myDelegate.overlappingIterator(startOffset, endOffset, onlyRenderedInGutter, onlyRenderedInScrollBar), this::isAvailable);
  }

  @Override
  @Nonnull
  public RangeHighlighter[] getAllHighlighters() {
    List<RangeHighlighter> list = ContainerUtil.filter(myDelegate.getAllHighlighters(), IS_AVAILABLE);
    return list.toArray(RangeHighlighter.EMPTY_ARRAY);
  }

  @Override
  public void dispose() {
  }

  //
  // Delegated
  //

  @Override
  @Nonnull
  public Document getDocument() {
    return myDelegate.getDocument();
  }

  @Override
  public void addMarkupModelListener(@Nonnull Disposable parentDisposable, @Nonnull MarkupModelListener listener) {
    myDelegate.addMarkupModelListener(parentDisposable, listener);
  }

  @Override
  public void fireAttributesChanged(@Nonnull RangeHighlighterEx segmentHighlighter, boolean renderersChanged, boolean fontStyleOrColorChanged) {
    myDelegate.fireAttributesChanged(segmentHighlighter, renderersChanged, fontStyleOrColorChanged);
  }

  @Override
  public void fireAfterAdded(@Nonnull RangeHighlighterEx segmentHighlighter) {
    myDelegate.fireAfterAdded(segmentHighlighter);
  }

  @Override
  public void fireBeforeRemoved(@Nonnull RangeHighlighterEx segmentHighlighter) {
    myDelegate.fireBeforeRemoved(segmentHighlighter);
  }

  @Override
  @Nullable
  public RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes) {
    return myDelegate.addPersistentLineHighlighter(lineNumber, layer, textAttributes);
  }

  @Override
  public void addRangeHighlighter(@Nonnull RangeHighlighterEx marker, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer) {
    myDelegate.addRangeHighlighter(marker, start, end, greedyToLeft, greedyToRight, layer);
  }

  @Override
  @Nonnull
  public RangeHighlighter addRangeHighlighter(int startOffset, int endOffset, int layer, @Nullable TextAttributes textAttributes, @Nonnull HighlighterTargetArea targetArea) {
    return myDelegate.addRangeHighlighter(startOffset, endOffset, layer, textAttributes, targetArea);
  }

  @Override
  @Nonnull
  public RangeHighlighter addLineHighlighter(int line, int layer, @Nullable TextAttributes textAttributes) {
    return myDelegate.addLineHighlighter(line, layer, textAttributes);
  }

  @Override
  @Nonnull
  public RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                                   int endOffset,
                                                                   int layer,
                                                                   TextAttributes textAttributes,
                                                                   @Nonnull HighlighterTargetArea targetArea,
                                                                   boolean isPersistent,
                                                                   Consumer<? super RangeHighlighterEx> changeAttributesAction) {
    return myDelegate.addRangeHighlighterAndChangeAttributes(startOffset, endOffset, layer, textAttributes, targetArea, isPersistent, changeAttributesAction);
  }

  @Override
  public void setRangeHighlighterAttributes(@Nonnull RangeHighlighter highlighter, @Nonnull TextAttributes textAttributes) {
    myDelegate.setRangeHighlighterAttributes(highlighter, textAttributes);
  }

  @Override
  public void changeAttributesInBatch(@Nonnull RangeHighlighterEx highlighter, @Nonnull Consumer<? super RangeHighlighterEx> changeAttributesAction) {
    myDelegate.changeAttributesInBatch(highlighter, changeAttributesAction);
  }

  @Override
  public void removeHighlighter(@Nonnull RangeHighlighter rangeHighlighter) {
    myDelegate.removeHighlighter(rangeHighlighter);
  }

  @Override
  public void removeAllHighlighters() {
    myDelegate.removeAllHighlighters();
  }

  @Override
  @Nullable
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myDelegate.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myDelegate.putUserData(key, value);
  }
}
