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

import consulo.util.collection.primitive.ints.impl.map.ConcurrentIntKeySoftValueHashMap;
import consulo.util.collection.primitive.ints.impl.map.ConcurrentIntKeyWeakValueHashMap;
import consulo.util.collection.primitive.ints.impl.map.ConcurrentIntObjectHashMap;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public final class IntMaps {
  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectWeakValueHashMap() {
    return new ConcurrentIntKeyWeakValueHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectHashMap() {
    return new ConcurrentIntObjectHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
    return new ConcurrentIntObjectHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectSoftValueHashMap() {
    return new ConcurrentIntKeySoftValueHashMap<>();
  }
}
