/*
 * Copyright 2013-2024 consulo.io
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

import consulo.util.collection.impl.map.ReusableLinkedHashtable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2024-12-03
 */
public class ImmutableLinkedHashMap<K, V> extends AbstractImmutableMap<K, V> implements SequencedMap<K, V> {
    private static final ImmutableLinkedHashMap<Object, Object> EMPTY = of(ReusableLinkedHashtable.empty());

    protected ReusableLinkedHashtable<K, V> myTable;

    /**
     * Returns an empty {@code SimpleImmutableLinkedHashMap} with canonical equals/hashCode strategy
     * (using equals/hashCode of the key as is).
     *
     * @return an empty {@code SimpleImmutableLinkedHashMap}.
     * @see HashingStrategy#canonical()
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableLinkedHashMap<K, V> empty() {
        return (ImmutableLinkedHashMap<K, V>)EMPTY;
    }

    /**
     * Returns an empty {@code SimpleImmutableLinkedHashMap} with supplied equals/hashCode strategy.
     *
     * @param strategy strategy to compare/hash keys.
     * @return an empty {@code SimpleImmutableLinkedHashMap}.
     */
    @Nonnull
    public static <K, V> ImmutableLinkedHashMap<K, V> empty(HashingStrategy<K> strategy) {
        return strategy == HashingStrategy.canonical() ? empty() : of(ReusableLinkedHashtable.<K, V>empty(strategy));
    }

    /**
     * <p>Returns an {@code SimpleImmutableLinkedHashMap} which contains all the entries of the supplied map.</p>
     *
     * <p>Will return the supplied map if it's already an {@code SimpleImmutableLinkedHashMap} with canonical equals/hashCode strategy.</p>
     *
     * @param map map to copy key/values from. Must not have {@code null} keys.
     * @return An {@code SimpleImmutableLinkedHashMap} with values from supplied map.
     * @throws IllegalArgumentException if map contains {@code null} keys.
     */
    @Nonnull
    public static <K, V> ImmutableLinkedHashMap<K, V> fromMap(@Nonnull Map<? extends K, ? extends V> map) {
        return fromMap(HashingStrategy.canonical(), map);
    }

    /**
     * <p>Returns an {@code SimpleImmutableLinkedHashMap} which contains all the entries of the supplied map.
     * It's assumed that the supplied map remains unchanged during the {@code fromMap} call.</p>
     *
     * @param strategy strategy to compare keys
     * @param map      map to copy values from
     * @return a pre-populated {@code SimpleImmutableLinkedHashMap}. Map return the supplied map if
     * it's already an {@code SimpleImmutableLinkedHashMap} which uses the same equals/hashCode strategy.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableLinkedHashMap<K, V> fromMap(
        @Nonnull HashingStrategy<K> strategy,
        @Nonnull Map<? extends K, ? extends V> map
    ) {
        if (map instanceof ImmutableLinkedHashMap ilhm && ilhm.getStrategy() == strategy) {
            // Same strategy SimpleImmutableLinkedHashMap. Reusing it.
            return (ImmutableLinkedHashMap<K, V>)ilhm;
        }
        else if (map.isEmpty()) {
            // Empty map optimization.
            return empty(strategy);
        }

        ReusableLinkedHashtable<K, V> newTable = ReusableLinkedHashtable.blankOfSize(strategy, map.size());
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            newTable.insertNullable(entry.getKey(), entry.getValue());
        }
        return of(newTable);
    }

    @Override
    public ImmutableLinkedHashMap<K, V> reversed() {
        return of(myTable.copyReversed());
    }

    /**
     * Returns an {@code SimpleImmutableLinkedHashMap} which contains all the entries of the current map except the supplied key.
     *
     * @param key a key to exclude from the result
     * @return an {@code SimpleImmutableLinkedHashMap} which contains all the entries of the current map except the supplied key
     */
    @Contract(pure = true)
    @Nonnull
    @Override
    public ImmutableLinkedHashMap<K, V> without(@Nonnull K key) {
        if (isEmpty()) {
            return this;
        }

        ReusableLinkedHashtable<K, V> table = myTable;
        int keyPos = table.getPos(key), newSize = table.getSize() - 1;
        if (keyPos < 0) {
            // Element is not found, return current map.
            return this;
        }
        else if (newSize == 0) {
            // Removing the last element in the map — return empty map.
            return empty(getStrategy());
        }

        return of(table.copyRangeWithout(newSize, table, null, keyPos));
    }

    /**
     * <p>Returns an {@code SimpleImmutableLinkedHashMap} which contains all the entries of the current map
     * and the supplied key/value pair.</p>
     *
     * <p>May return the same map if given key is already associated with the same value. Note, however, that if value is
     * not the same but equal object, the new map will be created as sometimes it's desired to replace the object with
     * another one which is equal to the old object.</p>
     *
     * @param key   a key to add/replace.
     * @param value a value to associate with the key.
     * @return an {@code SimpleImmutableLinkedHashMap} which contains all the entries of the current map plus the supplied mapping.
     */
    @Contract(pure = true)
    @Nonnull
    @Override
    public ImmutableLinkedHashMap<K, V> with(@Nonnull K key, @Nullable V value) {
        if (isEmpty()) {
            return fromMap(Collections.singletonMap(key, value));
        }

        ReusableLinkedHashtable<K, V> table = this.myTable;
        int hashCode = table.hashCode(key), keyPos = table.getPos(hashCode, key);
        if (keyPos >= 0) {
            if (table.getValue(keyPos) == value) {
                // Value is the same. Reusing current map.
                return this;
            }
            return of(table.copy().setValueAtPos(keyPos, value));
        }

        return of(table.copyOfSize(table.getSize() + 1).insert(hashCode, key, value));
    }

