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

import consulo.util.collection.impl.CollectionFactory;
import consulo.util.collection.impl.map.*;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2019-12-01
 */
public final class Maps {
  private static CollectionFactory ourFactory = CollectionFactory.get();

  /**
   * @return defaultValue if there is no entry in the map (in that case defaultValue is placed into the map),
   * or corresponding value if entry already exists.
   */
  @Nonnull
  public static <K, V> V cacheOrGet(@Nonnull Map<K, V> map, @Nonnull final K key, @Nonnull final V defaultValue) {
    V v = map.get(key);
    if (v != null) return v;
    V prev = map.putIfAbsent(key, defaultValue);
    return prev == null ? defaultValue : prev;
  }

  /**
   * @return defaultValue if there is no entry in the map (in that case defaultValue is placed into the map),
   * or corresponding value if entry already exists.
   */
  @Nonnull
  public static <K, V> V cacheOrGet(@Nonnull ConcurrentMap<K, V> map, @Nonnull final K key, @Nonnull final V defaultValue) {
    return cacheOrGet((Map<K, V>)map, key, defaultValue);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> newHashMap(@Nonnull Map<? extends K, ? extends V> map, @Nonnull HashingStrategy<K> hashingStrategy) {
    return ourFactory.newHashMapWithStrategy(CollectionFactory.UNKNOWN_CAPACITY, 1f, map, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> newHashMap(int initialCapacity, @Nonnull HashingStrategy<K> hashingStrategy) {
    return ourFactory.newHashMapWithStrategy(initialCapacity, 1f, null, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> newHashMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<K> hashingStrategy) {
    return ourFactory.newHashMapWithStrategy(initialCapacity, loadFactor, null, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> newHashMap(@Nonnull HashingStrategy<K> hashingStrategy) {
    return ourFactory.newHashMapWithStrategy(CollectionFactory.UNKNOWN_CAPACITY, 1f, null, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> newLinkedHashMap(@Nonnull HashingStrategy<K> hashingStrategy) {
    return new LinkedHashMap<>(hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentHashMap() {
    return new java.util.concurrent.ConcurrentHashMap<K, V>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakValueHashMap() {
    return new ConcurrentWeakValueHashMap<K, V>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftValueHashMap() {
    return new ConcurrentSoftValueHashMap<K, V>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap(@Nonnull HashingStrategy<K> hashingStrategy) {
    return new consulo.util.collection.impl.map.ConcurrentWeakHashMap<K, V>(hashingStrategy);
  }

  /**
   * Hard keys weak values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newWeakValueHashMap() {
    return new WeakValueHashMap<K, V>(HashingStrategy.<K>canonical());
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
    return newWeakHashMap(initialCapacity, 0.8f, HashingStrategy.canonical());
  }

  @Contract(value = "_, _, _ -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newWeakHashMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<? super K> strategy) {
    return ourFactory.<K, V>newWeakHashMap(initialCapacity, loadFactor, strategy);
  }

  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newWeakKeyWeakValueHashMap() {
    return new WeakKeyWeakValueHashMap<K, V>(true);
  }

  /**
   * Hard keys soft values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newSoftValueHashMap() {
    return new SoftValueHashMap<K, V>(HashingStrategy.canonical());
  }

  @Nonnull
  public static <K, V> Map<K, V> newWeakKeySoftValueHashMap() {
    return new WeakKeySoftValueHashMap<K, V>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftHashMap() {
    return new ConcurrentSoftHashMap<K, V>();
  }

  @Nonnull
  @Contract(value = " -> new", pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap() {
    return new ConcurrentWeakHashMap<K, V>(0.75f);
  }

  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakIdentityMap() {
    return new consulo.util.collection.impl.map.ConcurrentWeakHashMap<K, V>(HashingStrategy.<K>identity());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeyWeakValueHashMap() {
    return newConcurrentWeakKeyWeakValueHashMap(HashingStrategy.<K>canonical());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeyWeakValueHashMap(@Nonnull HashingStrategy<K> strategy) {
    return new ConcurrentWeakKeyWeakValueHashMap<K, V>(100, 0.75f, Runtime.getRuntime().availableProcessors(), strategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull HashingStrategy<K> hashingStrategy) {
    return new consulo.util.collection.impl.map.ConcurrentSoftHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull HashingStrategy<K> hashingStrategy) {
    return new consulo.util.collection.impl.map.ConcurrentWeakHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeySoftValueHashMap() {
    return newConcurrentWeakKeySoftValueHashMap(100, 0.75f, Runtime.getRuntime().availableProcessors(), ContainerUtil.<K>canonicalStrategy());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeySoftValueHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final HashingStrategy<K> hashingStrategy) {
    return new ConcurrentWeakKeySoftValueHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> ConcurrentMap<T, V> newConcurrentHashMap(int initialCapacity) {
    return new java.util.concurrent.ConcurrentHashMap<>(initialCapacity);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> ConcurrentMap<T, V> newConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
    return new java.util.concurrent.ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> ConcurrentMap<T, V> newConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull HashingStrategy<T> hashStrategy) {
    return new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel, hashStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> ConcurrentMap<T, V> newConcurrentHashMap(@Nonnull HashingStrategy<T> hashStrategy) {
    return new ConcurrentHashMap<>(hashStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftKeySoftValueHashMap() {
    return new ConcurrentSoftKeySoftValueHashMap<K, V>(100, 0.75f, Runtime.getRuntime().availableProcessors(), HashingStrategy.canonical());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentSoftKeySoftValueHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final HashingStrategy<K> hashingStrategy) {
    return new consulo.util.collection.impl.map.ConcurrentSoftKeySoftValueHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  /**
   * Soft keys hard values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newSoftHashMap() {
    return newSoftHashMap(HashingStrategy.canonical());
  }

  @Contract(value = "_ -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newSoftHashMap(@Nonnull HashingStrategy<? super K> strategy) {
    return ourFactory.<K, V>newSoftHashMap(strategy);
  }

  public static void trimToSize(@Nonnull Map<?, ?> map) {
    ourFactory.trimToSize(map);
  }

  @Contract(value = "_ -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> newLinkedHashMap(@Nonnull Predicate<Map<K, V>> removeEldestEntryFunc) {
    return new LinkedHashMap<>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> eldest, K key, V value) {
        return removeEldestEntryFunc.test(this);
      }
    };
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> notNullize(@Nullable Map<K, V> map) {
    return map == null ? Collections.emptyMap() : map;
  }
}
