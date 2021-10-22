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

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public interface IntCollection extends IntIterable {
  boolean add(int value);

  default boolean addAll(int[] collection) {
    for (int i : collection) {
      add(i);
    }
    return true;
  }

  default boolean addAll(IntCollection collection) {
    collection.forEach(this::add);
    return true;
  }

  boolean remove(int value);

  default void removeAll(int... array) {
    for (int value : array) {
      remove(value);
    }
  }

  boolean contains(int value);

  default boolean retainAll(IntCollection otherCollection) {
    Objects.requireNonNull(otherCollection);
    boolean modified = false;
    PrimitiveIterator.OfInt it = iterator();
    while (it.hasNext()) {
      if (!otherCollection.contains(it.nextInt())) {
        it.remove();
        modified = true;
      }
    }
    return modified;
  }
  
  int[] toArray();

  int size();

  default boolean isEmpty() {
    return size() == 0;
  }

  void clear();

  @Nonnull
  default IntStream stream() {
    return StreamSupport.intStream(splitterator(), false);
  }

  @Nonnull
  default Spliterator.OfInt splitterator() {
    return Spliterators.spliterator(iterator(), size(), 0);
  }
}
