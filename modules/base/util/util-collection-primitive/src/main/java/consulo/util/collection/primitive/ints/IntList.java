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

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public interface IntList extends IntCollection {
  void add(int index, int value);

  default boolean addAll(int index, IntList c) {
    return addAll(index, c.toArray());
  }

  boolean addAll(int index, int[] values);

  int get(int index);

  int indexOf(int value);

  /**
   * @param index
   * @param newValue
   * @return oldValue
   */
  int set(int index, int newValue);

  int removeByIndex(int index);

  void sort();

  PrimitiveListIterator.OfInt listIterator();

  PrimitiveListIterator.OfInt listIterator(int index);

  default void removeRange(int fromIndex, int toIndex) {
    PrimitiveListIterator.OfInt it = listIterator(fromIndex);
    for (int i = 0, n = toIndex - fromIndex; i < n; i++) {
      it.next();
      it.remove();
    }
  }
}
