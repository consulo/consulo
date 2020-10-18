/*
 * Copyright 2013-2019 consulo.io
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
package consulo.util.nodep.classloader;

import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-07-15
 * <p>
 * FIXME [VISTALL] make more clear impl ?
 */
class LongHashSet {
  interface LongConsumer {
    void consume(long value);
  }

  private final Set<Long> impl;

  LongHashSet() {
    this(10);
  }

  LongHashSet(int capacity) {
    impl = new HashSet<Long>(capacity);
  }

  void add(long value) {
    impl.add(value);
  }

  boolean contains(long value) {
    return impl.contains(value);
  }

  void forEach(LongConsumer consumer) {
    for (Long aLong : impl) {
      consumer.consume(aLong);
    }
  }

  int size() {
    return impl.size();
  }

  long[] toArray() {
    long[] array = new long[impl.size()];
    int i = 0;
    for (Long val : impl) {
      array[i++] = val;
    }
    return array;
  }
}
