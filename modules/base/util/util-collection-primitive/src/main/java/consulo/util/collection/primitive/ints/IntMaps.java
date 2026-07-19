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

import consulo.util.collection.primitive.impl.FastUtilIntIntMap;
import consulo.util.collection.primitive.impl.FastUtilIntObjectMap;
import consulo.util.collection.primitive.ints.impl.map.ConcurrentIntKeySoftValueHashMap;
import consulo.util.collection.primitive.ints.impl.map.ConcurrentIntKeyWeakValueHashMap;
import consulo.util.collection.primitive.ints.impl.map.ConcurrentIntObjectHashMap;

/**
 * @author VISTALL
 * @since 2021-02-07
 */
@SuppressWarnings("deprecation")
public final class IntMaps {
  private static final int UNKNOWN_CAPACITY = -1;

  public static <V> IntObjectMap<V> newIntObjectHashMap() {
    return newIntObjectHashMap(UNKNOWN_CAPACITY);
  }

  public static <V> IntObjectMap<V> newIntObjectHashMap(int capacity) {
    return new FastUtilIntObjectMap<>(capacity);
  }

  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectWeakValueHashMap() {
    return new ConcurrentIntKeyWeakValueHashMap<>();
  }

  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectHashMap() {
    return new ConcurrentIntObjectHashMap<>();
  }

  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
    return new ConcurrentIntObjectHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
  }

  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectSoftValueHashMap() {
    return new ConcurrentIntKeySoftValueHashMap<>();
  }

  public static IntIntMap newIntIntHashMap() {
    return newIntIntHashMap(UNKNOWN_CAPACITY);
  }

  public static IntIntMap newIntIntHashMap(int capacity) {
    return new FastUtilIntIntMap(capacity);
  }

  public static void trimToSize(IntIntMap map) {
    if (map instanceof FastUtilIntIntMap fastUtilMap) {
      fastUtilMap.trimToSize();
    }
  }
}
