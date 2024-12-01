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
import consulo.util.collection.impl.map.ReusableLinkedHashtableUser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>An immutable hash map optimized for frequent adding of new keys. It stores order of adding of new keys and during iteration returns
 * entries in that order. Custom equals/hashCode strategy is supported. Null keys are prohibited. Null values are allowed.</p>
 *
 * <p>Keys and values are both stored in an open-addressed hash-table. All map entries are connected in a singly linked list.
 * The start and the end of the list are marked. So when adding new element we can reuse hash-tables, just mark new element as the last.
 * This won't disturb previously created maps referencing same hash-table.</p>
 *
 * <p>Initially map is created 25% full and grows up to 50% full (to prevent degradation due to cache collisions),
 * after that hash-table is recreated. So hash-table recreation happens only log<sub>2</sub>(N) times during map growth.</p>
 *
 * <p>The last entry of the list references the first entry. So if the last marked entry isn't referencing the first marked entry we know
 * that we cannot safely reuse hash-table (because hash-table was already reused by another {@code ImmutableLinkedHashMap} instance).</p>
 *
 * @see HashingStrategy
 * @author UNV
 * @since 2024-11-18
 */
public class ImmutableLinkedHashMap<K, V> implements ImmutableMap<K, V>, ReusableLinkedHashtableUser {
    private static final ImmutableLinkedHashMap<Object, Object> EMPTY = of(ReusableLinkedHashtable.empty());

    protected ReusableLinkedHashtable<K, V>.Range myRange;

    /**
     * Returns an empty {@code ImmutableLinkedHashMap} with canonical equals/hashCode strategy (using equals/hashCode of the key as is).
     *
     * @return an empty {@code ImmutableLinkedHashMap}.
     * @see HashingStrategy#canonical()
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableLinkedHashMap<K, V> empty() {
        return (ImmutableLinkedHashMap<K, V>)EMPTY;
    }

    /**
     * Returns an empty {@code ImmutableLinkedHashMap} with supplied equals/hashCode strategy.
     *
     * @param strategy strategy to compare/hash keys.
     * @return an empty {@code ImmutableLinkedHashMap}.
     */
    @Nonnull
    public static <K, V> ImmutableLinkedHashMap<K, V> empty(HashingStrategy<K> strategy) {
        return strategy == HashingStrategy.canonical() ? empty() : of(ReusableLinkedHashtable.<K, V>empty(strategy));
    }

    /**
     * <p>Returns an {@code ImmutableLinkedHashMap} which contains all the entries of the supplied map.</p>
     *
     * <p>Will return the supplied map if it's already an {@code ImmutableLinkedHashMap} with canonical equals/hashCode strategy.</p>
     *
     * @param map map to copy key/values from. Must not have {@code null} keys.
     * @return An {@code ImmutableLinkedHashMap} with values from supplied map.
     * @throws IllegalArgumentException if map contains {@code null} keys.
     */
    @Nonnull
    public static <K, V> ImmutableLinkedHashMap<K, V> fromMap(@Nonnull Map<? extends K, ? extends V> map) {
        return fromMap(HashingStrategy.canonical(), map);
    }

    /**
     * <p>Returns an {@code ImmutableLinkedHashMap} which contains all the entries of the supplied map.
     * It's assumed that the supplied map remains unchanged during the {@code fromMap} call.</p>
     *
     * @param strategy strategy to compare keys
     * @param map      map to copy values from
     * @return a pre-populated {@code ImmutableLinkedHashMap}. Map return the supplied map if
     * it's already an {@code ImmutableLinkedHashMap} which uses the same equals/hashCode strategy.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableLinkedHashMap<K, V> fromMap(
        @Nonnull HashingStrategy<K> strategy,
        @Nonnull Map<? extends K, ? extends V> map
    ) {
        if (map instanceof ImmutableLinkedHashMap ilhm && ilhm.getStrategy() == strategy) {
            // Same strategy ImmutableLinkedHashMap. Reusing it.
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

    /**
     * Returns an {@code ImmutableLinkedHashMap} which contains all the entries of the current map except the supplied key.
     *
     * @param key a key to exclude from the result
     * @return an {@code ImmutableLinkedHashMap} which contains all the entries of the current map except the supplied key
     */
    @Contract(pure = true)
    @Nonnull
    @Override
    public ImmutableLinkedHashMap<K, V> without(@Nonnull K key) {
        ReusableLinkedHashtable<K, V>.Range range = myRange;
        if (range.mySize == 0) {
            return this;
        }

        ReusableLinkedHashtable<K, V> table = range.getTable();
        int keyPos = table.getPos(key), newSize = range.mySize - 1;
        if (keyPos < 0 || !range.isMasterRange() && !range.isInList(keyPos)) {
            // Element is not found, return current map.
            return this;
        }
        else if (range.mySize == 1) {
            // Removing the last element in the map — return empty map.
            return empty(getStrategy());
        }
        else if (keyPos == range.myStartPos) {
            // Removing the first element in the list. Reuse hash-table, just mark second element in the list as first.
            return reuse(newSize, range.getPosAfter(range.myStartPos), range.myEndPos);
        }
        else if (keyPos == range.myEndPos) {
            // Removing the last element in the list. Reuse hash-table, just mark as last an element in the list before last.
            return reuse(newSize, range.myStartPos, range.getPosBefore(range.myEndPos));
        }

        return of(range.copyWithout(newSize, null, keyPos));
    }

