/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.openapi.vcs;

import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.util.containers.ReadonlyList;
import consulo.ide.impl.idea.util.containers.StepList;
import consulo.util.lang.function.PairConsumer;

import java.util.Comparator;
import java.util.function.Consumer;

/**
 * @author irengrig
 * <p>
 * two group header can NOT immediately follow one another
 */
public abstract class GroupingMerger<T, S> {
  private S myCurrentGroup;

  protected boolean filter(T t) {
    return true;
  }

  protected abstract void willBeRecountFrom(int idx, int wasSize);

  protected abstract S getGroup(T t);

  protected abstract T wrapGroup(S s, T item);

  protected abstract void oldBecame(int was, int is);

  protected abstract void afterConsumed(T t, int i);

  protected T wrapItem(T t) {
    return t;
  }

  public S getCurrentGroup() {
    return myCurrentGroup;
  }

  public int firstPlusSecond(final StepList<T> first, ReadonlyList<T> second, Comparator<T> comparator, int idxFrom) {
    int wasSize = first.getSize();
    if (second.getSize() == 0) {
      return wasSize;
    }
    int idx;
    if (idxFrom == -1) {
      idx = stolenBinarySearch(first, second.get(0), comparator, 0);
      if (idx < 0) {
        idx = -(idx + 1);
      }
    }
    else {
      idx = idxFrom;
    }
    // for group headers to not be left alone without its group
    if (idx > 0 && (!filter(first.get(idx - 1)))) {
      --idx;
      //if (idx > 0) --idx;         // todo whether its ok
    }
    ReadonlyList<T> remergePart = first.cut(idx);
    if (idx > 0) {
      myCurrentGroup = getGroup(first.get(idx - 1));
    }
    final int finalIdx = idx;
    willBeRecountFrom(idx, wasSize);
    merge(remergePart, second, comparator, new PairConsumer<T, Integer>() {
      @Override
      public void consume(T t, Integer integer) {
        doForGroup(t, first);
        first.add(t);
        int was = integer + finalIdx;
        //System.out.println("was " + integer + "became " + (first.getSize() - 1));
        oldBecame(was, first.getSize() - 1);
      }
    }, t -> {
      doForGroup(t, first);

      T wrapped = wrapItem(t);
      first.add(wrapped);
      afterConsumed(wrapped, first.getSize() - 1);
    });
    return idx;
  }

  private void doForGroup(T t, StepList<T> first) {
    S newGroup = getGroup(t);
    if (newGroup != null && !Comparing.equal(newGroup, myCurrentGroup)) {
      first.add(wrapGroup(newGroup, t));
      myCurrentGroup = newGroup;
    }
  }

  public void merge(ReadonlyList<T> one, ReadonlyList<T> two, Comparator<T> comparator, PairConsumer<T, Integer> oldAdder, Consumer<T> newAdder) {
    int idx1 = 0;
    int idx2 = 0;
    while (idx1 < one.getSize() && idx2 < two.getSize()) {
      T firstOne = one.get(idx1);
      if (!filter(firstOne)) {
        ++idx1;
        continue;
      }
      int comp = comparator.compare(firstOne, two.get(idx2));
      if (comp <= 0) {
        oldAdder.consume(firstOne, idx1);
        ++idx1;
        if (comp == 0) {
          // take only one
          ++idx2;
        }
      }
      else {
        newAdder.accept(two.get(idx2));
        ++idx2;
      }
    }
    while (idx1 < one.getSize()) {
      T firstOne = one.get(idx1);
      if (!filter(firstOne)) {
        ++idx1;
        continue;
      }
      oldAdder.consume(one.get(idx1), idx1);
      ++idx1;
    }
    while (idx2 < two.getSize()) {
      newAdder.accept(two.get(idx2));
      ++idx2;
    }
  }

  private static <T> int stolenBinarySearch(ReadonlyList<? extends T> l, T key, Comparator<? super T> c, int from) {
    int low = from;
    int high = (l.getSize() - from) - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      T midVal = l.get(mid);
      int cmp = c.compare(midVal, key);

      if (cmp < 0) {
        low = mid + 1;
      }
      else if (cmp > 0) {
        high = mid - 1;
      }
      else {
        return mid; // key found
      }
    }
    return -(low + 1);  // key not found
  }
}
