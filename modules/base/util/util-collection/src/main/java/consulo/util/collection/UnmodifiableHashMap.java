// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.collection;

import org.jetbrains.annotations.Contract;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An immutable unordered hash-based map which optimizes incremental growth and may have custom equals/hashCode strategy.
 * This map doesn't support null keys, but supports null values. This map is not serializable.
 *
 * @param <K> type of the map keys
 * @param <V> type of the map values
 * @implNote internally this map is represented as an open-addressed hash-table which contains interleaved keys and values
 * and has exactly half empty entries, and up to three separate key/value pairs stored in the fields. This allows reusing the
 * same table when a new element is added. Thanks to this rehashing occurs only once in four additions.
 */
public final class UnmodifiableHashMap<K, V> extends AbstractImmutableMap<K, V> {
    private static final UnmodifiableHashMap<Object, Object> EMPTY =
        new UnmodifiableHashMap<>(HashingStrategy.canonical(), ArrayUtil.EMPTY_OBJECT_ARRAY, null, null, null, null, null, null);

    @Nonnull
    private final HashingStrategy<K> strategy;
    @Nonnull
    private final Object[] data;
    private final K k1, k2, k3;
    private final V v1, v2, v3;
    private final int size;
    private Set<K> keySet;
    private Collection<V> values;

    /**
     * Returns an empty {@code UnmodifiableHashMap} with canonical equals/hashCode strategy.
     *
     * @param <K> type of map keys
     * @param <V> type of map values
     * @return an empty {@code UnmodifiableHashMap}.
     * @see HashingStrategy#canonical()
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <K, V> UnmodifiableHashMap<K, V> empty() {
        return (UnmodifiableHashMap<K, V>)EMPTY;
    }

    /**
     * Returns an empty {@code UnmodifiableHashMap} with supplied strategy.
     *
     * @param strategy strategy to compare keys
     * @param <K>      type of map keys
     * @param <V>      type of map values
     * @return an empty {@code UnmodifiableHashMap}.
     */
    @Nonnull
    public static <K, V> UnmodifiableHashMap<K, V> empty(HashingStrategy<K> strategy) {
        return strategy == HashingStrategy.canonical()
            ? empty()
            : new UnmodifiableHashMap<>(strategy, ArrayUtil.EMPTY_OBJECT_ARRAY, null, null, null, null, null, null);
    }

    /**
     * Returns an {@code UnmodifiableHashMap} which contains all the entries of the supplied map.
     *
     * @param map map to copy values from
     * @param <K> type of map keys
     * @param <V> type of map values
     * @return a pre-populated {@code UnmodifiableHashMap}. Map return the supplied map if
     * it's already an {@code UnmodifiableHashMap} which uses the same equals/hashCode strategy.
     */
    @Nonnull
    public static <K, V> UnmodifiableHashMap<K, V> fromMap(@Nonnull Map<? extends K, ? extends V> map) {
        return fromMap(HashingStrategy.canonical(), map);
    }