    /**
     * <p>Returns an {@code ImmutableLinkedHashMap} which contains all the entries of the current map and the supplied key/value pair.</p>
     *
     * <p>May return the same map if given key is already associated with the same value. Note, however, that if value is
     * not the same but equal object, the new map will be created as sometimes it's desired to replace the object with
     * another one which is equal to the old object.</p>
     *
     * @param key   a key to add/replace.
     * @param value a value to associate with the key.
     * @return an {@code ImmutableLinkedHashMap} which contains all the entries of the current map plus the supplied mapping.
     */
    @Contract(pure = true)
    @Nonnull
    @Override
    public ImmutableLinkedHashMap<K, V> with(@Nonnull K key, @Nullable V value) {
        ReusableLinkedHashtable<K, V>.Range range = myRange;
        if (range.mySize == 0) {
            return fromMap(Collections.singletonMap(key, value));
        }

        ReusableLinkedHashtable<K, V> table = range.getTable();
        int hashCode = table.hashCode(key), keyPos = table.getPos(hashCode, key);
        if (keyPos >= 0 && !range.isMasterRange()) {
            // We're not in the largest map sharing current hash-table. So we need to check if found key is actually in our list.
            if (range.isInList(keyPos)) {
                if (table.getValue(keyPos) == value) {
                    // Value is the same. Reusing current map.
                    return this;
                }
                return of(range.copy().setValueAtPos(keyPos, value));
            }
            return of(range.copy().insertAtPos(keyPos, hashCode, key, value));
        }

        if (keyPos >= 0) {
            if (table.getValue(keyPos) == value) {
                // Value is the same. Reusing current map.
                return this;
            }
            // Different value of the same key. Copy hash-table and replace value.
            return of(range.copy().setValueAtPos(keyPos, value));
        }

        int newSize = range.mySize + 1;
        if (range.canResizeTableTo(newSize)) {
            // Hash-table is less than 50% full. Reusing it.
            return of(table.insertAtPos(~keyPos, hashCode, key, value));
        }

        // Recreating hash-table.
        return of(table.copyOfSize(newSize).insert(hashCode, key, value));
    }

