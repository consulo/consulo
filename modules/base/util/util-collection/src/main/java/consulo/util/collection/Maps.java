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
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.Collections;
import java.util.HashMap;
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
    public static <K, V> V cacheOrGet(Map<K, V> map, K key, V defaultValue) {
        V v = map.get(key);
        if (v != null) {
            return v;
        }
        V prev = map.putIfAbsent(key, defaultValue);
        return prev == null ? defaultValue : prev;
    }

    public static <K, V> void putIfNotNull(K key, @Nullable V value, Map<K, V> result) {
        if (value != null) {
            result.put(key, value);
        }
    }

    /**
     * @return defaultValue if there is no entry in the map (in that case defaultValue is placed into the map),
     * or corresponding value if entry already exists.
     */
    public static <K, V> V cacheOrGet(ConcurrentMap<K, V> map, K key, V defaultValue) {
        return cacheOrGet((Map<K, V>)map, key, defaultValue);
    }

    @Contract(pure = true)
    public static <K, V> Map<K, V> newHashMap(Map<? extends K, ? extends V> map, HashingStrategy<K> hashingStrategy) {
        return ourFactory.newHashMapWithStrategy(CollectionFactory.UNKNOWN_CAPACITY, 1f, map, hashingStrategy);
    }

    @Contract(pure = true)
    public static <K, V> Map<K, V> newHashMap(int initialCapacity, HashingStrategy<K> hashingStrategy) {
        return ourFactory.newHashMapWithStrategy(initialCapacity, 1f, null, hashingStrategy);
    }

    @Contract(pure = true)
    public static <K, V> Map<K, V> newHashMap(int initialCapacity, float loadFactor, HashingStrategy<K> hashingStrategy) {
        return ourFactory.newHashMapWithStrategy(initialCapacity, loadFactor, null, hashingStrategy);
    }

    @Contract(pure = true)
    public static <K, V> Map<K, V> newHashMap(HashingStrategy<K> hashingStrategy) {
        return ourFactory.newHashMapWithStrategy(CollectionFactory.UNKNOWN_CAPACITY, 1f, null, hashingStrategy);
    }

    @Contract(pure = true)
    public static <K, V> Map<K, V> newLinkedHashMap(HashingStrategy<K> hashingStrategy) {
        return new LinkedHashMap<>(hashingStrategy);
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentHashMap() {
        return new java.util.concurrent.ConcurrentHashMap<>();
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakValueHashMap() {
        return new ConcurrentWeakValueHashMap<>();
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentSoftValueHashMap() {
        return new ConcurrentSoftValueHashMap<>();
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap(HashingStrategy<K> hashingStrategy) {
        return new ConcurrentWeakHashMap<>(hashingStrategy);
    }

    /**
     * Hard keys weak values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    public static <K, V> Map<K, V> newWeakValueHashMap() {
        return new WeakValueHashMap<>(HashingStrategy.<K>canonical());
    }

    /**
     * Weak keys hard values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    public static <K, V> Map<K, V> newWeakHashMap() {
        return newWeakHashMap(4);
    }

    @Contract(value = "_ -> new", pure = true)
    public static <K, V> Map<K, V> newWeakHashMap(int initialCapacity) {
        return newWeakHashMap(initialCapacity, 0.8f, HashingStrategy.canonical());
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static <K, V> Map<K, V> newWeakHashMap(int initialCapacity, float loadFactor, HashingStrategy<? super K> strategy) {
        return ourFactory.<K, V>newWeakHashMap(initialCapacity, loadFactor, strategy);
    }

    @Contract(value = " -> new", pure = true)
    public static <K, V> Map<K, V> newWeakKeyWeakValueHashMap() {
        return new WeakKeyWeakValueHashMap<>(true);
    }

    /**
     * Hard keys soft values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    public static <K, V> Map<K, V> newSoftValueHashMap() {
        return new SoftValueHashMap<>(HashingStrategy.canonical());
    }

    public static <K, V> Map<K, V> newWeakKeySoftValueHashMap() {
        return new WeakKeySoftValueHashMap<>();
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentSoftHashMap() {
        return new ConcurrentSoftHashMap<>();
    }

    @Contract(value = " -> new", pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap() {
        return new ConcurrentWeakHashMap<>(0.75f);
    }

    @Contract(value = " -> new", pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakIdentityMap() {
        return new ConcurrentWeakHashMap<>(HashingStrategy.<K>identity());
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeyWeakValueHashMap() {
        return newConcurrentWeakKeyWeakValueHashMap(HashingStrategy.<K>canonical());
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeyWeakValueHashMap(HashingStrategy<K> strategy) {
        return new ConcurrentWeakKeyWeakValueHashMap<>(100, 0.75f, Runtime.getRuntime().availableProcessors(), strategy);
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentSoftHashMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<K> hashingStrategy
    ) {
        return new ConcurrentSoftHashMap<>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakHashMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<K> hashingStrategy
    ) {
        return new ConcurrentWeakHashMap<>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeySoftValueHashMap() {
        return newConcurrentWeakKeySoftValueHashMap(
            100,
            0.75f,
            Runtime.getRuntime().availableProcessors(),
            HashingStrategy.canonical()
        );
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentWeakKeySoftValueHashMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<K> hashingStrategy
    ) {
        return new ConcurrentWeakKeySoftValueHashMap<>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
    }

    @Contract(pure = true)
    public static <T, V> ConcurrentMap<T, V> newConcurrentHashMap(int initialCapacity) {
        return new java.util.concurrent.ConcurrentHashMap<>(initialCapacity);
    }

    @Contract(pure = true)
    public static <T, V> ConcurrentMap<T, V> newConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        return new java.util.concurrent.ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    }

    @Contract(pure = true)
    public static <T, V> ConcurrentMap<T, V> newConcurrentHashMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<T> hashStrategy
    ) {
        return new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel, hashStrategy);
    }

    @Contract(pure = true)
    public static <T, V> ConcurrentMap<T, V> newConcurrentHashMap(HashingStrategy<T> hashStrategy) {
        return new ConcurrentHashMap<>(hashStrategy);
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentSoftKeySoftValueHashMap() {
        return new ConcurrentSoftKeySoftValueHashMap<>(
            100,
            0.75f,
            Runtime.getRuntime().availableProcessors(),
            HashingStrategy.canonical()
        );
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentSoftKeySoftValueHashMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<K> hashingStrategy
    ) {
        return new ConcurrentSoftKeySoftValueHashMap<>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
    }

    /**
     * Soft keys hard values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    public static <K, V> Map<K, V> newSoftHashMap() {
        return newSoftHashMap(HashingStrategy.canonical());
    }

    @Contract(value = "_ -> new", pure = true)
    public static <K, V> Map<K, V> newSoftHashMap(HashingStrategy<? super K> strategy) {
        return ourFactory.<K, V>newSoftHashMap(strategy);
    }

    public static void trimToSize(Map<?, ?> map) {
        ourFactory.trimToSize(map);
    }

    @Contract(value = "_ -> new", pure = true)
    public static <K, V> Map<K, V> newLinkedHashMap(Predicate<Map<K, V>> removeEldestEntryFunc) {
        return new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest, K key, V value) {
                return removeEldestEntryFunc.test(this);
            }
        };
    }

    @Contract(pure = true)
    public static <K, V> Map<K, V> notNullize(@Nullable Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    @Contract(pure = true)
    public static <A, B> Map<B, A> reverseMap(Map<A, B> map) {
        Map<B, A> result = new HashMap<>();
        for (Map.Entry<A, B> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }
}