    /**
     * Returns an {@code UnmodifiableHashMap} which contains all the entries of the supplied map. It's assumed that the supplied
     * map remains unchanged during the {@code fromMap} call.
     *
     * @param strategy strategy to compare keys
     * @param map      map to copy values from
     * @param <K>      type of map keys
     * @param <V>      type of map values
     * @return a pre-populated {@code UnmodifiableHashMap}. Map return the supplied map if
     * it's already an {@code UnmodifiableHashMap} which uses the same equals/hashCode strategy.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <K, V> UnmodifiableHashMap<K, V> fromMap(
        @Nonnull HashingStrategy<K> strategy,
        @Nonnull Map<? extends K, ? extends V> map
    ) {
        if (map instanceof UnmodifiableHashMap uhm && uhm.strategy == strategy) {
            return (UnmodifiableHashMap<K, V>)uhm;
        }
        else if (map.isEmpty()) {
            return empty(strategy);
        }
        else if (map.size() <= 3) {
            K k1 = null;
            K k2 = null;
            K k3 = null;
            V v1 = null;
            V v2 = null;
            V v3 = null;
            Iterator<? extends Entry<? extends K, ? extends V>> iterator = map.entrySet().iterator();
            if (iterator.hasNext()) {
                Entry<? extends K, ? extends V> e = iterator.next();
                k1 = ensureKeyIsNotNull(e.getKey());
                v1 = e.getValue();
                if (iterator.hasNext()) {
                    e = iterator.next();
                    k2 = ensureKeyIsNotNull(e.getKey());
                    v2 = e.getValue();
                    if (iterator.hasNext()) {
                        e = iterator.next();
                        k3 = ensureKeyIsNotNull(e.getKey());
                        v3 = e.getValue();
                        assert !iterator.hasNext();
                    }
                }
            }
            return new UnmodifiableHashMap<>(strategy, ArrayUtil.EMPTY_OBJECT_ARRAY, k1, v1, k2, v2, k3, v3);
        }
        Object[] newData = new Object[map.size() << 2];
        map.forEach((k, v) -> insert(strategy, newData, Objects.requireNonNull(k), v));
        return new UnmodifiableHashMap<>(strategy, newData, null, null, null, null, null, null);
    }

    private UnmodifiableHashMap(
        @Nonnull HashingStrategy<K> strategy,
        @Nonnull Object[] data,
        @Nullable K k1,
        @Nullable V v1,
        @Nullable K k2,
        @Nullable V v2,
        @Nullable K k3,
        @Nullable V v3
    ) {
        this(strategy, data, k1, v1, k2, v2, k3, v3, (data.length >>> 2) + (k1 == null ? 0 : k2 == null ? 1 : k3 == null ? 2 : 3));
    }

    private UnmodifiableHashMap(
        @Nonnull HashingStrategy<K> strategy,
        @Nonnull Object[] data,
        @Nullable K k1,
        @Nullable V v1,
        @Nullable K k2,
        @Nullable V v2,
        @Nullable K k3,
        @Nullable V v3,
        int size
    ) {
        this.strategy = strategy;
        this.data = data;
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.size = size;
    }

    /**
     * Returns an {@code UnmodifiableHashMap} which contains all the entries as this map except the supplied key.
     *
     * @param key a key to exclude from the result
     * @return an {@code UnmodifiableHashMap} which contains all the entries as this map except the supplied key
     */
    @Contract(pure = true)
    @Nonnull
    @Override
    public UnmodifiableHashMap<K, V> without(@Nonnull K key) {
        int pos = data.length == 0 ? -1 : tablePos(strategy, data, key);
        if (pos >= 0) {
            Object[] newData = new Object[(size - 1) << 2];
            for (int i = 0; i < data.length; i += 2) {
                if (i == pos) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                K k = (K)data[i];
                if (k == null) {
                    continue;
                }
                Object v = data[i + 1];
                insert(strategy, newData, k, v);
            }
            if (k1 != null) {
                insert(strategy, newData, k1, v1);
                if (k2 != null) {
                    insert(strategy, newData, k2, v2);
                    if (k3 != null) {
                        insert(strategy, newData, k3, v3);
                    }
                }
            }
            return new UnmodifiableHashMap<>(strategy, newData, null, null, null, null, null, null);
        }
        if (k1 != null) {
            if (strategy.equals(k1, key)) {
                return new UnmodifiableHashMap<>(strategy, data, k2, v2, k3, v3, null, null);
            }
            if (k2 != null) {
                if (strategy.equals(k2, key)) {
                    return new UnmodifiableHashMap<>(strategy, data, k1, v1, k3, v3, null, null);
                }
                if (k3 != null && strategy.equals(k3, key)) {
                    return new UnmodifiableHashMap<>(strategy, data, k1, v1, k2, v2, null, null);
                }
            }
        }
        return this;
    }

