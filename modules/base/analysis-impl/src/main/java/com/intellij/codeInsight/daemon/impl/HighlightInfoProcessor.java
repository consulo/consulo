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

import com.intellij.openapi.util.TextRange;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public abstract class HighlightInfoProcessor {
  public void highlightsInsideVisiblePartAreProduced(@Nonnull HighlightingSession session,
                                                     @Nonnull List<HighlightInfo> infos,
                                                     @Nonnull TextRange priorityRange,
                                                     @Nonnull TextRange restrictRange,
                                                     int groupId) {
  }

  public void highlightsOutsideVisiblePartAreProduced(@Nonnull HighlightingSession session,
                                                      @Nonnull List<HighlightInfo> infos,
                                                      @Nonnull TextRange priorityRange,
                                                      @Nonnull TextRange restrictedRange,
                                                      int groupId) {
  }

  public void infoIsAvailable(@Nonnull HighlightingSession session,
                              @Nonnull HighlightInfo info,
                              @Nonnull TextRange priorityRange,
                              @Nonnull TextRange restrictedRange,
                              int groupId) {
  }

  public void allHighlightsForRangeAreProduced(@Nonnull HighlightingSession session, @Nonnull TextRange elementRange, @Nullable List<HighlightInfo> infos) {
  }

  public void progressIsAdvanced(@Nonnull HighlightingSession highlightingSession, double progress) {
  }


  private static final HighlightInfoProcessor EMPTY = new HighlightInfoProcessor() {
  };

  @Nonnull
  public static HighlightInfoProcessor getEmpty() {
    return EMPTY;
  }
}
