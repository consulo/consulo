/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.editor;

import com.intellij.openapi.util.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a region of text in the editor which can be folded.
 *
 * @see FoldingModel#addFoldRegion(int, int, String)
 * @see FoldingModel#addFoldRegion(FoldRegion)
 * @see FoldingModel#getAllFoldRegions()
 */
public interface FoldRegion extends RangeMarker {
  FoldRegion[] EMPTY_ARRAY = new FoldRegion[0];

  /**
   * If {@code Boolean.TRUE} value is set for this key on a collapsed fold region (see {@link #putUserData(Key, Object)}),
   * there will not be a visual indication that region contains certain highlighters inside. By default such indication is added.
   *
   * @see com.intellij.openapi.editor.markup.RangeHighlighter#VISIBLE_IF_FOLDED
   */
  Key<Boolean> MUTE_INNER_HIGHLIGHTERS = Key.create("mute.inner.highlighters");

  /**
   * Checks if the fold region is currently expanded.
   *
   * @return true if the fold region is expanded, false otherwise.
   */
  boolean isExpanded();

  /**
   * Expands or collapses the fold region.
   *
   * @param expanded true if the region should be expanded, false otherwise.
   */
  void setExpanded(boolean expanded);

  /**
   * Returns the placeholder text displayed when the fold region is collapsed.
   *
   * @return the placeholder text.
   */
  @Nonnull
  String getPlaceholderText();

  Editor getEditor();

  @Nullable
  FoldingGroup getGroup();

  boolean shouldNeverExpand();
}
