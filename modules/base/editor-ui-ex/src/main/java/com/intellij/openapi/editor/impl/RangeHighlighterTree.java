/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.util.Getter;
import javax.annotation.Nonnull;

class RangeHighlighterTree extends RangeMarkerTree<RangeHighlighterEx> {
  private final MarkupModelEx myMarkupModel;

  RangeHighlighterTree(@Nonnull Document document, @Nonnull MarkupModelEx markupModel) {
    super(document);
    myMarkupModel = markupModel;
  }

  @Override
  protected int compareEqualStartIntervals(@Nonnull IntervalNode<RangeHighlighterEx> i1, @Nonnull IntervalNode<RangeHighlighterEx> i2) {
    RHNode o1 = (RHNode)i1;
    RHNode o2 = (RHNode)i2;
    int d = o2.myLayer - o1.myLayer;
    if (d != 0) {
      return d;
    }
    return super.compareEqualStartIntervals(i1, i2);
  }

  @Nonnull
  @Override
  protected RHNode createNewNode(@Nonnull RangeHighlighterEx key, int start, int end,
                                 boolean greedyToLeft, boolean greedyToRight, boolean stickingToRight, int layer) {
    return new RHNode(this, key, start, end, greedyToLeft, greedyToRight, stickingToRight, layer);
  }

  static class RHNode extends RMNode<RangeHighlighterEx> {
    final int myLayer;

    RHNode(@Nonnull RangeHighlighterTree rangeMarkerTree,
           @Nonnull final RangeHighlighterEx key,
           int start,
           int end,
           boolean greedyToLeft,
           boolean greedyToRight,
           boolean stickingToRight,
           int layer) {
      super(rangeMarkerTree, key, start, end, greedyToLeft, greedyToRight, stickingToRight);
      myLayer = layer;
    }

    //range highlighters are strongly referenced
    @Override
    protected Getter<RangeHighlighterEx> createGetter(@Nonnull RangeHighlighterEx interval) {
      //noinspection unchecked
      return (Getter<RangeHighlighterEx>)interval;
    }
  }

  @Override
  void fireBeforeRemoved(@Nonnull RangeHighlighterEx markerEx, @Nonnull Object reason) {
    myMarkupModel.fireBeforeRemoved(markerEx);
  }
}
