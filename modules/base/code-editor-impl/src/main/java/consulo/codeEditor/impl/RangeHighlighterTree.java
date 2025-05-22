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
package consulo.codeEditor.impl;

import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.document.Document;
import consulo.document.MarkupIterator;
import consulo.document.impl.RangeMarkerTree;
import consulo.document.impl.TextRangeInterval;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

class RangeHighlighterTree extends RangeMarkerTree<RangeHighlighterEx> {
    private final MarkupModelEx myMarkupModel;

    RangeHighlighterTree(@Nonnull Document document, @Nonnull MarkupModelEx markupModel) {
        super(document);
        myMarkupModel = markupModel;
    }

    void updateRenderedFlags(RangeHighlighterEx highlighter) {
        RHNode node = (RHNode) lookupNode(highlighter);
        if (node != null) {
            node.recalculateRenderFlagsUp();
        }
    }

    @Override
    public void correctMax(@Nonnull IntervalNode<RangeHighlighterEx> node, int deltaUpToRoot) {
        super.correctMax(node, deltaUpToRoot);
        ((RHNode) node).recalculateRenderFlags();
    }

    @Override
    protected int compareEqualStartIntervals(@Nonnull IntervalNode<RangeHighlighterEx> i1, @Nonnull IntervalNode<RangeHighlighterEx> i2) {
        RHNode o1 = (RHNode) i1;
        RHNode o2 = (RHNode) i2;
        int d = o2.myLayer - o1.myLayer;
        if (d != 0) {
            return d;
        }
        return super.compareEqualStartIntervals(i1, i2);
    }

    @Nonnull
    @Override
    protected RHNode createNewNode(@Nonnull RangeHighlighterEx key, int start, int end, boolean greedyToLeft, boolean greedyToRight, boolean stickingToRight, int layer) {
        return new RHNode(this, key, start, end, greedyToLeft, greedyToRight, stickingToRight, layer);
    }

    static class RHNode extends RMNode<RangeHighlighterEx> {
        private static final byte RENDERED_IN_GUTTER_FLAG = STICK_TO_RIGHT_FLAG << 1;

        final int myLayer;

        RHNode(@Nonnull RangeHighlighterTree rangeMarkerTree, @Nonnull final RangeHighlighterEx key, int start, int end, boolean greedyToLeft, boolean greedyToRight, boolean stickingToRight, int layer) {
            super(rangeMarkerTree, key, start, end, greedyToLeft, greedyToRight, stickingToRight);
            myLayer = layer;
        }

        //range highlighters are strongly referenced
        @Override
        protected Supplier<RangeHighlighterEx> createGetter(@Nonnull RangeHighlighterEx interval) {
            //noinspection unchecked
            return (Supplier<RangeHighlighterEx>) interval;
        }

        private void recalculateRenderFlags() {
            boolean renderedInGutter = false;
            for (Supplier<? extends RangeHighlighterEx> getter : intervals) {
                RangeHighlighterEx h = getter.get();
                renderedInGutter |= h.isRenderedInGutter();
            }
            RHNode left = (RHNode) getLeft();
            if (left != null) {
                renderedInGutter |= left.isRenderedInGutter();
            }
            RHNode right = (RHNode) getRight();
            if (right != null) {
                renderedInGutter |= right.isRenderedInGutter();
            }
            setFlag(RENDERED_IN_GUTTER_FLAG, renderedInGutter);
        }

        private void recalculateRenderFlagsUp() {
            RHNode n = this;
            while (n != null) {
                boolean prevInGutter = n.isRenderedInGutter();
                n.recalculateRenderFlags();
                if (n.isRenderedInGutter() == prevInGutter) {
                    break;
                }
                n = (RHNode) n.getParent();
            }
        }

        @Override
        public void addInterval(@Nonnull RangeHighlighterEx h) {
            super.addInterval(h);

            if (!isRenderedInGutter() && h.isRenderedInGutter()) {
                recalculateRenderFlagsUp();
            }
        }

        @Override
        public void removeIntervalInternal(int i) {
            RangeHighlighterEx h = intervals.get(i).get();
            boolean recalculateFlags = h.isRenderedInGutter();
            super.removeIntervalInternal(i);
            if (recalculateFlags) {
                recalculateRenderFlagsUp();
            }
        }

        boolean isRenderedInGutter() {
            return isFlagSet(RENDERED_IN_GUTTER_FLAG);
        }
    }

    @Override
    public void fireBeforeRemoved(@Nonnull RangeHighlighterEx markerEx, @Nonnull Object reason) {
        myMarkupModel.fireBeforeRemoved(markerEx);
    }
}
