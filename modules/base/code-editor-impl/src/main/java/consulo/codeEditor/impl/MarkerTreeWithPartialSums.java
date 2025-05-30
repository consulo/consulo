// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl;

import consulo.document.Document;
import consulo.document.impl.RangeMarkerImpl;
import consulo.document.impl.RangeMarkerTree;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Extension of {@link RangeMarkerTree} which can quickly calculate sum of values associated with markers for a given offset range.
 * Only 'non-greedy' markers with zero length are supported (for such markers start offset is always equal to end offset).
 * Not thread safe - cannot be used from multiple threads simultaneously.
 */
public class MarkerTreeWithPartialSums<T extends RangeMarkerImpl & IntSupplier> extends HardReferencingRangeMarkerTree<T> {
    public MarkerTreeWithPartialSums(@Nonnull Document document) {
        super(document);
    }

    @Override
    protected Node<T> getRoot() {
        return (Node<T>) super.getRoot();
    }

    /**
     * Should be called whenever a value associated with a marker is changed,
     * so that internal caches related to calculating value sums could be updated.
     */
    public void valueUpdated(T marker) {
        Node<T> node = (Node<T>) lookupNode(marker);
        if (node != null) node.recalculateSubTreeSumUp();
    }

    /**
     * Calculates sum of values associated with markers having offset less than or equal to given offset.
     */
    public int getSumOfValuesUpToOffset(int offset) {
        return getSumOfValuesForOverlappingRanges(getRoot(), offset, 0);
    }

    private int getSumOfValuesForOverlappingRanges(@Nullable Node<T> node, int offset, int deltaUpToRootExclusive) {
        if (node == null) return 0;
        int delta = deltaUpToRootExclusive + node.getDelta();
        if (offset >= node.getMaxEnd() + delta) return node.subtreeSum;
        int value = getSumOfValuesForOverlappingRanges(node.getLeft(), offset, delta);
        if (offset >= node.intervalStart() + delta) {
            value += node.getLocalSum();
            value += getSumOfValuesForOverlappingRanges(node.getRight(), offset, delta);
        }
        return value;
    }

    @Override
    @Nonnull
    protected RMNode<T> createNewNode(@Nonnull T key,
                                               int start,
                                               int end,
                                               boolean greedyToLeft,
                                               boolean greedyToRight,
                                               boolean stickingToRight,
                                               int layer) {
        assert start == end;
        assert !greedyToLeft;
        assert !greedyToRight;
        return new Node<>(this, key, start, start, false, false, stickingToRight);
    }

    @Override
    public void correctMax(@Nonnull IntervalNode<T> node, int deltaUpToRoot) {
        super.correctMax(node, deltaUpToRoot);
        ((Node<T>) node).recalculateSubTreeSum();
    }

    public static final class Node<T extends RangeMarkerImpl & IntSupplier> extends RMNode<T> {
        private int subtreeSum;

        Node(@Nonnull RangeMarkerTree<T> rangeMarkerTree,
             @Nonnull T key,
             int start,
             int end,
             boolean greedyToLeft,
             boolean greedyToRight,
             boolean stickingToRight) {
            super(rangeMarkerTree, key, start, end, greedyToLeft, greedyToRight, stickingToRight);
        }

        @Override
        public Node<T> getLeft() {
            return (Node<T>) super.getLeft();
        }

        @Override
        public Node<T> getRight() {
            return (Node<T>) super.getRight();
        }

        @Override
        public Node<T> getParent() {
            return (Node<T>) super.getParent();
        }

        private int getLocalSum() {
            int sum = 0;
            for (Supplier<? extends T> g : intervals) {
                sum += g.get().getAsInt();
            }
            return sum;
        }

        private void recalculateSubTreeSum() {
            subtreeSum = getLocalSum();
            Node<T> left = getLeft();
            if (left != null) subtreeSum += left.subtreeSum;
            Node<T> right = getRight();
            if (right != null) subtreeSum += right.subtreeSum;
        }

        private void recalculateSubTreeSumUp() {
            Node<?> n = this;
            while (n != null) {
                n.recalculateSubTreeSum();
                n = n.getParent();
            }
        }

        @Override
        public void addInterval(@Nonnull T interval) {
            super.addInterval(interval);
            recalculateSubTreeSumUp();
        }

        @Override
        public void removeIntervalInternal(int i) {
            super.removeIntervalInternal(i);
            recalculateSubTreeSumUp();
        }
    }
}
