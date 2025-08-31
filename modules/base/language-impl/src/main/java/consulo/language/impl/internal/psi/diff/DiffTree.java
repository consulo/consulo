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
package consulo.language.impl.internal.psi.diff;

import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.ThreeState;
import consulo.util.lang.ref.SimpleReference;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class DiffTree<OT, NT> {
  private static final int CHANGE_PARENT_VERSUS_CHILDREN_THRESHOLD = 20;

  private final FlyweightCapableTreeStructure<OT> myOldTree;
  private final FlyweightCapableTreeStructure<NT> myNewTree;
  private final ShallowNodeComparator<OT, NT> myComparator;
  private final List<SimpleReference<OT[]>> myOldChildrenLists = new ArrayList<SimpleReference<OT[]>>();
  private final List<SimpleReference<NT[]>> myNewChildrenLists = new ArrayList<SimpleReference<NT[]>>();
  private final CharSequence myOldText;
  private final CharSequence myNewText;
  private final int myOldTreeStart;
  private final int myNewTreeStart;

  private DiffTree(@Nonnull FlyweightCapableTreeStructure<OT> oldTree,
                   @Nonnull FlyweightCapableTreeStructure<NT> newTree,
                   @Nonnull ShallowNodeComparator<OT, NT> comparator,
                   @Nonnull CharSequence oldText) {
    myOldTree = oldTree;
    myNewTree = newTree;
    myComparator = comparator;
    myOldText = oldText;
    myOldTreeStart = oldTree.getStartOffset(oldTree.getRoot());
    myNewText = newTree.toString(newTree.getRoot());
    myNewTreeStart = newTree.getStartOffset(newTree.getRoot());
  }

  public static <OT, NT> void diff(@Nonnull FlyweightCapableTreeStructure<OT> oldTree,
                                   @Nonnull FlyweightCapableTreeStructure<NT> newTree,
                                   @Nonnull ShallowNodeComparator<OT, NT> comparator,
                                   @Nonnull DiffTreeChangeBuilder<OT, NT> consumer,
                                   @Nonnull CharSequence oldText) {
    DiffTree<OT, NT> tree = new DiffTree<OT, NT>(oldTree, newTree, comparator, oldText);
    tree.build(oldTree.getRoot(), newTree.getRoot(), 0, consumer);
  }

  private enum CompareResult {
    EQUAL, // 100% equal
    DRILL_DOWN_NEEDED, // element types are equal, but elements are composite
    TYPE_ONLY, // only element types are equal
    NOT_EQUAL, // 100% different
  }

  @Nonnull
  private static <OT, NT> DiffTreeChangeBuilder<OT, NT> emptyConsumer() {
    //noinspection unchecked
    return EMPTY_CONSUMER;
  }
  private static final DiffTreeChangeBuilder EMPTY_CONSUMER = new DiffTreeChangeBuilder() {
    @Override
    public void nodeReplaced(@Nonnull Object oldChild, @Nonnull Object newChild) {

    }

    @Override
    public void nodeDeleted(@Nonnull Object oldParent, @Nonnull Object oldNode) {

    }

    @Override
    public void nodeInserted(@Nonnull Object oldParent, @Nonnull Object newNode, int pos) {

    }
  };

  @Nonnull
  private CompareResult build(@Nonnull OT oldN, @Nonnull NT newN, int level, @Nonnull DiffTreeChangeBuilder<OT, NT> consumer) {
    OT oldNode = myOldTree.prepareForGetChildren(oldN);
    NT newNode = myNewTree.prepareForGetChildren(newN);

    if (level == myNewChildrenLists.size()) {
      myNewChildrenLists.add(new SimpleReference<NT[]>());
      myOldChildrenLists.add(new SimpleReference<OT[]>());
    }

    SimpleReference<OT[]> oldChildrenR = myOldChildrenLists.get(level);
    int oldChildrenSize = myOldTree.getChildren(oldNode, oldChildrenR);
    OT[] oldChildren = oldChildrenR.get();

    SimpleReference<NT[]> newChildrenR = myNewChildrenLists.get(level);
    int newChildrenSize = myNewTree.getChildren(newNode, newChildrenR);
    NT[] newChildren = newChildrenR.get();

    CompareResult result;
    if (Math.abs(oldChildrenSize - newChildrenSize) > CHANGE_PARENT_VERSUS_CHILDREN_THRESHOLD) {
      consumer.nodeReplaced(oldNode, newNode);
      result = CompareResult.NOT_EQUAL;
    }
    else if (oldChildrenSize == 0 && newChildrenSize == 0) {
      if (!myComparator.hashCodesEqual(oldNode, newNode) || !myComparator.typesEqual(oldNode, newNode)) {
        consumer.nodeReplaced(oldNode, newNode);
        result = CompareResult.NOT_EQUAL;
      }
      else {
        result = CompareResult.EQUAL;
      }
    }
    else {
      ShallowNodeComparator<OT, NT> comparator = myComparator;

      int minSize = Math.min(oldChildrenSize, newChildrenSize);
      int suffixLength = match(oldChildren, oldChildrenSize - 1, newChildren, newChildrenSize - 1, level, -1, minSize);
      // for equal size old and new children we have to compare one element less because it was already checked in (unsuccessful) suffix match
      int maxPrefixLength = minSize - suffixLength - (oldChildrenSize == newChildrenSize && suffixLength < minSize ? 1 : 0);
      int prefixLength = match(oldChildren, 0, newChildren, 0, level, 1, maxPrefixLength);

      if (oldChildrenSize == newChildrenSize && suffixLength + prefixLength == oldChildrenSize) {
        result = CompareResult.EQUAL;
      }
      else if (consumer == emptyConsumer()) {
        result = CompareResult.NOT_EQUAL;
      }
      else {
        int oldIndex = prefixLength;
        int newIndex = prefixLength;
        while (oldIndex < oldChildrenSize - suffixLength || newIndex < newChildrenSize - suffixLength) {
          OT oldChild1 = oldIndex < oldChildrenSize - suffixLength ? oldChildren[oldIndex] : null;
          OT oldChild2 = oldIndex < oldChildrenSize - suffixLength - 1 ? oldChildren[oldIndex + 1] : null;
          OT oldChild3 = oldIndex < oldChildrenSize - suffixLength - 2 ? oldChildren[oldIndex + 2] : null;
          NT newChild1 = newIndex < newChildrenSize - suffixLength ? newChildren[newIndex] : null;
          NT newChild2 = newIndex < newChildrenSize - suffixLength - 1 ? newChildren[newIndex + 1] : null;
          NT newChild3 = newIndex < newChildrenSize - suffixLength - 2 ? newChildren[newIndex + 2] : null;

          CompareResult c11 = looksEqual(comparator, oldChild1, newChild1);
          if (c11 == CompareResult.EQUAL || c11 == CompareResult.DRILL_DOWN_NEEDED) {
            if (c11 == CompareResult.DRILL_DOWN_NEEDED) {
              build(oldChild1, newChild1, level + 1, consumer);
            }
            oldIndex++;
            newIndex++;
            continue;
          }
          if (c11 == CompareResult.TYPE_ONLY) {
            CompareResult c21 = looksEqual(comparator, oldChild2, newChild1);
            if (c21 == CompareResult.EQUAL || c21 == CompareResult.DRILL_DOWN_NEEDED) {
              consumer.nodeDeleted(oldNode, oldChild1);
              oldIndex++;
              continue;
            }
            CompareResult c12 = looksEqual(comparator, oldChild1, newChild2);
            if (c12 == CompareResult.EQUAL || c12 == CompareResult.DRILL_DOWN_NEEDED) {
              consumer.nodeInserted(oldNode, newChild1, newIndex);
              newIndex++;
              continue;
            }
            consumer.nodeReplaced(oldChild1, newChild1);
            oldIndex++;
            newIndex++;
            continue;
          }

          CompareResult c12 = looksEqual(comparator, oldChild1, newChild2);
          if (c12 == CompareResult.EQUAL || c12 == CompareResult.DRILL_DOWN_NEEDED) {
            consumer.nodeInserted(oldNode, newChild1, newIndex);
            newIndex++;
            continue;
          }

          CompareResult c21 = looksEqual(comparator, oldChild2, newChild1);
          if (c21 == CompareResult.EQUAL || c21 == CompareResult.DRILL_DOWN_NEEDED || c21 == CompareResult.TYPE_ONLY) {
            consumer.nodeDeleted(oldNode, oldChild1);
            oldIndex++;
            continue;
          }

          if (c12 == CompareResult.TYPE_ONLY) {
            consumer.nodeInserted(oldNode, newChild1, newIndex);
            newIndex++;
            continue;
          }

          if (oldChild1 == null) {
            consumer.nodeInserted(oldNode, newChild1, newIndex);
            newIndex++;
            continue;
          }
          if (newChild1 == null) {
            consumer.nodeDeleted(oldNode, oldChild1);
            oldIndex++;
            continue;
          }

          // check that maybe two children are inserted/deleted
          // (which frequently is a case when e.g. a PsiMethod inserted, the trailing PsiWhiteSpace is appended too)
          if (oldChild3 != null || newChild3 != null) {
            CompareResult c13 = looksEqual(comparator, oldChild1, newChild3);
            if (c13 == CompareResult.EQUAL || c13 == CompareResult.DRILL_DOWN_NEEDED || c13 == CompareResult.TYPE_ONLY) {
              consumer.nodeInserted(oldNode, newChild1, newIndex);
              newIndex++;
              consumer.nodeInserted(oldNode, newChild2, newIndex);
              newIndex++;
              continue;
            }
            CompareResult c31 = looksEqual(comparator, oldChild3, newChild1);
            if (c31 == CompareResult.EQUAL || c31 == CompareResult.DRILL_DOWN_NEEDED || c31 == CompareResult.TYPE_ONLY) {
              consumer.nodeDeleted(oldNode, oldChild1);
              consumer.nodeDeleted(oldNode, oldChild2);
              oldIndex++;
              oldIndex++;
              continue;
            }
          }

          // last resort: maybe the last elements are more similar?
          OT oldLastChild = oldIndex < oldChildrenSize - suffixLength ? oldChildren[oldChildrenSize - suffixLength - 1] : null;
          NT newLastChild = newIndex < newChildrenSize - suffixLength ? newChildren[newChildrenSize - suffixLength - 1] : null;
          CompareResult c = oldLastChild == null || newLastChild == null ? CompareResult.NOT_EQUAL : looksEqual(comparator, oldLastChild, newLastChild);
          if (c == CompareResult.EQUAL || c == CompareResult.TYPE_ONLY || c == CompareResult.DRILL_DOWN_NEEDED) {
            if (c == CompareResult.DRILL_DOWN_NEEDED) {
              build(oldLastChild, newLastChild, level + 1, consumer);
            }
            else {
              consumer.nodeReplaced(oldLastChild, newLastChild);
            }
            suffixLength++;
            continue;
          }

          consumer.nodeReplaced(oldChild1, newChild1);
          oldIndex++;
          newIndex++;
        }
        result = CompareResult.NOT_EQUAL;
      }
    }
    myOldTree.disposeChildren(oldChildren, oldChildrenSize);
    myNewTree.disposeChildren(newChildren, newChildrenSize);
    return result;
  }

  // tries to match as many nodes as possible from the beginning (if step=1) of from the end (if step =-1)
  // returns number of nodes matched
  private int match(OT[] oldChildren,
                    int oldIndex,
                    NT[] newChildren,
                    int newIndex,
                    int level,
                    int step, // 1 if we go from the start to the end; -1 if we go from the end to the start
                    int maxLength) {
    int delta = 0;
    while (delta != maxLength*step) {
      OT oldChild = oldChildren[oldIndex + delta];
      NT newChild = newChildren[newIndex + delta];

      CompareResult c11 = looksEqual(myComparator, oldChild, newChild);

      if (c11 == CompareResult.DRILL_DOWN_NEEDED) {
        c11 = textMatch(oldChild, newChild) ? build(oldChild, newChild, level + 1, DiffTree.<OT, NT>emptyConsumer()) : CompareResult.NOT_EQUAL;
        assert c11 != CompareResult.DRILL_DOWN_NEEDED;
      }
      if (c11 != CompareResult.EQUAL) {
        break;
      }
      delta += step;
    }
    return delta*step;
  }

  private boolean textMatch(OT oldChild, NT newChild) {
    int oldStart = myOldTree.getStartOffset(oldChild) - myOldTreeStart;
    int oldEnd = myOldTree.getEndOffset(oldChild) - myOldTreeStart;
    int newStart = myNewTree.getStartOffset(newChild) - myNewTreeStart;
    int newEnd = myNewTree.getEndOffset(newChild) - myNewTreeStart;
    // drill down only if node texts match, but when they do, match all the way down unconditionally
    return CharArrayUtil.regionMatches(myOldText, oldStart, oldEnd, myNewText, newStart, newEnd);
  }

  @Nonnull
  private CompareResult looksEqual(@Nonnull ShallowNodeComparator<OT, NT> comparator, OT oldChild1, NT newChild1) {
    if (oldChild1 == null || newChild1 == null) {
      return oldChild1 == newChild1 ? CompareResult.EQUAL : CompareResult.NOT_EQUAL;
    }
    if (!comparator.typesEqual(oldChild1, newChild1)) return CompareResult.NOT_EQUAL;
    ThreeState ret = comparator.deepEqual(oldChild1, newChild1);
    if (ret == ThreeState.YES) return CompareResult.EQUAL;
    if (ret == ThreeState.UNSURE) return CompareResult.DRILL_DOWN_NEEDED;
    return CompareResult.TYPE_ONLY;
  }
}
