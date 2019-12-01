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

import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.util.Map;
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
  public static <V> ConcurrentIntObjectMap<V> newConcurrentIntObjectSoftValueHashMap() {
    return new ConcurrentIntKeySoftValueHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftValueMap() {
    return new ConcurrentSoftValueHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap(@Nonnull TObjectHashingStrategy<K> hashingStrategy) {
    return new ConcurrentWeakHashMap<>(hashingStrategy);
  }

  /**
   * Hard keys weak values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newWeakValueHashMap() {
    return new WeakValueHashMap<>(ObjectHashingStrategies.<K>canonicalStrategy());
  }

  /**
   * Weak keys hard values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newWeakHashMap() {
    return newWeakHashMap(4);
  }

  @Contract(value = "_ -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newWeakHashMap(int initialCapacity) {
    return newWeakHashMap(initialCapacity, 0.8f, ContainerUtil.canonicalStrategy());
  }

  @Contract(value = "_, _, _ -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newWeakHashMap(int initialCapacity, float loadFactor, @Nonnull TObjectHashingStrategy<? super K> strategy) {
    return new WeakHashMap<K, V>(initialCapacity, loadFactor, strategy);
  }

  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newWeakKeyWeakValueHashMap() {
    return new WeakKeyWeakValueHashMap<>(true);
  }

  /**
   * Hard keys soft values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newSoftValueHashMap() {
    return new SoftValueHashMap<>(ContainerUtil.canonicalStrategy());
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentLongObjectMap<V> newConcurrentLongObjectHashMap() {
    return new ConcurrentLongObjectHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentLongObjectMap<V> newConcurrentLongObjectHashMap(int initialCapacity) {
    return new ConcurrentLongObjectHashMap<>(initialCapacity);
  }

  @Nonnull
  public static <K, V> Map<K, V> newWeakKeySoftValueHashMap() {
    return new WeakKeySoftValueHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftHashMap() {
    return new ConcurrentSoftHashMap<>();
  }

  @Nonnull
  @Contract(value = " -> new", pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap() {
    return new ConcurrentWeakHashMap<>(0.75f);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeyWeakValueHashMap() {
    return newConcurrentWeakKeyWeakValueHashMap(ContainerUtil.<K>canonicalStrategy());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeyWeakValueHashMap(@Nonnull TObjectHashingStrategy<K> strategy) {
    return new ConcurrentWeakKeyWeakValueHashMap<>(100, 0.75f, Runtime.getRuntime().availableProcessors(), strategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull TObjectHashingStrategy<K> hashingStrategy) {
    return new ConcurrentSoftHashMap<>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull TObjectHashingStrategy<K> hashingStrategy) {
    return new ConcurrentWeakHashMap<>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeySoftValueHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final TObjectHashingStrategy<K> hashingStrategy) {
    return new ConcurrentWeakKeySoftValueHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }
}
