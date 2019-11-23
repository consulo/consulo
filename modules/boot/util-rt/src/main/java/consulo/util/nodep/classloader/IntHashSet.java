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
final class IntHashSet {
  private final Set<Integer> impl;

  IntHashSet() {
    this(10);
  }

  IntHashSet(int capacity) {
    impl = new HashSet<Integer>(capacity);
  }

  void add(int value) {
    impl.add(value);
  }

  boolean contains(int value) {
    return impl.contains(value);
  }

  int[] toArray() {
    int[] array = new int[impl.size()];
    int i = 0;
    for (Integer val : impl) {
      array[i++] = val;
    }
    return array;
  }
}
