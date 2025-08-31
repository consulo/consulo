// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.application.ApplicationManager;
import consulo.application.util.function.CommonProcessors;
import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.internal.SweepProcessor;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.impl.RangeMarkerTree;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.lang.IntPair;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;

public abstract class FoldRegionsTree {
    private final RangeMarkerTree<FoldRegionImpl> myMarkerTree;
    @Nonnull
    private volatile CachedData myCachedData = new CachedData();

    private static final Comparator<FoldRegion> BY_END_OFFSET = Comparator.comparingInt(RangeMarker::getEndOffset);
    private static final Comparator<? super FoldRegion> BY_END_OFFSET_REVERSE = Collections.reverseOrder(BY_END_OFFSET);

    public static final HashingStrategy<FoldRegion> OFFSET_BASED_HASHING_STRATEGY = new HashingStrategy<FoldRegion>() {
        @Override
        public int hashCode(FoldRegion o) {
            return o.getStartOffset() * 31 + o.getEndOffset();
        }

        @Override
        public boolean equals(FoldRegion o1, FoldRegion o2) {
            return o1.getStartOffset() == o2.getStartOffset() && o1.getEndOffset() == o2.getEndOffset();
        }
    };

    public FoldRegionsTree(@Nonnull RangeMarkerTree<FoldRegionImpl> markerTree) {
        myMarkerTree = markerTree;
    }

    public void clear() {
        clearCachedValues();
        myMarkerTree.clear();
    }

    public void clearCachedValues() {
        myCachedData = new CachedData();
    }

    public void clearCachedInlayValues() {
        myCachedData.topFoldedInlaysHeightValid = false;
    }

    protected abstract boolean isFoldingEnabled();

    protected abstract boolean hasBlockInlays();

    protected abstract int getFoldedBlockInlaysHeight(int foldStartOffset, int foldEndOffset);

    protected abstract int getLineHeight();

    public CachedData rebuild() {
        List<FoldRegion> visible = new ArrayList<>(myMarkerTree.size());

        SweepProcessor.Generator<FoldRegionImpl> generator = processor -> myMarkerTree.processOverlappingWith(0, Integer.MAX_VALUE, processor);
        SweepProcessor.sweep(generator, new SweepProcessor<FoldRegionImpl>() {
            FoldRegionImpl lastCollapsedRegion;

            @Override
            public boolean process(int offset, @Nonnull FoldRegionImpl region, boolean atStart, @Nonnull Collection<FoldRegionImpl> overlapping) {
                if (atStart) {
                    if (lastCollapsedRegion == null || region.getEndOffset() > lastCollapsedRegion.getEndOffset()) {
                        if (!region.isExpanded()) {
                            hideContainedRegions(region);
                            lastCollapsedRegion = region;
                        }
                        visible.add(region);
                    }
                }
                return true;
            }

            private void hideContainedRegions(FoldRegion region) {
                for (int i = visible.size() - 1; i >= 0; i--) {
                    if (region.getStartOffset() == visible.get(i).getStartOffset()) {
                        visible.remove(i);
                    }
                    else {
                        break;
                    }
                }
            }
        });

        FoldRegion[] visibleRegions = toFoldArray(visible);

        Arrays.sort(visibleRegions, BY_END_OFFSET_REVERSE);

        return updateCachedAndSortOffsets(visibleRegions, true);
    }

    @Nonnull
    private static FoldRegion[] toFoldArray(@Nonnull List<FoldRegion> topLevels) {
        return topLevels.isEmpty() ? FoldRegion.EMPTY_ARRAY : topLevels.toArray(FoldRegion.EMPTY_ARRAY);
    }

    public void updateCachedOffsets() {
        CachedData cachedData = myCachedData;
        updateCachedAndSortOffsets(cachedData.visibleRegions, false);
    }

