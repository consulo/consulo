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
package consulo.util.collection;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 2019-12-01
 */
public final class Maps {
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
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakValueMap() {
    return new ConcurrentWeakValueHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftValueMap() {
    return new ConcurrentSoftValueHashMap<>();
  }
}
