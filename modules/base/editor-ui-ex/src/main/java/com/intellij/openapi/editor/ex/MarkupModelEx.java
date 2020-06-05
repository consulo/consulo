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
package com.intellij.openapi.editor.ex;

import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public interface MarkupModelEx extends MarkupModel {
  void dispose();

  @Nullable
  RangeHighlighterEx addPersistentLineHighlighter(int lineNumber, int layer, TextAttributes textAttributes);

  void fireAttributesChanged(@Nonnull RangeHighlighterEx segmentHighlighter, boolean renderersChanged, boolean fontStyleChanged);

  void fireAfterAdded(@Nonnull RangeHighlighterEx segmentHighlighter);

  void fireBeforeRemoved(@Nonnull RangeHighlighterEx segmentHighlighter);

  boolean containsHighlighter(@Nonnull RangeHighlighter highlighter);

  void addRangeHighlighter(@Nonnull RangeHighlighterEx marker, int start, int end, boolean greedyToLeft, boolean greedyToRight, int layer);

  void addMarkupModelListener(@Nonnull Disposable parentDisposable, @Nonnull MarkupModelListener listener);

  void setRangeHighlighterAttributes(@Nonnull RangeHighlighter highlighter, @Nonnull TextAttributes textAttributes);

  boolean processRangeHighlightersOverlappingWith(int start, int end, @Nonnull Processor<? super RangeHighlighterEx> processor);

  boolean processRangeHighlightersOutside(int start, int end, @Nonnull Processor<? super RangeHighlighterEx> processor);

  @Nonnull
  MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset);

  @Nonnull
  MarkupIterator<RangeHighlighterEx> overlappingIterator(int startOffset, int endOffset, boolean onlyRenderedInGutter, boolean onlyRenderedInScrollBar);

  // optimization: creates highlighter and fires only one event: highlighterCreated
  @Nonnull
  RangeHighlighterEx addRangeHighlighterAndChangeAttributes(int startOffset,
                                                            int endOffset,
                                                            int layer,
                                                            TextAttributes textAttributes,
                                                            @Nonnull HighlighterTargetArea targetArea,
                                                            boolean isPersistent,
                                                            Consumer<? super RangeHighlighterEx> changeAttributesAction);

  // runs change attributes action and fires highlighterChanged event if there were changes
  void changeAttributesInBatch(@Nonnull RangeHighlighterEx highlighter, @Nonnull Consumer<? super RangeHighlighterEx> changeAttributesAction);
}