    private CachedData updateCachedAndSortOffsets(FoldRegion[] visibleRegions, boolean fromRebuild) {
        if (!isFoldingEnabled()) {
            return null;
        }
        if (visibleRegions == null) {
            return rebuild();
        }

        List<FoldRegion> topLevel = new ArrayList<>(visibleRegions.length / 2);

        for (FoldRegion region : visibleRegions) {
            if (!region.isValid()) {
                if (fromRebuild) {
                    throw new RuntimeExceptionWithAttachments("FoldRegionsTree.rebuild() failed",
                        AttachmentFactory.get().create("visibleRegions.txt", Arrays.toString(visibleRegions)));
                }
                return rebuild();
            }
            if (!region.isExpanded()) {
                topLevel.add(region);
            }
        }
        FoldRegion[] topLevelRegions = topLevel.toArray(FoldRegion.EMPTY_ARRAY);
        Arrays.sort(topLevelRegions, BY_END_OFFSET);

        int[] startOffsets = ArrayUtil.newIntArray(topLevelRegions.length);
        int[] endOffsets = ArrayUtil.newIntArray(topLevelRegions.length);
        int[] foldedLines = ArrayUtil.newIntArray(topLevelRegions.length);
        int[] customYAdjustment = ArrayUtil.newIntArray(topLevelRegions.length);

        int foldedLinesSum = 0;
        int currentCustomYAdjustment = 0;
        int lineHeight = getLineHeight();
        for (int i = 0; i < topLevelRegions.length; i++) {
            FoldRegion region = topLevelRegions[i];
            startOffsets[i] = region.getStartOffset();
            endOffsets[i] = region.getEndOffset() - 1;
            Document document = region.getDocument();
            foldedLinesSum += document.getLineNumber(region.getEndOffset()) - document.getLineNumber(region.getStartOffset());
            foldedLines[i] = foldedLinesSum;
            if (region instanceof CustomFoldRegion) {
                currentCustomYAdjustment += ((CustomFoldRegion) region).getHeightInPixels() - lineHeight;
            }
            customYAdjustment[i] = currentCustomYAdjustment;
        }

        CachedData data = new CachedData(visibleRegions, topLevelRegions, startOffsets, endOffsets, foldedLines, customYAdjustment);
        myCachedData = data;
        return data;
    }

    public boolean checkIfValidToCreate(int start, int end) {
        // check that range doesn't strictly overlaps other regions and is distinct from everything else
        return myMarkerTree.processOverlappingWith(start, end, region -> {
            int rStart = region.getStartOffset();
            int rEnd = region.getEndOffset();
            if (rStart < start) {
                if (region.isValid() && start < rEnd && rEnd < end) {
                    return false;
                }
            }
            else if (rStart == start) {
                if (rEnd == end) {
                    return false;
                }
            }
            else {
                if (rStart > end) {
                    return true;
                }
                if (region.isValid() && rStart < end && end < rEnd) {
                    return false;
                }
            }
            return true;
        });
    }

    private CachedData ensureAvailableData() {
        CachedData cachedData = myCachedData;
        if (!cachedData.isAvailable() && ApplicationManager.getApplication().isDispatchThread()) {
            return rebuild();
        }
        return cachedData;
    }

    @Nullable
    public FoldRegion fetchOutermost(int offset) {
        if (!isFoldingEnabled()) {
            return null;
        }
        CachedData cachedData = ensureAvailableData();

        int[] starts = cachedData.topStartOffsets;
        int[] ends = cachedData.topEndOffsets;
        if (starts == null || ends == null) {
            return null;
        }

        int i = ObjectUtil.binarySearch(0, ends.length, mid -> ends[mid] < offset ? -1 : starts[mid] > offset ? 1 : 0);
        return i < 0 ? null : cachedData.topLevelRegions[i];
    }

    @Nullable
    public FoldRegion[] fetchVisible() {
        if (!isFoldingEnabled()) {
            return null;
        }
        CachedData cachedData = ensureAvailableData();

        return cachedData.visibleRegions;
    }