    /**
     * <p>Returns an {@code UnmodifiableHashMap} which contains all the entries as this map plus the supplied mapping.</p>
     *
     * <p>May return the same map if given key is already associated with the same value. Note, however, that if value is
     * not the same but equal object, the new map will be created as sometimes it's desired to replace the object with
     * another one which is equal to the old object.</p>
     *
     * @param key   a key to add/replace
     * @param value a value to associate with the key
     * @return an {@code UnmodifiableHashMap} which contains all the entries as this map plus the supplied mapping.
     */
    @Contract(pure = true)
    @Nonnull
    @Override
    public UnmodifiableHashMap<K, V> with(@Nonnull K key, @Nullable V value) {
        int pos = data.length == 0 ? -1 : tablePos(strategy, data, key);
        if (pos >= 0) {
            if (data[pos + 1] == value) {
                return this;
            }
            Object[] copy = data.clone();
            copy[pos + 1] = value;
            return new UnmodifiableHashMap<>(strategy, copy, k1, v1, k2, v2, k3, v3);
        }
        if (k1 == null) {
            return new UnmodifiableHashMap<>(strategy, data, key, value, null, null, null, null);
        }
        if (strategy.equals(k1, key)) {
            return value == v1 ? this : new UnmodifiableHashMap<>(strategy, data, k1, value, k2, v2, k3, v3);
        }
        if (k2 == null) {
            return new UnmodifiableHashMap<>(strategy, data, k1, v1, key, value, null, null);
        }
        if (strategy.equals(k2, key)) {
            return value == v2 ? this : new UnmodifiableHashMap<>(strategy, data, k1, v1, k2, value, k3, v3);
        }
        if (k3 == null) {
            return new UnmodifiableHashMap<>(strategy, data, k1, v1, k2, v2, key, value);
        }
        if (strategy.equals(k3, key)) {
            return value == v3 ? this : new UnmodifiableHashMap<>(strategy, data, k1, v1, k2, v2, k3, value);
        }
        Object[] newData = new Object[(size + 1) << 2];
        for (int i = 0; i < data.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K)data[i];
            if (k == null) {
                continue;
            }
            Object v = data[i + 1];
            insert(strategy, newData, k, v);
        }
        insert(strategy, newData, k1, v1);
        insert(strategy, newData, k2, v2);
        insert(strategy, newData, k3, v3);
        insert(strategy, newData, key, value);
        return new UnmodifiableHashMap<>(strategy, newData, null, null, null, null, null, null);
    }

    /**
     * <p>Returns an {@code UnmodifiableHashMap} which contains all the entries as this map plus all the mappings of the supplied map.</p>
     *
     * <p>May (but not guaranteed) return the same map if the supplied map is empty or all its
     * mappings already exist in this map (assuming values are compared by reference). The equals/hashCode strategy
     * of the resulting map is the same as the strategy of this map.</p>
     *
     * @param map to add entries from
     * @return an {@code UnmodifiableHashMap} which contains all the entries as this map plus all the mappings of the supplied map.
     */
    @Nonnull
    @Override
    public UnmodifiableHashMap<K, V> withAll(@Nonnull Map<? extends K, ? extends V> map) {
        if (isEmpty()) {
            return fromMap(strategy, map);
        }

        int mapSize = map.size();
        if (mapSize == 0) {
            return this;
        }
        else if (mapSize == 1) {
            Entry<? extends K, ? extends V> entry = map.entrySet().iterator().next();
            return with(entry.getKey(), entry.getValue());
        }

        // Map size may be excessive if parameter map keys partially overlap with our map. Ignoring this
        int newSize = size + mapSize;
        Object[] newData = new Object[newSize << 2];
        for (int i = 0; i < data.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K)data[i];
            if (k == null) {
                continue;
            }
            Object v = data[i + 1];
            insert(strategy, newData, k, v);
        }
        if (k1 != null) {
            insert(strategy, newData, k1, v1);
            if (k2 != null) {
                insert(strategy, newData, k2, v2);
                if (k3 != null) {
                    insert(strategy, newData, k3, v3);
                }
            }
        }
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (replace(strategy, newData, ensureKeyIsNotNull(entry.getKey()), entry.getValue())) {
                newSize--;
            }
        }
        return new UnmodifiableHashMap<>(strategy, newData, null, null, null, null, null, null, newSize);
    }

    private static <K> void insert(HashingStrategy<K> strategy, Object[] data, K k, Object v) {
        int insertPos = tablePos(strategy, data, k);
        insertPos = ~insertPos;
        assert insertPos >= 0;
        data[insertPos] = k;
        data[insertPos + 1] = v;
    }

    private static <K> boolean replace(HashingStrategy<K> strategy, Object[] data, K k, Object v) {
        int insertPos = tablePos(strategy, data, k);
        boolean replacing = insertPos >= 0;
        insertPos = replacing ? insertPos : ~insertPos;
        data[insertPos] = k;
        data[insertPos + 1] = v;
        return replacing;
    }

    @Nonnull
    @Override
    public HashingStrategy<K> getStrategy() {
        return strategy;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        @SuppressWarnings("unchecked") K typedKey = (K)key;
        if (data.length > 0 && tablePos(strategy, data, typedKey) >= 0) {
            return true;
        }
        if (k1 != null) {
            if (strategy.equals(k1, typedKey)) {
                return true;
            }
            if (k2 != null) {
                if (strategy.equals(k2, typedKey)) {
                    return true;
                }
                if (k3 != null) {
                    return strategy.equals(k3, typedKey);
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (k1 != null) {
            if (Objects.equals(v1, value)) {
                return true;
            }
            if (k2 != null) {
                if (Objects.equals(v2, value)) {
                    return true;
                }
                if (k3 != null && Objects.equals(v3, value)) {
                    return true;
                }
            }
        }
        for (int i = 0; i < data.length; i += 2) {
            if (data[i] != null && Objects.equals(data[i + 1], value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        return getOrDefault(key, null);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        @SuppressWarnings("unchecked") K typedKey = (K)key;
        if (k1 != null) {
            if (strategy.equals(k1, typedKey)) {
                return v1;
            }
            if (k2 != null) {
                if (strategy.equals(k2, typedKey)) {
                    return v2;
                }
                if (k3 != null && strategy.equals(k3, typedKey)) {
                    return v3;
                }
            }
        }
        if (data.length == 0) {
            return defaultValue;
        }
        int pos = tablePos(strategy, data, typedKey);
        @SuppressWarnings("unchecked") V v = pos < 0 ? defaultValue : (V)data[pos + 1];
        return v;
    }

    private static <K> int tablePos(HashingStrategy<K> strategy, Object[] data, K key) {
        int pos = Math.floorMod(strategy.hashCode(key), data.length >>> 1) << 1;
        while (true) {
            @SuppressWarnings("unchecked")
            K candidate = (K)data[pos];
            if (candidate == null) {
                return ~pos;
            }
            if (strategy.equals(candidate, key)) {
                return pos;
            }
            pos += 2;
            if (pos == data.length) {
                pos = 0;
            }
        }
    }

    private static <K> K ensureKeyIsNotNull(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Null keys are not supported");
        }
        return key;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Map)) {
            return false;
        }
        Map<?, ?> map = (Map<?, ?>)obj;
        if (size() != map.size()) {
            return false;
        }
        if (k1 != null) {
            if (!Objects.equals(map.get(k1), v1)) {
                return false;
            }
            if (k2 != null) {
                if (!Objects.equals(map.get(k2), v2)) {
                    return false;
                }
                if (k3 != null && !Objects.equals(map.get(k3), v3)) {
                    return false;
                }
            }
        }
        for (int i = 0; i < data.length; i += 2) {
            @SuppressWarnings("unchecked") K key = (K)data[i];
            if (key != null && !Objects.equals(map.get(key), data[i + 1])) {
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (k1 != null) {
            if (k2 != null) {
                if (k3 != null) {
                    action.accept(k3, v3);
                }
                action.accept(k2, v2);
            }
            action.accept(k1, v1);
        }
        for (int i = 0; i < data.length; i += 2) {
            Object key = data[i];
            if (key != null) {
                action.accept((K)key, (V)data[i + 1]);
            }
        }
    }

    private abstract class MyIterator<E> implements Iterator<E> {
        /**
         * Points to the next key
         * -3 => k3, -2 => k2, -1 => k1, >= 0 => offset in the data table
         */
        int pos;

        MyIterator() {
            if (k1 == null) {
                pos = -1;
                advance();
            }
            else {
                pos = k2 == null ? -1 : k3 == null ? -2 : -3;
            }
        }

        @Override
        public boolean hasNext() {
            return pos < data.length;
        }

        private void advance() {
            if (pos < 0) {
                pos++;
            }
            else {
                pos += 2;
            }

            if (pos >= 0) {
                while (pos < data.length && data[pos] == null) {
                    pos++;
                }
            }
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (pos < 0) {
                int offset = ~pos;
                advance();
                return fieldElement(offset);
            }
            int offset = pos;
            advance();
            return tableElement(offset);
        }

        abstract E fieldElement(int offset);

        abstract E tableElement(int offset);
    }

    @Nonnull
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet<>() {
                @Nonnull
                @Override
                public Iterator<K> iterator() {
                    return new MyIterator<K>() {
                        @Override
                        K fieldElement(int offset) {
                            return offset == 0 ? k1 : offset == 1 ? k2 : k3;
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        K tableElement(int offset) {
                            return (K)data[offset];
                        }
                    };
                }

                @Override
                public void forEach(Consumer<? super K> action) {
                    if (k1 != null) {
                        if (k2 != null) {
                            if (k3 != null) {
                                action.accept(k3);
                            }
                            action.accept(k2);
                        }
                        action.accept(k1);
                    }
                    for (int i = 0; i < data.length; i += 2) {
                        Object key = data[i];
                        if (key != null) {
                            @SuppressWarnings("unchecked") K k = (K)data[i];
                            action.accept(k);
                        }
                    }
                }

                @Override
                public boolean contains(Object o) {
                    return containsKey(o);
                }

                @Override
                public int size() {
                    return UnmodifiableHashMap.this.size();
                }
            };
        }
        return keySet;
    }

    @Nonnull
    @Override
    public Collection<V> values() {
        if (values == null) {
            values = new AbstractCollection<>() {
                @Nonnull
                @Override
                public Iterator<V> iterator() {
                    return new MyIterator<V>() {
                        @Override
                        V fieldElement(int offset) {
                            return offset == 0 ? v1 : offset == 1 ? v2 : v3;
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        V tableElement(int offset) {
                            return (V)data[offset + 1];
                        }
                    };
                }

                @Override
                public void forEach(Consumer<? super V> action) {
                    if (k1 != null) {
                        if (k2 != null) {
                            if (k3 != null) {
                                action.accept(v3);
                            }
                            action.accept(v2);
                        }
                        action.accept(v1);
                    }
                    for (int i = 0; i < data.length; i += 2) {
                        Object key = data[i];
                        if (key != null) {
                            @SuppressWarnings("unchecked")
                            V v = (V)data[i + 1];
                            action.accept(v);
                        }
                    }
                }

                @Override
                public boolean contains(Object o) {
                    return containsValue(o);
                }

                @Override
                public int size() {
                    return UnmodifiableHashMap.this.size();
                }
            };
        }
        return values;
    }

    @Nonnull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Nonnull
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new MyIterator<Entry<K, V>>() {
                    @Override
                    Entry<K, V> fieldElement(int offset) {
                        return offset == 0
                            ? new AbstractMap.SimpleImmutableEntry<>(k1, v1)
                            : offset == 1
                            ? new AbstractMap.SimpleImmutableEntry<>(k2, v2)
                            : new AbstractMap.SimpleImmutableEntry<>(k3, v3);
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    Entry<K, V> tableElement(int offset) {
                        return new AbstractMap.SimpleImmutableEntry<>((K)data[offset], (V)data[offset + 1]);
                    }
                };
            }

            @Override
            public int size() {
                return UnmodifiableHashMap.this.size();
            }
        };
    }
}