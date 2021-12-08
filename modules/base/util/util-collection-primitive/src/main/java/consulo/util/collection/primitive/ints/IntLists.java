/*
 * Copyright 2013-2021 consulo.io
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
package consulo.util.collection.primitive.ints;

import consulo.util.collection.primitive.PrimitiveListIterator;
import consulo.util.collection.primitive.ints.impl.list.IntArrayList;

import javax.annotation.Nonnull;
import java.util.RandomAccess;

/**
 * @author VISTALL
 * @since 10/02/2021
 */
@SuppressWarnings("deprecation")
public class IntLists {
  private static final int REVERSE_THRESHOLD = 18;

  @Nonnull
  public static IntList newArrayList() {
    return new IntArrayList();
  }

  @Nonnull
  public static IntList newArrayList(int[] values) {
    return new IntArrayList(values);
  }

  @Nonnull
  public static IntList newArrayList(int capacity) {
    return new IntArrayList(capacity);
  }

  public static void trimToSize(@Nonnull IntList list) {
    if (list instanceof IntArrayList intArrayList) {
      intArrayList.trimToSize();
    }
  }

  /**
   * Reverses the order of the elements in the specified list.<p>
   * <p>
   * This method runs in linear time.
   *
   * @see java.util.Collections#reverse
   *
   * @param list the list whose elements are to be reversed.
   * @throws UnsupportedOperationException if the specified list or
   *                                       its list-iterator does not support the {@code set} operation.
   */
  public static void reverse(IntList list) {
    int size = list.size();
    if (size < REVERSE_THRESHOLD || list instanceof RandomAccess) {
      for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) {
        swap(list, i, j);
      }
    }
    else {
      // instead of using a raw type here, it's possible to capture
      // the wildcard but it will require a call to a supplementary
      // private method
      PrimitiveListIterator.OfInt fwd = list.listIterator();
      PrimitiveListIterator.OfInt rev = list.listIterator(size);
      for (int i = 0, mid = list.size() >> 1; i < mid; i++) {
        int tmp = fwd.nextInt();
        fwd.setInt(rev.previousInt());
        rev.setInt(tmp);
      }
    }
  }

  /**
   * Swaps the elements at the specified positions in the specified list.
   * (If the specified positions are equal, invoking this method leaves
   * the list unchanged.)
   *
   * @param list The list in which to swap elements.
   * @param i    the index of one element to be swapped.
   * @param j    the index of the other element to be swapped.
   * @throws IndexOutOfBoundsException if either {@code i} or {@code j}
   *                                   is out of range (i &lt; 0 || i &gt;= list.size()
   *                                   || j &lt; 0 || j &gt;= list.size()).
   * @since 1.4
   */
  public static void swap(IntList list, int i, int j) {
    list.set(i, list.set(j, list.get(i)));
  }
}
