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
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

// IMPL class hardcoding logic to react to errors/warnings found during highlighting
// DO NOT USE directly
public abstract class HighlightInfoProcessor {
  // HInfos for visible part of file/block are produced.
  // Will remove all range-highlighters from there and replace them with passed infos
  public void highlightsInsideVisiblePartAreProduced(@Nonnull HighlightingSession session,
                                                     @Nullable Editor editor,
                                                     @Nonnull List<? extends HighlightInfo> infos,
                                                     @Nonnull TextRange priorityRange,
                                                     @Nonnull TextRange restrictRange, int groupId) {
  }

  public void highlightsOutsideVisiblePartAreProduced(@Nonnull HighlightingSession session,
                                                      @Nullable Editor editor,
                                                      @Nonnull List<? extends HighlightInfo> infos,
                                                      @Nonnull TextRange priorityRange,
                                                      @Nonnull TextRange restrictedRange, int groupId) {
  }

  // new HInfo became available during highlighting.
  // Incrementally add this HInfo in EDT iff there were nothing there before.
  public void infoIsAvailable(@Nonnull HighlightingSession session, @Nonnull HighlightInfo info, @Nonnull TextRange priorityRange, @Nonnull TextRange restrictedRange, int groupId) {
  }

  // this range is over.
  // Can queue to EDT to remove abandoned bijective highlighters from this range. All the rest abandoned highlighters have to wait until *AreProduced().
  public void allHighlightsForRangeAreProduced(@Nonnull HighlightingSession session, @Nonnull TextRange elementRange, @Nullable List<? extends HighlightInfo> infos) {
  }

  public void progressIsAdvanced(@Nonnull HighlightingSession highlightingSession, @Nullable Editor editor, double progress) {
  }


  private static final HighlightInfoProcessor EMPTY = new HighlightInfoProcessor() {
  };

  @Nonnull
  public static HighlightInfoProcessor getEmpty() {
    return EMPTY;
  }
}