    @Nullable
    public FoldRegion[] fetchTopLevel() {
        if (!isFoldingEnabled()) {
            return null;
        }
        CachedData cachedData = ensureAvailableData();
        return cachedData.topLevelRegions;
    }

    public static boolean containsStrict(FoldRegion region, int offset) {
        return region.getStartOffset() < offset && offset < region.getEndOffset();
    }

    @Nonnull
    public FoldRegion[] fetchCollapsedAt(int offset) {
        if (!isFoldingEnabled()) {
            return FoldRegion.EMPTY_ARRAY;
        }
        List<FoldRegion> allCollapsed = new ArrayList<>();
        myMarkerTree.processContaining(offset, region -> {
            if (!region.isExpanded() && containsStrict(region, offset)) {
                allCollapsed.add(region);
            }
            return true;
        });
        return toFoldArray(allCollapsed);
    }

    public boolean intersectsRegion(int startOffset, int endOffset) {
        if (!isFoldingEnabled()) {
            return true;
        }
        return !myMarkerTree.processAll(region -> {
            boolean contains1 = containsStrict(region, startOffset);
            boolean contains2 = containsStrict(region, endOffset);
            return contains1 == contains2;
        });
    }

    @Nonnull
    public FoldRegion[] fetchAllRegions() {
        if (!isFoldingEnabled()) {
            return FoldRegion.EMPTY_ARRAY;
        }
        List<FoldRegion> regions = new ArrayList<>();
        myMarkerTree.processOverlappingWith(0, Integer.MAX_VALUE, new CommonProcessors.CollectProcessor<>(regions));
        return toFoldArray(regions);
    }

    private void forEach(@Nonnull Consumer<? super FoldRegion> consumer) {
        myMarkerTree.processAll(region -> {
            consumer.accept(region);
            return true;
        });
    }

    public int getFoldedLinesCountBefore(int offset) {
        if (!isFoldingEnabled()) {
            return 0;
        }
        CachedData cachedData = ensureAvailableData();
        int idx = getLastTopLevelIndexBefore(cachedData, offset);
        if (idx == -1) {
            return 0;
        }
        assert cachedData.topFoldedLines != null;
        return cachedData.topFoldedLines[idx];
    }

    public int getTotalNumberOfFoldedLines() {
        if (!isFoldingEnabled()) {
            return 0;
        }
        CachedData cachedData = ensureAvailableData();
        int[] foldedLines = cachedData.topFoldedLines;

        if (foldedLines == null || foldedLines.length == 0) {
            return 0;
        }
        return foldedLines[foldedLines.length - 1];
    }

    public int getHeightOfFoldedBlockInlaysBefore(int offset) {
        if (!isFoldingEnabled()) {
            return 0;
        }
        CachedData cachedData = ensureAvailableData();
        int idx = getLastTopLevelIndexBefore(cachedData, offset);
        if (idx == -1) {
            return 0;
        }
        cachedData.ensureInlayDataAvailable();
        int[] topFoldedInlaysHeight = cachedData.topFoldedInlaysHeight;
        return topFoldedInlaysHeight == null ? 0 : topFoldedInlaysHeight[idx];
    }

    public int getTotalHeightOfFoldedBlockInlays() {
        if (!isFoldingEnabled()) {
            return 0;
        }
        CachedData cachedData = ensureAvailableData();
        cachedData.ensureInlayDataAvailable();
        int[] foldedInlaysHeight = cachedData.topFoldedInlaysHeight;
        return foldedInlaysHeight == null || foldedInlaysHeight.length == 0 ? 0 : foldedInlaysHeight[foldedInlaysHeight.length - 1];
    }

    public int getLastTopLevelIndexBefore(int offset) {
        if (!isFoldingEnabled()) {
            return -1;
        }
        CachedData cachedData = ensureAvailableData();
        return getLastTopLevelIndexBefore(cachedData, offset);
    }

