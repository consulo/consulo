// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
// Converted from CompositeDeclarativeHintWithMarginsView.kt :contentReference[oaicite:0]{index=0}
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.DeclarativeInlayPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.util.lang.TriPredicate;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public abstract class CompositeDeclarativeHintWithMarginsView<Model, SubView extends DeclarativeHintViewWithMargins>
    implements DeclarativeHintView<Model> {
    private final boolean ignoreInitialMargin;
    private SubViewMetrics computedSubViewMetrics;
    private InlayTextMetricsStamp inlayTextMetricsStamp;

    protected CompositeDeclarativeHintWithMarginsView(boolean ignoreInitialMargin) {
        this.ignoreInitialMargin = ignoreInitialMargin;
    }

    protected abstract SubView getSubView(int index);

    protected abstract int getSubViewCount();

    private SubViewMetrics getSubViewMetrics(InlayTextMetricsStorage fontMetricsStorage) {
        SubViewMetrics metrics = computedSubViewMetrics;
        InlayTextMetricsStamp currentStamp = getCurrentTextMetricsStamp(fontMetricsStorage);
        boolean areFontMetricsActual = areFontMetricsActual(currentStamp);
        if (metrics == null || !areFontMetricsActual) {
            SubViewMetrics computed = computeSubViewMetrics(ignoreInitialMargin, fontMetricsStorage, !areFontMetricsActual);
            computedSubViewMetrics = computed;
            inlayTextMetricsStamp = currentStamp;
            return computed;
        }
        return metrics;
    }

    protected InlayTextMetricsStamp getCurrentTextMetricsStamp(InlayTextMetricsStorage fontMetricsStorage) {
        return fontMetricsStorage.getCurrentStamp();
    }

    protected boolean areFontMetricsActual(InlayTextMetricsStamp currentStamp) {
        return Objects.equals(inlayTextMetricsStamp, currentStamp);
    }

    @Override
    public int calcWidthInPixels(Inlay<?> inlay, InlayTextMetricsStorage fontMetricsStorage) {
        return getSubViewMetrics(fontMetricsStorage).fullWidth;
    }

    @Override
    public void paint(Inlay<?> inlay, Graphics2D g, Rectangle2D targetRegion, TextAttributes textAttributes,
                      InlayTextMetricsStorage fontMetricsStorage) {
        forEachSubViewBounds(fontMetricsStorage, (subView, leftBound, rightBound) -> {
            int width = rightBound - leftBound;
            Rectangle currentRegion = new Rectangle((int) targetRegion.getX() + leftBound,
                (int) targetRegion.getY(),
                width,
                (int) targetRegion.getHeight());
            subView.paint(inlay, g, currentRegion, textAttributes, fontMetricsStorage);
            return true;
        });
    }

    @Override
    public void handleLeftClick(EditorMouseEvent e, Point pointInsideInlay,
                                InlayTextMetricsStorage fontMetricsStorage, boolean controlDown) {
        forSubViewAtPoint(pointInsideInlay, fontMetricsStorage,
            (subView, translated) -> subView.handleLeftClick(e, translated, fontMetricsStorage, controlDown));
    }

    @Override
    public LightweightHint handleHover(EditorMouseEvent e, Point pointInsideInlay,
                                       InlayTextMetricsStorage fontMetricsStorage) {
        class HintHolder {
            LightweightHint hint;
        }
        HintHolder holder = new HintHolder();
        forSubViewAtPoint(pointInsideInlay, fontMetricsStorage,
            (subView, translated) -> holder.hint = subView.handleHover(e, translated, fontMetricsStorage));
        return holder.hint;
    }

    @Override
    public void handleRightClick(EditorMouseEvent e, Point pointInsideInlay,
                                 InlayTextMetricsStorage fontMetricsStorage) {
        forSubViewAtPoint(pointInsideInlay, fontMetricsStorage,
            (subView, translated) -> subView.handleRightClick(e, translated, fontMetricsStorage));
    }

    @Override
    public InlayMouseArea getMouseArea(Point pointInsideInlay, InlayTextMetricsStorage fontMetricsStorage) {
        class AreaHolder {
            InlayMouseArea area;
        }
        AreaHolder holder = new AreaHolder();
        forSubViewAtPoint(pointInsideInlay, fontMetricsStorage,
            (subView, translated) -> holder.area = subView.getMouseArea(translated, fontMetricsStorage));
        return holder.area;
    }

    private void forSubViewAtPoint(Point pointInsideInlay, InlayTextMetricsStorage fontMetricsStorage,
                                   BiConsumer<SubView, Point> action) {
        int x = pointInsideInlay.x;
        forEachSubViewBounds(fontMetricsStorage, (subView, leftBound, rightBound) -> {
            if (x >= leftBound && x < rightBound) {
                action.accept(subView, new Point(x - leftBound, pointInsideInlay.y));
                return false;
            }
            return true;
        });
    }

    private void forEachSubViewBounds(InlayTextMetricsStorage fontMetricsStorage,
                                      TriPredicate<SubView, Integer, Integer> action) {
        int[] sortedBounds = getSubViewMetrics(fontMetricsStorage).sortedBounds;
        for (int index = 0; index < getSubViewCount(); index++) {
            int leftBound = sortedBounds[2 * index];
            int rightBound = sortedBounds[2 * index + 1];

            if (!action.test(getSubView(index), leftBound, rightBound)) {
                break;
            }
        }
    }

    private SubViewMetrics computeSubViewMetrics(boolean ignoreInitialMargin,
                                                 InlayTextMetricsStorage fontMetricsStorage,
                                                 boolean forceUpdate) {
        int count = getSubViewCount();
        int[] sortedBounds = new int[count * 2];
        int xSoFar = 0;
        int previousMargin = 0;
        var first = getSubView(0);
        int margin0 = first.getMargin();
        sortedBounds[0] = ignoreInitialMargin ? 0 : margin0;
        sortedBounds[1] = sortedBounds[0] + first.getBoxWidth(fontMetricsStorage, forceUpdate);
        previousMargin = margin0;
        xSoFar = sortedBounds[1];
        for (int i = 1; i < count; i++) {
            var sub = getSubView(i);
            int margin = sub.getMargin();
            int left = xSoFar + Math.max(previousMargin, margin);
            int right = left + sub.getBoxWidth(fontMetricsStorage, forceUpdate);
            sortedBounds[2 * i] = left;
            sortedBounds[2 * i + 1] = right;
            previousMargin = margin;
            xSoFar = right;
        }
        return new SubViewMetrics(sortedBounds, xSoFar + previousMargin);
    }

    void invalidateComputedSubViewMetrics() {
        computedSubViewMetrics = null;
    }

    private static <M, S extends DeclarativeHintViewWithMargins> InlayPresentationList createPresentationList(CompositeDeclarativeHintWithMarginsView<M, S> view,
                                                                                                              InlayData inlayData) {
        return new InlayPresentationList(inlayData, view::invalidateComputedSubViewMetrics);
    }

    public static final class SubViewMetrics {
        public final int[] sortedBounds;
        public final int fullWidth;

        public SubViewMetrics(int[] sortedBounds, int fullWidth) {
            this.sortedBounds = sortedBounds;
            this.fullWidth = fullWidth;
        }
    }

    public static class SingleDeclarativeHintView
        extends CompositeDeclarativeHintWithMarginsView<InlayData, InlayPresentationList> {
        private final InlayPresentationList presentationList;

        public SingleDeclarativeHintView(InlayData inlayData) {
            super(false);
            this.presentationList = createPresentationList(this, inlayData);
        }

        public InlayPresentationList getPresentationList() {
            return presentationList;
        }

        @Override
        protected int getSubViewCount() {
            return 1;
        }

        @Override
        protected InlayPresentationList getSubView(int index) {
            return presentationList;
        }

        @RequiredUIAccess
        @Override
        public void updateModel(InlayData newModel) {
            presentationList.updateModel(newModel);
        }
    }

    public static class MultipleDeclarativeHintsView
        extends CompositeDeclarativeHintWithMarginsView<List<InlayData>, InlayPresentationList> {
        private List<InlayPresentationList> presentationLists;

        public MultipleDeclarativeHintsView(List<InlayData> inlayData) {
            super(true);
            if (inlayData.size() == 1) {
                this.presentationLists = Collections.singletonList(createPresentationList(this, inlayData.get(0)));
            }
            else {
                List<InlayPresentationList> lists = new ArrayList<>();
                for (InlayData data : inlayData) {
                    lists.add(createPresentationList(this, data));
                }
                this.presentationLists = lists;
            }
        }

        public List<InlayPresentationList> getPresentationLists() {
            return presentationLists;
        }

        @Override
        protected int getSubViewCount() {
            return presentationLists.size();
        }

        @Override
        protected InlayPresentationList getSubView(int index) {
            return presentationLists.get(index);
        }

        @RequiredUIAccess
        @Override
        public void updateModel(List<InlayData> newModel) {
            if (newModel.size() == presentationLists.size()) {
                for (int i = 0; i < presentationLists.size(); i++) {
                    presentationLists.get(i).updateModel(newModel.get(i));
                }
                return;
            }
            int oldIndex = 0, newIndex = 0;
            List<InlayPresentationList> newPresentationLists = new ArrayList<>(newModel.size());
            while (oldIndex < presentationLists.size() && newIndex < newModel.size()) {
                int oldPrio = getPriority(presentationLists.get(oldIndex).getModel().getPosition());
                int newPrio = getPriority(newModel.get(newIndex).getPosition());
                if (oldPrio == newPrio) {
                    InlayPresentationList pl = presentationLists.get(oldIndex);
                    pl.updateModel(newModel.get(newIndex));
                    newPresentationLists.add(pl);
                    oldIndex++;
                    newIndex++;
                }
                else if (oldPrio < newPrio) {
                    oldIndex++;
                }
                else {
                    newPresentationLists.add(createPresentationList(this, newModel.get(newIndex)));
                    newIndex++;
                }
            }
            while (newIndex < newModel.size()) {
                newPresentationLists.add(createPresentationList(this, newModel.get(newIndex)));
                newIndex++;
            }
            presentationLists = newPresentationLists;
            invalidateComputedSubViewMetrics();
        }
    }

    private static int getPriority(DeclarativeInlayPosition pos) {
        if (pos instanceof DeclarativeInlayPosition.AboveLineIndentedPosition) {
            return ((DeclarativeInlayPosition.AboveLineIndentedPosition) pos).getPriority();
        }
        else if (pos instanceof DeclarativeInlayPosition.EndOfLinePosition) {
            return ((DeclarativeInlayPosition.EndOfLinePosition) pos).getPriority();
        }
        else if (pos instanceof DeclarativeInlayPosition.InlineInlayPosition) {
            return ((DeclarativeInlayPosition.InlineInlayPosition) pos).getPriority();
        }
        throw new IllegalArgumentException("Unknown InlayPosition: " + pos.getClass());
    }
}
