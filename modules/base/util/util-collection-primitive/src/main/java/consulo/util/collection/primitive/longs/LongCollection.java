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
package consulo.util.collection.primitive.longs;

import javax.annotation.Nonnull;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * @author VISTALL
 * @since 17/05/2021
 */
public interface LongCollection extends LongIterable {
  boolean add(long value);

  default boolean addAll(long[] collection) {
    for (long i : collection) {
      add(i);
    }
    return true;
  }

  default boolean addAll(LongCollection collection) {
    collection.forEach(this::add);
    return true;
  }

  boolean remove(long value);

  default void removeAll(long... array) {
    for (long value : array) {
      remove(value);
    }
  }

  boolean contains(long value);

  long[] toArray();

  int size();

  default boolean isEmpty() {
    return size() == 0;
  }

  void clear();

  @Nonnull
  default LongStream stream() {
    return StreamSupport.longStream(splitterator(), false);
  }

  @Nonnull
  default Spliterator.OfLong splitterator() {
    return Spliterators.spliterator(iterator(), size(), 0);
  }
}