    private static int getLastTopLevelIndexBefore(CachedData cachedData, int offset) {
        int[] endOffsets = cachedData.topEndOffsets;

        if (endOffsets == null) {
            return -1;
        }

        offset--; // end offsets are decremented in cache
        int i = Arrays.binarySearch(endOffsets, offset);
        return i < 0 ? -i - 2 : i;
    }

    /**
     * @return (prevAdjustment, curAdjustment)
     */
    @Nonnull
    IntPair getCustomRegionsYAdjustment(int offset, int idx) {
        if (!isFoldingEnabled()) {
            return new IntPair(0, 0);
        }
        CachedData cachedData = ensureAvailableData();
        int prevAdjustment = idx == -1 ? 0 : cachedData.topCustomYAdjustment[idx];
        int curAdjustment = idx + 1 < cachedData.topStartOffsets.length && cachedData.topStartOffsets[idx + 1] == offset
            ? cachedData.topCustomYAdjustment[idx + 1] - prevAdjustment : 0;
        return new IntPair(prevAdjustment, curAdjustment);
    }

    @Nullable
    public FoldRegion getRegionAt(int startOffset, int endOffset) {
        FoldRegionImpl[] found = {null};
        myMarkerTree.processOverlappingWith(startOffset, endOffset, region -> {
            if (region.getStartOffset() == startOffset && region.getEndOffset() == endOffset) {
                found[0] = region;
                return false;
            }
            return true;
        });
        return found[0];
    }

    public void clearDocumentRangesModificationStatus() {
        forEach(region -> ((FoldRegionImpl) region).resetDocumentRegionChanged());
    }

    private class CachedData {
        private final FoldRegion[] visibleRegions;  // all foldings outside collapsed regions
        private final FoldRegion[] topLevelRegions; // all visible regions which are collapsed
        private final int[] topStartOffsets;
        private final int[] topEndOffsets;
        private final int[] topFoldedLines;
        private final int[] topCustomYAdjustment;
        private int[] topFoldedInlaysHeight;
        private boolean topFoldedInlaysHeightValid;

        private CachedData() {
            visibleRegions = null;
            topLevelRegions = null;
            topStartOffsets = null;
            topEndOffsets = null;
            topFoldedLines = null;
            topCustomYAdjustment = null;
        }

        private CachedData(@Nonnull FoldRegion[] visibleRegions,
                           @Nonnull FoldRegion[] topLevelRegions,
                           @Nonnull int[] topStartOffsets,
                           @Nonnull int[] topEndOffsets,
                           @Nonnull int[] topFoldedLines,
                           @Nonnull int[] topCustomYAdjustment) {
            this.visibleRegions = visibleRegions;
            this.topLevelRegions = topLevelRegions;
            this.topStartOffsets = topStartOffsets;
            this.topEndOffsets = topEndOffsets;
            this.topFoldedLines = topFoldedLines;
            this.topCustomYAdjustment = topCustomYAdjustment;
            ensureInlayDataAvailable();
        }

        private boolean isAvailable() {
            return visibleRegions != null;
        }

        private void ensureInlayDataAvailable() {
            if (topFoldedInlaysHeightValid || !ApplicationManager.getApplication().isDispatchThread()) {
                return;
            }
            topFoldedInlaysHeightValid = true;
            if (hasBlockInlays()) {
                int count = topLevelRegions.length;
                topFoldedInlaysHeight = ArrayUtil.newIntArray(count);
                int inlaysHeightSum = 0;
                for (int i = 0; i < count; i++) {
                    inlaysHeightSum += getFoldedBlockInlaysHeight(topStartOffsets[i], topEndOffsets[i] + 1);
                    topFoldedInlaysHeight[i] = inlaysHeightSum;
                }
            }
            else {
                topFoldedInlaysHeight = null;
            }
        }
    }
}
