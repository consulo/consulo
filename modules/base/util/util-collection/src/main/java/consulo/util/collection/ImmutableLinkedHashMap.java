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

import consulo.util.collection.impl.map.ReusableLinkedHashtableRange;
import consulo.util.collection.impl.map.ReusableLinkedHashtable;
import consulo.util.collection.impl.map.ReusableLinkedHashtableUser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.lang.ref.WeakReference;
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
public class ImmutableLinkedHashMap<K, V> implements Map<K, V>, ReusableLinkedHashtableRange, ReusableLinkedHashtableUser {
    private static final ImmutableLinkedHashMap<Object, Object> EMPTY = of(ReusableLinkedHashtable.empty());

    @Nonnull
    private ReusableLinkedHashtable<K, V> myTable;
    private final int mySize;
    private int myStartPos, myEndPos;
    private WeakReference<SubCollectionsCache> mySubCollectionCache = null;

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
        if (map instanceof ImmutableLinkedHashMap ilhm && ilhm.myTable.getStrategy() == strategy) {
            // Same strategy ImmutableLinkedHashMap. Reusing it.
            return (ImmutableLinkedHashMap<K, V>)ilhm;
        }
        else if (map.isEmpty()) {
            // Empty map optimization.
            return empty(strategy);
        }

        ReusableLinkedHashtable<K, V> newTable = ReusableLinkedHashtable.blankOfSize(strategy, map.size());
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            newTable.insert(ensureKeyIsNotNull(entry.getKey()), entry.getValue());
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
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public ImmutableLinkedHashMap<K, V> without(@Nonnull K key) {
        if (mySize == 0) {
            return this;
        }

        int keyPos;
        boolean checkPossibleMatchFromSatellite = false;
        synchronized (myTable) {
            keyPos = myTable.getPos(key);
            if (keyPos >= 0 && !isMaster()) {
                checkPossibleMatchFromSatellite = true;
            }
        }

        if (checkPossibleMatchFromSatellite) {
            // We're not in the largest map sharing current hash-table. So we need to check if found key is actually in our list.
            return myTable.isInList(this, keyPos) ? of(myTable.copyRangeWithoutPos(this, keyPos)) : this;
        }

        if (keyPos < 0) {
            // Element is not found, return current map.
            return this;
        }
        else if (mySize == 1) {
            // Removing the last element in the map — return empty map.
            return empty(myTable.getStrategy());
        }
        else if (keyPos == myStartPos) {
            // Removing the first element in the list. Reuse hash-table, just mark second element in the list as first.
            return reuse(mySize - 1, myTable.getPosAfter(myStartPos), myEndPos);
        }
        else if (keyPos == myEndPos) {
            // Removing the last element in the list. Reuse hash-table, just mark as last an element in the list before last.
            return reuse(mySize - 1, myStartPos, myTable.getPosBefore(this, myEndPos));
        }

        return of(myTable.copyRangeWithoutPos(this, keyPos));
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
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public ImmutableLinkedHashMap<K, V> with(@Nonnull K key, @Nullable V value) {
        if (mySize == 0) {
            return fromMap(Collections.singletonMap(key, value));
        }

        int keyPos;
        boolean checkPossibleMatchFromSatellite = false;
        synchronized (myTable) {
            keyPos = myTable.getPos(key);
            if (keyPos >= 0 && !isMaster()) {
                checkPossibleMatchFromSatellite = true;
            }
        }

        if (checkPossibleMatchFromSatellite) {
            // We're not in the largest map sharing current hash-table. So we need to check if found key is actually in our list.
            if (myTable.isInList(this, keyPos)) {
                if (myTable.getValue(keyPos) == value) {
                    // Value is the same. Reusing current map.
                    return this;
                }
                return of(myTable.copyRange(this).setValueAtPos(keyPos, value));
            }
            return of(myTable.copyRange(this).insertAtPos(keyPos, key, value));
        }

        if (keyPos >= 0) {
            if (myTable.getValue(keyPos) == value) {
                // Value is the same. Reusing current map.
                return this;
            }
            // Different value of the same key. Copy hash-table and replace value.
            return of(myTable.copyRange(this).setValueAtPos(keyPos, value));
        }

        if (mySize + 1 < myTable.getMaxSafeSize()) {
            synchronized (myTable) {
                if (isMaster()) {
                    // Hash-table is less than 50% full. Reusing it.
                    return of(myTable.insertAtPos(~keyPos, key, value));
                }
            }
        }

        // Recreating hash-table.
        return of(myTable.copyOfSize(mySize + 1).insert(key, value));
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
    public ImmutableLinkedHashMap<K, V> withAll(@Nonnull Map<? extends K, ? extends V> map) {
        if (isEmpty()) {
            // Optimization for empty current map.
            return fromMap(myTable.getStrategy(), map);
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
        return mySize;
    }

    @Override
    public boolean isEmpty() {
        return mySize == 0;
    }

    @Override
    @SuppressWarnings({"unchecked", "SynchronizeOnNonFinalField"})
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        int keyPos;
        synchronized (myTable) {
            keyPos = myTable.getPos((K)key);
            if (keyPos < 0) {
                return false;
            }
            if (isMaster()) {
                return true;
            }
        }
        // We're not in the largest map sharing current hash-table. So we need to check if found key is actually in our list.
        return myTable.isInList(this, keyPos);
    }

    @Override
    public boolean containsValue(Object value) {
        return myTable.isValueInList(this, value);
    }

    @Override
    public V get(Object key) {
        return getOrDefault(key, null);
    }

    @Override
    @SuppressWarnings({"unchecked", "SynchronizeOnNonFinalField"})
    public V getOrDefault(Object key, V defaultValue) {
        if (key == null || mySize == 0) {
            return defaultValue;
        }
        int keyPos;
        synchronized (myTable) {
            keyPos = myTable.getPos((K)key);
            if (keyPos < 0) {
                return defaultValue;
            }
            if (isMaster()) {
                return (V)myTable.getValue(keyPos);
            }
        }

        // We're not in the largest map sharing current hash-table. So we need to check if found key is actually in our list.
        return myTable.isInList(this, keyPos) ? (V)myTable.getValue(keyPos) : defaultValue;
    }

    /**
     * Unsupported operation: this map is immutable. Use {@link #with(Object, Object)} to create a new
     * {@code ImmutableLinkedHashMap} with an additional element.
     */
    @Deprecated
    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation: this map is immutable. Use {@link #without(Object)} to create a new
     * {@code ImmutableLinkedHashMap} without some element.
     */
    @Deprecated
    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation: this map is immutable. Use {@link #withAll(Map)} to create a new
     * {@code ImmutableLinkedHashMap} with additional elements from the specified Map.
     */
    @Deprecated
    @Override
    public void putAll(@Nonnull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation: this map is immutable. Use {@link #empty()} to get an empty {@code ImmutableLinkedHashMap}.
     */
    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super K, ? super V> action) {
        myTable.forEach(this, action);
    }

    @Nonnull
    @Override
    public Set<K> keySet() {
        return getSubCollectionCache().getKeySet();
    }

    @Nonnull
    @Override
    public Collection<V> values() {
        return getSubCollectionCache().getValues();
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
        return getSubCollectionCache().getEntrySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int hashCode() {
        HashingStrategy<K> strategy = myTable.getStrategy();
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

    @Override
    public int getStartPos() {
        return myStartPos;
    }

    @Override
    public int getEndPos() {
        return myEndPos;
    }

    protected ImmutableLinkedHashMap(@Nonnull ReusableLinkedHashtable<K, V> table) {
        this(table, table.getSize(), table.getStartPos(), table.getEndPos());
    }

    protected ImmutableLinkedHashMap(@Nonnull ReusableLinkedHashtable<K, V> table, int size, int startPos, int endPos) {
        myTable = table;
        mySize = size;
        myStartPos = startPos;
        myEndPos = endPos;
        table.link(this);
    }

    private ImmutableLinkedHashMap<K, V> reuse(int size, int startPos, int endPos) {
        return new ImmutableLinkedHashMap<>(myTable, size, startPos, endPos);
    }

    private static <K, V> ImmutableLinkedHashMap<K, V> of(ReusableLinkedHashtable<K, V> table) {
        return new ImmutableLinkedHashMap<>(table);
    }

    /**
     * <p>Checks if we can safely reuse current hash-table for appending new entries without hash-table recreation.
     * This is only possible if no elements were added after the last element or before the first element of the list
     * (the last element points to the first element).</p>
     *
     * <p>Must be called from synchronized context!</p>
     *
     * @return {@code true} if we can safely reuse this hash-table for adding new elements.
     * {@code false} if hash-table need to be recreated.
     */
    @Override
    public boolean isMaster() {
        return mySize == myTable.getSize();
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    private ImmutableLinkedHashMap<K, V> withAllTryReusing(@Nonnull Map<? extends K, ? extends V> map) {
        if (mySize + map.size() < myTable.getMaxSafeSize()) {
            synchronized (myTable) {
                if (isMaster()) {
                    // Hash-table is less than 50% full and we're in the largest map using current hash-table. Reusing it.
                    boolean keyDuplication = false;
                    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
                        K key = entry.getKey();
                        if (key == null) {
                            // Null key is encountered during appending values to hash-table. Cleaning up and throwing exception.
                            myTable.removeRange(myEndPos, myTable.getEndPos());
                            throwKeyIsNull();
                        }
                        int pos = myTable.getPos(key);
                        if (pos >= 0) {
                            if (myTable.getValue(pos) == entry.getValue()) {
                                // Value is the same. Nothing to add.
                                continue;
                            }
                            else {
                                // Duplicate key with different value found: cleaning up filled values from hash-table. Then recreating.
                                keyDuplication = true;
                                myTable.removeRange(myEndPos, myTable.getEndPos());
                                break;
                            }
                        }
                        myTable.insertAtPos(~pos, key, entry.getValue());
                    }

                    if (!keyDuplication) {
                        if (myTable.getSize() == mySize) {
                            // All key/value pairs from the supplied map were the same as in current map
                            return this;
                        }
                        else {
                            return reuse(myTable.getSize(), myTable.getStartPos(), myTable.getEndPos());
                        }
                    }
                }
            }
        }

        return withAllRecreate(map);
    }

    private ImmutableLinkedHashMap<K, V> withAllRecreate(@Nonnull Map<? extends K, ? extends V> map) {
        ReusableLinkedHashtable<K, V> newTable = myTable.blankOfSize(mySize + map.size());
        myTable.forEach(
            this,
            (key, value) -> {
                if (!map.containsKey(key)) {
                    newTable.insert(key, value);
                }
            }
        );
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            K key = ensureKeyIsNotNull(entry.getKey());
            V value = entry.getValue();
            newTable.insert(key, value);
        }
        return of(newTable);
    }

    private static <K> K ensureKeyIsNotNull(K key) {
        if (key == null) {
            throwKeyIsNull();
        }
        return key;
    }

    private static void throwKeyIsNull() {
        throw new IllegalArgumentException("Null keys are not supported");
    }

    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void detachFromTable() {
        if (isMaster()) {
            return;
        }

        ReusableLinkedHashtable<K, V> newTable = myTable.copyRange(this);
        synchronized (myTable) {
            myTable = newTable;
            myStartPos = newTable.getStartPos();
            myEndPos = newTable.getEndPos();
        }
    }

    private SubCollectionsCache getSubCollectionCache() {
        SubCollectionsCache cache = mySubCollectionCache == null ? null : mySubCollectionCache.get();
        if (cache == null) {
            cache = new SubCollectionsCache();
            mySubCollectionCache = new WeakReference<>(cache);
        }
        return cache;
    }

    private abstract class MyIterator<E> implements Iterator<E> {
        protected int pos = -1;

        @Override
        public boolean hasNext() {
            return pos != myEndPos;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            pos = pos < 0 ? myStartPos : myTable.getPosAfter(pos);
            return get(pos);
        }

        protected abstract E get(int pos);
    }

    private class MyEntryIterator extends MyIterator<Entry<K, V>> {
        private class FlyweightEntry implements Entry<K, V> {
            @Override
            public K getKey() {
                return myTable.getKey(pos);
            }

            @Override
            @SuppressWarnings("unchecked")
            public V getValue() {
                return (V)myTable.getValue(pos);
            }

            @Override
            public V setValue(V value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int hashCode() {
                throw new UnsupportedOperationException();
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean equals(Object obj) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString() {
                return getKey() + "=" + getValue();
            }
        }

        private final FlyweightEntry entry = new FlyweightEntry();

        @Override
        protected Entry<K, V> get(int offset) {
            return entry;
        }
    }

    private class MyKeySet extends AbstractSet<K> {
        @Nonnull
        @Override
        public Iterator<K> iterator() {
            return new MyIterator<K>() {
                @Override
                protected K get(int pos) {
                    return myTable.getKey(pos);
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super K> action) {
            myTable.forEachKey(ImmutableLinkedHashMap.this, action);
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
            return new MyIterator<V>() {
                @Override
                @SuppressWarnings("unchecked")
                protected V get(int pos) {
                    return (V)myTable.getValue(pos);
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super V> action) {
            myTable.forEachValue(ImmutableLinkedHashMap.this, action);
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
            return new MyEntryIterator();
        }

        @Override
        public int size() {
            return ImmutableLinkedHashMap.this.size();
        }
    }

    private class SubCollectionsCache {
        private Set<K> myKeySet = null;
        private Collection<V> myValues = null;
        private Set<Entry<K, V>> myEntrySet = null;

        public Set<K> getKeySet() {
            if (myKeySet == null) {
                myKeySet = new MyKeySet();
            }
            return myKeySet;
        }

        public Collection<V> getValues() {
            if (myValues == null) {
                myValues = new MyValues();
            }
            return myValues;
        }

        public Set<Entry<K, V>> getEntrySet() {
            if (myEntrySet == null) {
                myEntrySet = new MyEntrySet();
            }
            return myEntrySet;
        }
    }
}