    /**
     * <p>Returns an {@code SimpleImmutableLinkedHashMap} which contains all the entries of the current map
     * plus all the mappings of the supplied map.</p>
     *
     * <p>May (but not guaranteed) return the same map if the supplied map is empty or all its
     * mappings already exist in this map (assuming values are compared by reference). The equals/hashCode strategy
     * of the resulting map is the same as the strategy of this map.</p>
     *
     * @param map to add entries from.
     * @return An {@code SimpleImmutableLinkedHashMap} which contains all the entries of the current map
     * plus all the mappings of the supplied map.
     */
    @Nonnull
    @Override
    public ImmutableLinkedHashMap<K, V> withAll(@Nonnull Map<? extends K, ? extends V> map) {
        if (isEmpty()) {
            // Optimization for empty current map.
            return fromMap(getStrategy(), map);
        }

        int mapSize = map.size();
        if (mapSize == 0) {
            // Optimization for empty map.
            return this;
        }
        else if (mapSize == 1) {
            // Optimization for single-entry map.
            Entry<? extends K, ? extends V> entry = map.entrySet().iterator().next();
            return with(entry.getKey(), entry.getValue());
        }

        ReusableLinkedHashtable<K, V> newTable = myTable.copyRangeWithout(size() + map.size(), myTable, map.keySet(), -1);
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            newTable.insertNullable(entry.getKey(), entry.getValue());
        }
        return of(newTable);
    }

    @Override
    public int size() {
        return myTable.getSize();
    }

    @Override
    public boolean isEmpty() {
        return myTable.getSize() == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        return key != null && myTable.getPos((K)key) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        return myTable.isValueInList(myTable, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V defaultValue) {
        if (key == null) {
            return defaultValue;
        }

        ReusableLinkedHashtable<K, V> table = myTable;
        int keyPos = table.getPos((K)key);
        return keyPos < 0 ? defaultValue : (V)table.getValue(keyPos);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super K, ? super V> action) {
        myTable.forEach(myTable, action);
    }

    @Nonnull
    @Override
    public Set<K> keySet() {
        return sequencedKeySet();
    }

    @Nonnull
    @Override
    public Collection<V> values() {
        return sequencedValues();
    }

    /**
     * <p>This entry set uses single instance of {@code FlyweightEntry} to represent all entries of the map.</p>
     *
     * <p>We do not store entries as objects, only key/value pairs. To avoid expensive creation of entry object for each key/value pair
     * we create only a single instance of {@code FlyweightEntry} per iterator and it returns key/value pointed by iterator.</p>
     *
     * <p>To prevent misuse, {@code FlyweightEntry} throws UnsupportedOperationException if you call equals/hashCode on it.</p>
     *
     * @return Set to be used for iteration only.
     */
    @Nonnull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return sequencedEntrySet();
    }

    @Override
    public SequencedSet<K> sequencedKeySet() {
        return new MyKeySet();
    }

    @Override
    public SequencedCollection<V> sequencedValues() {
        return new MyValues();
    }

    @Override
    public SequencedSet<Entry<K, V>> sequencedEntrySet() {
        return new MyEntrySet();
    }

    @Nonnull
    @Override
    public HashingStrategy<K> getStrategy() {
        return myTable.getStrategy();
    }

    protected ImmutableLinkedHashMap(@Nonnull ReusableLinkedHashtable<K, V> table) {
        myTable = table;
    }

    private static <K, V> ImmutableLinkedHashMap<K, V> of(ReusableLinkedHashtable<K, V> table) {
        return new ImmutableLinkedHashMap<>(table);
    }

    private class MyKeySet extends AbstractSet<K> implements SequencedSet<K> {
        @Nonnull
        @Override
        public Iterator<K> iterator() {
            return myTable.new KeyIterator(myTable);
        }

        @Override
        public void forEach(Consumer<? super K> action) {
            myTable.forEachKey(myTable, action);
        }

        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public int size() {
            return ImmutableLinkedHashMap.this.size();
        }

        @Override
        public SequencedSet<K> reversed() {
            return ImmutableLinkedHashMap.this.reversed().sequencedKeySet();
        }
    }

    private class MyValues extends AbstractCollection<V> implements SequencedCollection<V> {
        @Nonnull
        @Override
        public Iterator<V> iterator() {
            return myTable.new ValueIterator(myTable);
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            myTable.forEachValue(myTable, action);
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public int size() {
            return ImmutableLinkedHashMap.this.size();
        }

        @Override
        public SequencedCollection<V> reversed() {
            return ImmutableLinkedHashMap.this.reversed().sequencedValues();
        }
    }

    private class MyEntrySet extends AbstractSet<Entry<K, V>> implements SequencedSet<Entry<K, V>> {
        @Nonnull
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return myTable.new EntryIterator(myTable);
        }

        @Override
        public int size() {
            return ImmutableLinkedHashMap.this.size();
        }

        @Override
        public SequencedSet<Entry<K, V>> reversed() {
            return ImmutableLinkedHashMap.this.reversed().sequencedEntrySet();
        }
    }
}
