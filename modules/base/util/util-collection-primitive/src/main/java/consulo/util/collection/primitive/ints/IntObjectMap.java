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
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public interface IntObjectMap<V> {
  interface IntObjectEntry<V1> {
    int getKey();

    V1 getValue();
  }

  @Nonnull
  IntSet keySet();

  @Nonnull
  int[] keys();

  /**
   * @return old value by key
   */
  @Nullable
  V put(int key, V value);

  @Nullable
  V get(int key);

  boolean containsKey(int key);

  boolean containsValue(V value);

  V remove(int key);

  @Nonnull
  Set<IntObjectEntry<V>> entrySet();

  @Nonnull
  Collection<V> values();

  int size();

  boolean isEmpty();

  void clear();

  default void forEach(IntObjConsumer<? super V> action) {
    Objects.requireNonNull(action);
    for (IntObjectEntry<V> entry : entrySet()) {
      int k;
      V v;
      try {
        k = entry.getKey();
        v = entry.getValue();
      }
      catch (IllegalStateException ise) {
        // this usually means the entry is no longer in the map.
        throw new ConcurrentModificationException(ise);
      }
      action.accept(k, v);
    }
  }

  default V putIfAbsent(int key, V value) {
    V v = get(key);
    if (v == null) {
      v = put(key, value);
    }

    return v;
  }
}