    /**
     * <p>Returns an {@code ImmutableLinkedHashMap} which contains all the entries of the current map
     * plus all the mappings of the supplied map.</p>
     *
     * <p>May (but not guaranteed) return the same map if the supplied map is empty or all its
     * mappings already exist in this map (assuming values are compared by reference). The equals/hashCode strategy
     * of the resulting map is the same as the strategy of this map.</p>
     *
     * @param map to add entries from.
     * @return An {@code ImmutableLinkedHashMap} which contains all the entries of the current map
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

        return withAllTryReusing(map);
    }

    @Override
    public int size() {
        return myRange.mySize;
    }

    @Override
    public boolean isEmpty() {
        return myRange.mySize == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        ReusableLinkedHashtable<K, V>.Range range = myRange;
        ReusableLinkedHashtable<K, V> table = range.getTable();
        int keyPos = table.getPos((K)key);
        if (keyPos < 0) {
            return false;
        }
        if (myRange.isMasterRange()) {
            return true;
        }
        // We're not in the largest map sharing current hash-table. So we need to check if found key is actually in our list.
        return range.isInList(keyPos);
    }

    @Override
    public boolean containsValue(Object value) {
        return myRange.isValueInList(value);
    }

    @Override
    public V get(Object key) {
        return getOrDefault(key, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V defaultValue) {
        ReusableLinkedHashtable<K, V>.Range range = myRange;
        if (key == null || range.mySize == 0) {
            return defaultValue;
        }

        ReusableLinkedHashtable<K, V> table = range.getTable();
        int keyPos = table.getPos((K)key);
        if (keyPos < 0) {
            return defaultValue;
        }
        if (range.isMasterRange()) {
            return (V)table.getValue(keyPos);
        }

        // We're not in the largest map sharing current hash-table. So we need to check if found key is actually in our list.
        return range.isInList(keyPos) ? (V)table.getValue(keyPos) : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super K, ? super V> action) {
        myRange.forEach(action);
    }

    @Nonnull
    @Override
    public Set<K> keySet() {
        return new MyKeySet();
    }

    @Nonnull
    @Override
    public Collection<V> values() {
        return new MyValues();
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
        return new MyEntrySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int hashCode() {
        HashingStrategy<K> strategy = getStrategy();
        int[] h = new int[]{0};
        forEach((key, value) -> h[0] += strategy.hashCode(key) ^ Objects.hashCode(value));
        return h[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        return obj instanceof Map map
            && size() == map.size()
            && entrySet().stream().allMatch(entry -> Objects.equals(map.get(entry.getKey()), entry.getValue()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append('{');
        forEach((k, v) -> {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(k).append('=').append(v);
        });
        return sb.append('}').toString();
    }

    public HashingStrategy<K> getStrategy() {
        return myRange.getTable().getStrategy();
    }

    protected ImmutableLinkedHashMap(@Nonnull ReusableLinkedHashtable<K, V> table) {
        myRange = table.rangeFor(this);
    }

    protected ImmutableLinkedHashMap(@Nonnull ReusableLinkedHashtable<K, V> table, int size, int startPos, int endPos) {
        myRange = table.rangeFor(this, size, startPos, endPos);
    }

    private ImmutableLinkedHashMap<K, V> reuse(int size, int startPos, int endPos) {
        return new ImmutableLinkedHashMap<>(myRange.getTable(), size, startPos, endPos);
    }

    private static <K, V> ImmutableLinkedHashMap<K, V> of(ReusableLinkedHashtable<K, V> table) {
        return new ImmutableLinkedHashMap<>(table);
    }

    private ImmutableLinkedHashMap<K, V> withAllTryReusing(@Nonnull Map<? extends K, ? extends V> map) {
        ReusableLinkedHashtable<K, V>.Range range = myRange;
        ReusableLinkedHashtable<K, V> table = range.getTable();
        int newSize = range.mySize + map.size();
        if (range.canResizeTableTo(newSize)) {
            // Hash-table is less than 50% full and we're in the largest map using current hash-table. Reusing it.
            boolean keyDuplication = false;
            for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
                K key = entry.getKey();
                if (key == null) {
                    // Null key is encountered during appending values to hash-table. Cleaning up and throwing exception.
                    table.removeRange(range.myEndPos, table.getEndPos());
                    throw new IllegalArgumentException("Null keys are not supported");
                }
                int hashCode = table.hashCode(key), pos = table.getPos(hashCode, key);
                if (pos >= 0) {
                    if (table.getValue(pos) == entry.getValue()) {
                        // Value is the same. Nothing to add.
                        continue;
                    }
                    else {
                        // Duplicate key with different value found: cleaning up filled values from hash-table. Then recreating.
                        keyDuplication = true;
                        table.removeRange(range.myEndPos, table.getEndPos());
                        break;
                    }
                }
                table.insertAtPos(~pos, hashCode, key, entry.getValue());
            }

            if (!keyDuplication) {
                if (range.isMasterRange()) {
                    // All key/value pairs from the supplied map were the same as in current map
                    return this;
                }
                else {
                    return of(table);
                }
            }
        }

        ReusableLinkedHashtable<K, V> newTable = range.copyWithout(newSize, map.keySet(), -1);
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            newTable.insertNullable(entry.getKey(), entry.getValue());
        }
        return of(newTable);
    }

    @Override
    public void detachFromTable() {
        ReusableLinkedHashtable<K, V>.Range range = myRange;
        if (!range.isMasterRange()) {
            myRange = range.copy().rangeFor(this);
        }
    }

    private class MyKeySet extends AbstractSet<K> {
        @Nonnull
        @Override
        public Iterator<K> iterator() {
            return myRange.keyIterator();
        }

        @Override
        public void forEach(Consumer<? super K> action) {
            myRange.forEachKey(action);
        }

        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public int size() {
            return ImmutableLinkedHashMap.this.size();
        }
    }

    private class MyValues extends AbstractCollection<V> {
        @Nonnull
        @Override
        public Iterator<V> iterator() {
            return myRange.valueIterator();
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            myRange.forEachValue(action);
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public int size() {
            return ImmutableLinkedHashMap.this.size();
        }
    }

    private class MyEntrySet extends AbstractSet<Entry<K, V>> {
        @Nonnull
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return myRange.entryIterator();
        }

        @Override
        public int size() {
            return ImmutableLinkedHashMap.this.size();
        }
    }
}