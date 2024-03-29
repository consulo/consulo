// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.document.impl;

import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.internal.EditorDocumentPriorities;
import consulo.document.internal.PrioritizedInternalDocumentListener;
import consulo.document.internal.RangeMarkerEx;
import consulo.util.collection.SmartList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class RangeMarkerTree<T extends RangeMarkerEx> extends IntervalTreeImpl<T> implements PrioritizedInternalDocumentListener {
  public RangeMarkerTree(@Nonnull Document document) {
    document.addDocumentListener(this);
  }

  public RangeMarkerTree() {
  }

  @Override
  public void moveTextHappened(@Nonnull Document document, int start, int end, int newBase) {
    reTarget(start, end, newBase);
  }

  @Override
  public int getPriority() {
    return EditorDocumentPriorities.RANGE_MARKER; // Need to make sure we invalidate all the stuff before someone (like LineStatusTracker) starts to modify highlights.
  }

  @Override
  public void documentChanged(@Nonnull DocumentEvent event) {
    updateMarkersOnChange(event);
  }

  @Override
  protected int compareEqualStartIntervals(@Nonnull IntervalTreeImpl.IntervalNode<T> i1, @Nonnull IntervalTreeImpl.IntervalNode<T> i2) {
    RMNode<?> o1 = (RMNode<?>)i1;
    RMNode<?> o2 = (RMNode<?>)i2;
    boolean greedyL1 = o1.isGreedyToLeft();
    boolean greedyL2 = o2.isGreedyToLeft();
    if (greedyL1 != greedyL2) return greedyL1 ? -1 : 1;

    int o1Length = o1.intervalEnd() - o1.intervalStart();
    int o2Length = o2.intervalEnd() - o2.intervalStart();
    int d = o1Length - o2Length;
    if (d != 0) return d;

    boolean greedyR1 = o1.isGreedyToRight();
    boolean greedyR2 = o2.isGreedyToRight();
    if (greedyR1 != greedyR2) return greedyR1 ? -1 : 1;

    boolean stickyR1 = o1.isStickingToRight();
    boolean stickyR2 = o2.isStickingToRight();
    if (stickyR1 != stickyR2) return stickyR1 ? -1 : 1;

    return 0;
  }

  public void dispose(@Nonnull Document document) {
    document.removeDocumentListener(this);
  }

  private static final int DUPLICATE_LIMIT = 30; // assertion: no more than DUPLICATE_LIMIT range markers are allowed to be registered at given (start, end)

  @Nonnull
  @Override
  public RMNode<T> addInterval(@Nonnull T interval, int start, int end, boolean greedyToLeft, boolean greedyToRight, boolean stickingToRight, int layer) {
    ((RangeMarkerImpl)interval).setValid(true);
    RMNode<T> node = (RMNode<T>)super.addInterval(interval, start, end, greedyToLeft, greedyToRight, stickingToRight, layer);

    if (IntervalTreeImpl.DEBUG && node.intervals.size() > DUPLICATE_LIMIT && ApplicationManager.getApplication().isUnitTestMode()) {
      l.readLock().lock();
      try {
        String msg = errMsg(node);
        if (msg != null) {
          IntervalTreeImpl.LOG.warn(msg);
        }
      }
      finally {
        l.readLock().unlock();
      }
    }
    return node;
  }

  private String errMsg(@Nonnull RMNode<T> node) {
    System.gc();
    final AtomicInteger alive = new AtomicInteger();
    node.processAliveKeys(t -> {
      alive.incrementAndGet();
      return true;
    });
    if (alive.get() > DUPLICATE_LIMIT) {
      return "Too many range markers (" + alive + ") registered for interval " + node;
    }

    return null;
  }

  @Nonnull
  @Override
  protected RMNode<T> createNewNode(@Nonnull T key, int start, int end, boolean greedyToLeft, boolean greedyToRight, boolean stickingToRight, int layer) {
    return new RMNode<>(this, key, start, end, greedyToLeft, greedyToRight, stickingToRight);
  }

  @Override
  protected RMNode<T> lookupNode(@Nonnull T key) {
    //noinspection unchecked
    return (RMNode<T>)((RangeMarkerImpl)key).myNode;
  }

  @Override
  protected void setNode(@Nonnull T key, IntervalNode<T> intervalNode) {
    ((RangeMarkerImpl)key).myNode = (RMNode<RangeMarkerEx>)intervalNode;
  }

  public static class RMNode<T extends RangeMarkerEx> extends IntervalTreeImpl.IntervalNode<T> {
    private static final byte EXPAND_TO_LEFT_FLAG = IntervalNode.VALID_FLAG << 1;
    private static final byte EXPAND_TO_RIGHT_FLAG = EXPAND_TO_LEFT_FLAG << 1;
    public static final byte STICK_TO_RIGHT_FLAG = EXPAND_TO_RIGHT_FLAG << 1;

    public RMNode(@Nonnull RangeMarkerTree<T> rangeMarkerTree, @Nonnull T key, int start, int end, boolean greedyToLeft, boolean greedyToRight, boolean stickingToRight) {
      super(rangeMarkerTree, key, start, end);
      setFlag(EXPAND_TO_LEFT_FLAG, greedyToLeft);
      setFlag(EXPAND_TO_RIGHT_FLAG, greedyToRight);
      setFlag(STICK_TO_RIGHT_FLAG, stickingToRight);
    }

    public boolean isGreedyToLeft() {
      return isFlagSet(EXPAND_TO_LEFT_FLAG);
    }

    public boolean isGreedyToRight() {
      return isFlagSet(EXPAND_TO_RIGHT_FLAG);
    }

    public boolean isStickingToRight() {
      return isFlagSet(STICK_TO_RIGHT_FLAG);
    }

    public void onRemoved() {
    }

    @Override
    public String toString() {
      return (isGreedyToLeft() ? "[" : "(") + intervalStart() + "," + intervalEnd() + (isGreedyToRight() ? "]" : ")");
    }
  }

  private void updateMarkersOnChange(@Nonnull DocumentEvent e) {
    try {
      l.writeLock().lock();
      if (size() == 0) return;
      checkMax(true);

      incModCount();

      List<IntervalNode<T>> affected = new SmartList<>();
      collectAffectedMarkersAndShiftSubtrees(getRoot(), e, affected);
      checkMax(false);

      if (!affected.isEmpty()) {
        updateAffectedNodes(e, affected);
      }
      checkMax(true);

      IntervalNode<T> root = getRoot();
      assert root == null || root.maxEnd + root.delta <= e.getDocument().getTextLength();
    }
    finally {
      l.writeLock().unlock();
    }
  }

  private void updateAffectedNodes(@Nonnull DocumentEvent e, List<IntervalNode<T>> affected) {
    // reverse direction to visit leaves first - it's cheaper to compute maxEndOf for them first
    for (int i = affected.size() - 1; i >= 0; i--) {
      IntervalNode<T> node = affected.get(i);
      // assumption: interval.getEndOffset() will never be accessed during remove()
      int startOffset = node.intervalStart();
      int endOffset = node.intervalEnd();
      removeNode(node);
      checkMax(false);
      node.clearDelta();   // we can do it because all the deltas up from the root to this node were cleared in the collectAffectedMarkersAndShiftSubtrees
      node.setParent(null);
      node.setLeft(null);
      node.setRight(null);
      node.setValid(true);
      assert node.intervalStart() == startOffset;
      assert node.intervalEnd() == endOffset;
    }
    checkMax(true);
    for (IntervalNode<T> node : affected) {
      List<Supplier<T>> keys = node.intervals;
      if (keys.isEmpty()) continue; // collected away

      RangeMarkerImpl marker = null;
      for (int i = keys.size() - 1; i >= 0; i--) {
        Supplier<T> key = keys.get(i);
        marker = (RangeMarkerImpl)key.get();
        if (marker != null) {
          if (!marker.isValid()) {
            // marker can become invalid on its own, e.g. FoldRegion
            node.removeIntervalInternal(i);
            marker = null;
            continue;
          }
          break;
        }
      }
      if (marker == null) continue; // node remains removed from the tree
      marker.documentChanged(e);
      if (marker.isValid()) {
        findOrInsertWithIntervals(node);
      }
      else {
        node.setValid(false);
        ((RMNode<?>)node).onRemoved();
      }
    }
  }

  private void findOrInsertWithIntervals(IntervalNode<T> node) {
    IntervalNode<T> insertedNode = findOrInsert(node);
    // can change if two range become the one
    if (insertedNode != node) {
      // merge happened
      insertedNode.addIntervalsFrom(node);
    }
  }

  // returns true if all deltas involved are still 0
  public boolean collectAffectedMarkersAndShiftSubtrees(@Nullable IntervalNode<T> root, @Nonnull DocumentEvent e, @Nonnull List<? super IntervalNode<T>> affected) {
    if (root == null) return true;
    boolean norm = pushDelta(root);

    int maxEnd = root.maxEnd;
    assert root.isValid();

    int offset = e.getOffset();
    int affectedEndOffset = offset + e.getOldLength();
    boolean hasAliveKeys = root.hasAliveKey(false);
    if (!hasAliveKeys) {
      // marker was garbage collected
      affected.add(root);
    }
    if (offset > maxEnd) {
      // no need to bother
    }
    else if (affectedEndOffset < root.intervalStart()) {
      // shift entire subtree
      int lengthDelta = e.getNewLength() - e.getOldLength();
      int newD = root.changeDelta(lengthDelta);
      norm &= newD == 0;
      IntervalNode<T> left = root.getLeft();
      if (left != null) {
        int newL = left.changeDelta(-lengthDelta);
        norm &= newL == 0;
      }
      norm &= pushDelta(root);
      norm &= collectAffectedMarkersAndShiftSubtrees(left, e, affected);
      correctMax(root, 0);
    }
    else {
      if (offset <= root.intervalEnd()) {
        // unlucky enough so that change affects the interval
        if (hasAliveKeys) affected.add(root); // otherwise we've already added it
        root.setValid(false);  //make invisible
      }

      norm &= collectAffectedMarkersAndShiftSubtrees(root.getLeft(), e, affected);
      norm &= collectAffectedMarkersAndShiftSubtrees(root.getRight(), e, affected);
      correctMax(root, 0);
    }
    return norm;
  }

  // all intervals contained in (start, end) will be shifted by (newBase-start)
  // that's what happens when you "move" text in document, e.g. ctrl-shift-up/down the selection.
  private void reTarget(int start, int end, int newBase) {
    l.writeLock().lock();
    try {
      checkMax(true);

      List<IntervalNode<T>> affected = new ArrayList<>();
      collectNodesToRetarget(getRoot(), start, end, affected);
      if (affected.isEmpty()) return;
      // remove all first because findOrInsert can remove gced nodes which could interfere with not-yet-removed nodes
      for (IntervalNode<T> node : affected) {
        removeNode(node);
      }
      int shift = newBase - start;
      for (IntervalNode<T> node : affected) {
        node.setLeft(null);
        node.setRight(null);
        node.setParent(null);
        node.changeDelta(shift);
        node.setValid(true);
        pushDelta(node);

        List<Supplier<T>> keys = node.intervals;
        if (keys.isEmpty()) continue; // collected away

        RangeMarkerImpl marker = null;
        for (int i = keys.size() - 1; i >= 0; i--) {
          Supplier<T> key = keys.get(i);
          marker = (RangeMarkerImpl)key.get();
          if (marker != null) {
            if (marker.isValid()) break;
            node.removeIntervalInternal(i);
            marker = null;
          }
        }
        if (marker == null) continue;

        marker.onReTarget(start, end, newBase);

        if (marker.isValid()) {
          findOrInsertWithIntervals(node);
        }
        else {
          node.setValid(false);
          ((RMNode<?>)node).onRemoved();
        }
      }
    }
    finally {
      checkMax(true);
      l.writeLock().unlock();
    }
  }

  private void collectNodesToRetarget(@Nullable IntervalNode<T> root, int start, int end, @Nonnull List<? super IntervalNode<T>> affected) {
    if (root == null) return;
    pushDelta(root);

    int maxEnd = root.maxEnd;
    assert root.isValid();

    if (start > maxEnd) {
      // no need to bother
      return;
    }
    collectNodesToRetarget(root.getLeft(), start, end, affected);
    if (start <= root.intervalStart() && root.intervalEnd() <= end) {
      affected.add(root);
    }
    if (end < root.intervalStart()) {
      return;
    }
    collectNodesToRetarget(root.getRight(), start, end, affected);
  }
}
