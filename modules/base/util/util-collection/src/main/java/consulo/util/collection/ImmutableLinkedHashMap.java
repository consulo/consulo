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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>An immutable ordered hash map optimized for frequent adding of new keys. Custom equals/hashCode strategy is supported.
 * Null keys are prohibited. Null values are allowed.</p>
 *
 * <p>Keys and values are both stored in an open-addressed hash-table. All map entries are connected in a singly linked list.
 * The start and the end of the list are marked. So when adding new element we can reuse arrays, just mark new element as the last.
 * This won't disturb previously created maps referencing same arrays.</p>
 *
 * <p>Initially map is created 25% full and grows up to 50% full (to prevent degradation due to cache collisions),
 * after that arrays are recreated. So array recreation happens only log<sub>2</sub>(N) times during map growth.</p>
 *
 * <p>The last entry of the list references the first entry. So if the last marked entry isn't referencing the first marked entry we know
 * that we cannot safely reuse arrays (because arrays were already reused by another {@code ImmutableLinkedHashMap} instance).</p>
 *
 * @author UNV
 * @since 2024-11-18
 */
public class ImmutableLinkedHashMap<K, V> implements Map<K, V>, ListRange {
    private static final ImmutableLinkedHashMap<Object, Object> EMPTY = ReusableArray.empty().toMap();

    @Nonnull
    private final ReusableArray<K, V> myArray;
    private final int mySize, myStartPos, myEndPos;
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
        return strategy == HashingStrategy.canonical() ? empty() : ReusableArray.<K, V>empty(strategy).toMap();
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
     * <p>Created map is 33% full, not 25% full as during arrays recreation.
     * One of the use-cases is that this map wouldn't grow at all so we don't want to waste memory.</p>
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
        if (map instanceof ImmutableLinkedHashMap ilhm && ilhm.myArray.myStrategy == strategy) {
            // Same strategy ImmutableLinkedHashMap. Reusing it.
            return (ImmutableLinkedHashMap<K, V>)ilhm;
        }
        else if (map.isEmpty()) {
            // Empty map optimization.
            return empty(strategy);
        }

        ReusableArray<K, V> newArray = ReusableArray.blankOfSize(strategy, map.size());
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            newArray.insert(ensureKeyIsNotNull(entry.getKey()), entry.getValue());
        }
        return newArray.toMap();
    }

    /**
     * Returns an {@code ImmutableLinkedHashMap} which contains all the entries of the current map except the supplied key.
     *
     * @param key a key to exclude from the result
     * @return an {@code ImmutableLinkedHashMap} which contains all the entries of the current map except the supplied key
     */
    @Contract(pure = true)
    @Nonnull
    public ImmutableLinkedHashMap<K, V> without(@Nonnull K key) {
        if (mySize == 0) {
            return this;
        }

        int keyPos;
        boolean checkUnsafePossibleHit = false;
        synchronized (myArray) {
            keyPos = myArray.getPos(key);
            if (keyPos >= 0 && !isMainArrayHolder()) {
                checkUnsafePossibleHit = true;
            }
        }

        if (checkUnsafePossibleHit) {
            // We're not in the largest map sharing current arrays. So we need to check if found key is actually in our list.
            return myArray.isInList(this, keyPos) ? myArray.copyRangeWithoutPos(this, keyPos).toMap() : this;
        }

        if (keyPos < 0) {
            // Element is not found, return current map.
            return this;
        }
        else if (mySize == 1) {
            // Removing the last element in the map — return empty map.
            return empty(myArray.myStrategy);
        }
        else if (keyPos == myStartPos) {
            // Removing the first element in the list. Reuse arrays, just mark second element in the list as first.
            return reuse(mySize - 1, myArray.getPosAfter(myStartPos), myEndPos);
        }
        else if (keyPos == myEndPos) {
            // Removing the last element in the list. Reuse arrays, just mark as last an element in the list before last.
            return reuse(mySize - 1, myStartPos, myArray.getPosBefore(this, myEndPos));
        }

        return myArray.copyRangeWithoutPos(this, keyPos).toMap();
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
    public ImmutableLinkedHashMap<K, V> with(@Nonnull K key, @Nullable V value) {
        if (mySize == 0) {
            return fromMap(Collections.singletonMap(key, value));
        }

        int keyPos;
        boolean checkUnsafePossibleDuplication = false;
        synchronized (myArray) {
            keyPos = myArray.getPos(key);
            if (keyPos >= 0 && !isMainArrayHolder()) {
                checkUnsafePossibleDuplication = true;
            }
        }

        if (checkUnsafePossibleDuplication) {
            // We're not in the largest map sharing current arrays. So we need to check if found key is actually in our list.
            if (myArray.isInList(this, keyPos)) {
                if (myArray.getValue(keyPos) == value) {
                    // Value is the same. Reusing current map.
                    return this;
                }
                return myArray.copyRange(this).setValueAtPos(keyPos, value).toMap();
            }
            return myArray.copyRange(this).insertAtPos(keyPos, key, value).toMap();
        }

        if (keyPos >= 0) {
            if (myArray.getValue(keyPos) == value) {
                // Value is the same. Reusing current map.
                return this;
            }
            // Different value of the same key. Copy arrays and replace value.
            return myArray.copyRange(this).setValueAtPos(keyPos, value).toMap();
        }

        if (mySize + 1 < myArray.getMaxSafeSize()) {
            synchronized (myArray) {
                if (isMainArrayHolder()) {
                    // Arrays are less than 50% full. Reusing arrays.
                    return myArray.insertAtPos(~keyPos, key, value).toMap();
                }
            }
        }

        // Recreating arrays.
        return myArray.copyOfSize(mySize + 1).insert(key, value).toMap();
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
            return fromMap(myArray.myStrategy, map);
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
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        int keyPos;
        synchronized (myArray) {
            keyPos = myArray.getPos((K)key);
            if (keyPos < 0) {
                return false;
            }
            if (isMainArrayHolder()) {
                return true;
            }
        }
        // We're not in the largest map sharing current arrays. So we need to check if found key is actually in our list.
        return myArray.isInList(this, keyPos);
    }

    @Override
    public boolean containsValue(Object value) {
        return myArray.isValueInList(this, value);
    }

    @Override
    public V get(Object key) {
        return getOrDefault(key, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V defaultValue) {
        if (key == null || mySize == 0) {
            return defaultValue;
        }
        int keyPos;
        synchronized (myArray) {
            keyPos = myArray.getPos((K)key);
            if (keyPos < 0) {
                return defaultValue;
            }
            if (isMainArrayHolder()) {
                return (V)myArray.getValue(keyPos);
            }
        }

        // We're not in the largest map sharing current arrays. So we need to check if found key is actually in our list.
        return myArray.isInList(this, keyPos) ? (V)myArray.getValue(keyPos) : defaultValue;
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
        myArray.forEach(this, action);
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
        int[] h = new int[]{0};
        forEach((key, value) -> h[0] += myArray.myStrategy.hashCode(key) ^ Objects.hashCode(value));
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

    protected ImmutableLinkedHashMap(@Nonnull ReusableArray<K, V> array) {
        this(array, array.getSize(), array.getStartPos(), array.getEndPos());
    }

    protected ImmutableLinkedHashMap(@Nonnull ReusableArray<K, V> array, int size, int startPos, int endPos) {
        this.myArray = array;
        this.mySize = size;
        this.myStartPos = startPos;
        this.myEndPos = endPos;
    }

    private ImmutableLinkedHashMap<K, V> reuse(int size, int startPos, int endPos) {
        ImmutableLinkedHashMap<K, V> map = new ImmutableLinkedHashMap<>(myArray, size, startPos, endPos);
        myArray.link(map.isMainArrayHolder() ? this : map);
        return map;
    }

    /**
     * <p>Checks if we can safely reuse current hash-table for appending new entries without arrays recreation.
     * This is only possible if no elements were added after the last element or before the first element of the list
     * (the last element points to the first element).</p>
     *
     * <p>Must be called from synchronized context!</p>
     *
     * @return {@code true} if we can safely reuse this hash-table for adding new elements. {@code false} if arrays need to be recreated.
     */
    private boolean isMainArrayHolder() {
        return mySize == myArray.getSize();
    }

    protected boolean optimize() {
        if (isMainArrayHolder()) {
            return false;
        }

        // TODO: optimize

        return false;
    }

    private static class ReusableArray<K, V> implements ListRange {
        @Nonnull
        protected static final ReferenceQueue<ImmutableLinkedHashMap> QUEUE = new ReferenceQueue<>();

        protected static final ReusableArray<Object, Object> EMPTY =
            new ReusableArray<>(HashingStrategy.canonical(), ArrayUtil.EMPTY_OBJECT_ARRAY, ArrayUtil.EMPTY_INT_ARRAY, 0);

        @Nonnull
        protected final HashingStrategy<K> myStrategy;
        @Nonnull
        protected final Object[] myData;
        @Nonnull
        protected final int[] myNextPos;
        private int mySize, myEndPos = -1;

        private MapLink myMapLink = null;

        private ReusableArray(@Nonnull HashingStrategy<K> strategy, @Nonnull Object[] data, @Nonnull int[] nextPos, int size) {
            myStrategy = strategy;
            myData = data;
            myNextPos = nextPos;
            mySize = size;
        }

        protected ImmutableLinkedHashMap<K, V> toMap() {
            return new ImmutableLinkedHashMap<>(this);
        }

        @Nonnull
        @SuppressWarnings("unchecked")
        protected static <K, V> ReusableArray<K, V> empty() {
            return (ReusableArray<K, V>)EMPTY;
        }

        @Nonnull
        protected static <K, V> ReusableArray<K, V> empty(HashingStrategy<K> strategy) {
            return strategy == HashingStrategy.canonical()
                ? empty()
                : new ReusableArray<>(strategy, ArrayUtil.EMPTY_OBJECT_ARRAY, ArrayUtil.EMPTY_INT_ARRAY, 0);
        }

        protected static <K, V> ReusableArray<K, V> blankOfSize(HashingStrategy<K> strategy, int size) {
            return size == 0 ? empty(strategy) : new ReusableArray<>(strategy, new Object[size << 3], new int[size << 2], 0);
        }

        protected ReusableArray<K, V> blankOfSize(int size) {
            return blankOfSize(myStrategy, size);
        }

        @SuppressWarnings("unchecked")
        protected ReusableArray<K, V> copyOfSize(int size) {
            ReusableArray<K, V> newArray = blankOfSize(size);
            if (mySize > 0) {
                for (int pos = getStartPos(), endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                    newArray.insert((K)myData[pos], (V)myData[pos + 1]);
                    if (pos == endPos) {
                        break;
                    }
                }
            }
            return newArray;
        }

        protected ReusableArray<K, V> copyRange(ListRange range) {
            return copyRangeWithoutPos(range, -1);
        }

        protected ReusableArray<K, V> copyRangeWithoutPos(ListRange range, int excludePos) {
            int startPos = range.getStartPos(), endPos = range.getEndPos();
            ReusableArray<K, V> newArray = new ReusableArray<>(myStrategy, new Object[myData.length], new int[myNextPos.length], 0);
            int newSize = 0, newEndPos = endPos;
            for (int pos = startPos; ; pos = myNextPos[pos >>> 1]) {
                if (pos != excludePos) {
                    newArray.myData[pos] = myData[pos];
                    newArray.myData[pos + 1] = myData[pos + 1];
                    newArray.myNextPos[newEndPos >>> 1] = pos;
                    newEndPos = pos;
                    newSize++;
                }
                if (pos == endPos) {
                    break;
                }
            }
            newArray.mySize = newSize;
            newArray.myEndPos = newEndPos;
            return newArray;
        }

        protected int getSize() {
            return mySize;
        }

        @Override
        public int getStartPos() {
            int endPos = this.myEndPos;
            return endPos >= 0 ? this.myNextPos[endPos >>> 1] : -1;
        }

        @Override
        public int getEndPos() {
            return myEndPos;
        }

        /**
         * <p>Upper size limit for current hash-table arrays. Set to 50% to prevent long hash collisions.
         * Arrays must be recreated after reaching this limit.</p>
         *
         * @return Current hash-table filling limit before forcing arrays recreation.
         */
        protected int getMaxSafeSize() {
            return myNextPos.length >> 1;
        }

        /**
         * Must be called only from synchronized block or during arrays recreation!
         */
        @SuppressWarnings("unchecked")
        protected int getPos(K key) {
            Object[] data = this.myData;
            int length = data.length;
            if (length == 0) {
                return -1;
            }

            HashingStrategy<K> strategy = this.myStrategy;
            int pos = Math.floorMod(strategy.hashCode(key), length >>> 1) << 1;
            while (true) {
                K candidate = (K)data[pos];
                if (candidate == null) {
                    return ~pos;
                }
                else if (strategy.equals(candidate, key)) {
                    return pos;
                }
                pos += 2;
                if (pos == length) {
                    pos = 0;
                }
            }
        }

        /**
         * Must be called only from arrays recreation!
         */
        protected ReusableArray<K, V> insert(K key, V value) {
            return insertAtPos(~getPos(key), key, value);
        }

        /**
         * Must be called only from arrays recreation!
         */
        protected ReusableArray<K, V> insertAtPos(int insertPos, K key, V value) {
            myData[insertPos] = key;
            myData[insertPos + 1] = value;
            int endPosShifted = myEndPos >> 1;
            if (endPosShifted >= 0) {
                int startPos = myNextPos[endPosShifted];
                myNextPos[endPosShifted] = insertPos;
                myNextPos[insertPos >>> 1] = startPos;
            }
            else {
                myNextPos[insertPos >>> 1] = insertPos;
            }
            myEndPos = insertPos;
            mySize++;
            return this;
        }

        /**
         * <p>Sets entry value and moves the entry to the end of the list.</p>
         *
         * <p>Must be called only from arrays recreation!</p>
         */
        protected ReusableArray<K, V> setValueAtPos(int keyPos, V value) {
            myData[keyPos + 1] = value;

            if (mySize == 1 || myEndPos == keyPos) {
                // Single-entry map or key hit was at the end of the list. No need to change linked list.
                return this;
            }

            int startPos = getStartPos();
            if (keyPos != startPos) {
                // Find previous entry in the list to remove link to the duplicated key. Move this entry to the end of the list.
                int prevPos = startPos;
                while (true) {
                    int pos = myNextPos[prevPos >>> 1];
                    if (pos == keyPos) {
                        myNextPos[prevPos >>> 1] = myNextPos[pos >>> 1];
                        break;
                    }
                    prevPos = pos;
                }
                myNextPos[myEndPos >>> 1] = keyPos;
                myNextPos[keyPos >>> 1] = startPos;
            }
            myEndPos = keyPos;

            return this;
        }

        protected void removeRange(int startPosExcluding, int endPosIncluding) {
            for (int cleaningPos = startPosExcluding, nextCleaningPos; cleaningPos != endPosIncluding; cleaningPos = nextCleaningPos) {
                nextCleaningPos = myNextPos[cleaningPos >>> 1];
                myData[nextCleaningPos] = null;
                myData[nextCleaningPos + 1] = null;
                mySize--;
            }
            myNextPos[startPosExcluding >>> 1] = myNextPos[endPosIncluding >>> 1];
        }

        public boolean isInList(ListRange range, int searchPos) {
            int startPos = range.getStartPos();
            if (startPos >= 0) {
                for (int pos = startPos, endPos = range.getEndPos(); ; pos = myNextPos[pos >>> 1]) {
                    if (searchPos == pos) {
                        return true;
                    }
                    if (pos == endPos) {
                        break;
                    }
                }
            }
            return false;
        }

        public boolean isValueInList(ListRange range, Object value) {
            int startPos = range.getStartPos();
            if (startPos >= 0) {
                for (int pos = startPos, endPos = range.getEndPos(); ; pos = myNextPos[pos >>> 1]) {
                    if (myData[pos] != null && Objects.equals(myData[pos + 1], value)) {
                        return true;
                    }
                    if (pos == endPos) {
                        break;
                    }
                }
            }
            return false;
        }

        protected synchronized void link(ImmutableLinkedHashMap map) {
            myMapLink = new MapLink(map, this.myMapLink);
        }

        protected synchronized void unlink(ImmutableLinkedHashMap map) {
            if (myMapLink == null) {
                return;
            }

            if (myMapLink.refersTo(map)) {
                myMapLink = myMapLink.myNext;
                return;
            }

            for (MapLink mapLink = myMapLink, nextMapLink; ; mapLink = nextMapLink) {
                nextMapLink = mapLink.myNext;
                if (nextMapLink == null) {
                    break;
                }
                else if (nextMapLink.refersTo(map)) {
                    mapLink.myNext = nextMapLink.myNext;
                    break;
                }
            }
        }

        protected synchronized void optimize() {
            while (myMapLink != null && (myMapLink.refersTo(null) || myMapLink.optimize())) {
                myMapLink = myMapLink.myNext;
            }

            for (MapLink mapLink = myMapLink, nextMapLink; mapLink != null; mapLink = nextMapLink) {
                nextMapLink = mapLink.myNext;

                while (nextMapLink != null && (nextMapLink.refersTo(null) || myMapLink.optimize())) {
                    nextMapLink = nextMapLink.myNext;
                    mapLink.myNext = nextMapLink;
                }
            }
        }

        @SuppressWarnings("unchecked")
        public K getKey(int keyPos) {
            return (K)myData[keyPos];
        }

        public Object getValue(int keyPos) {
            return myData[keyPos + 1];
        }

        public int getPosBefore(ListRange range, int targetPos) {
            int newEndPos = range.getStartPos();
            while (true) {
                int nextPos = myNextPos[newEndPos >>> 1];
                if (nextPos == targetPos) {
                    return newEndPos;
                }
                newEndPos = nextPos;
            }
        }

        public int getPosAfter(int targetPos) {
            return myNextPos[targetPos >>> 1];
        }

        @SuppressWarnings("unchecked")
        public void forEach(ListRange range, BiConsumer<? super K, ? super V> action) {
            if (mySize > 0) {
                for (int pos = range.getStartPos(), endPos = range.getEndPos(); ; pos = myNextPos[pos >>> 1]) {
                    action.accept((K)myData[pos], (V)myData[pos + 1]);
                    if (pos == endPos) {
                        break;
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void forEachKey(ListRange range, Consumer<? super K> action) {
            if (mySize > 0) {
                for (int pos = range.getStartPos(), endPos = range.getEndPos(); ; pos = myNextPos[pos >>> 1]) {
                    action.accept((K)myData[pos]);
                    if (pos == endPos) {
                        break;
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        public <V> void forEachValue(ListRange range, Consumer<? super V> action) {
            if (mySize > 0) {
                for (int pos = range.getStartPos(), endPos = range.getEndPos(); ; pos = myNextPos[pos >>> 1]) {
                    action.accept((V)myData[pos + 1]);
                    if (pos == endPos) {
                        break;
                    }
                }
            }
        }

        private class MapLink extends WeakReference<ImmutableLinkedHashMap> {
            protected MapLink myNext;

            protected MapLink(ImmutableLinkedHashMap referent, MapLink next) {
                super(referent, QUEUE);
                myNext = next;
            }

            protected ReusableArray getArrays() {
                return ReusableArray.this;
            }

            protected boolean optimize() {
                ImmutableLinkedHashMap map = get();
                return map == null || map.optimize();
            }
        }

        @SuppressWarnings("InfiniteLoopStatement")
        private static final Thread OPTIMIZER = new Thread(() -> {
            while (true) {
                try {
                    ReusableArray.MapLink mapLink = (ReusableArray.MapLink)QUEUE.remove();
                    mapLink.getArrays().optimize();
                }
                catch (InterruptedException ignore) {
                }
            }
        });

        static {
            OPTIMIZER.setDaemon(true);
            OPTIMIZER.start();
        }
    }

    private ImmutableLinkedHashMap<K, V> withAllTryReusing(@Nonnull Map<? extends K, ? extends V> map) {
        if (mySize + map.size() < myArray.getMaxSafeSize()) {
            synchronized (myArray) {
                if (isMainArrayHolder()) {
                    // Arrays are less than 50% full and we're in the largest map using current arrays. Reusing arrays.
                    boolean keyDuplication = false;
                    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
                        K key = entry.getKey();
                        if (key == null) {
                            // Null key is encountered during appending values to arrays. Cleaning up and throwing exception.
                            myArray.removeRange(myEndPos, myArray.getEndPos());
                            throwKeyIsNull();
                        }
                        int pos = myArray.getPos(key);
                        if (pos >= 0) {
                            if (myArray.getValue(pos) == entry.getValue()) {
                                // Value is the same. Nothing to add.
                                continue;
                            }
                            else {
                                // Duplicated key with different value found: cleaning up filled values from arrays. Then recreating.
                                keyDuplication = true;
                                myArray.removeRange(myEndPos, myArray.getEndPos());
                                break;
                            }
                        }
                        myArray.insertAtPos(~pos, key, entry.getValue());
                    }

                    if (!keyDuplication) {
                        if (myArray.getSize() == mySize) {
                            // All key/value pairs from the supplied map were the same as in current map
                            return this;
                        }
                        else {
                            return reuse(myArray.getSize(), myArray.getStartPos(), myArray.getEndPos());
                        }
                    }
                }
            }
        }

        return withAllRecreate(map);
    }

    private ImmutableLinkedHashMap<K, V> withAllRecreate(@Nonnull Map<? extends K, ? extends V> map) {
        ReusableArray<K, V> newArray = myArray.blankOfSize(mySize + map.size());
        myArray.forEach(
            this,
            (key, value) -> {
                if (!map.containsKey(key)) {
                    newArray.insert(key, value);
                }
            }
        );
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            K key = ensureKeyIsNotNull(entry.getKey());
            V value = entry.getValue();
            newArray.insert(key, value);
        }
        return newArray.toMap();
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
            pos = pos < 0 ? myStartPos : myArray.getPosAfter(pos);
            return get(pos);
        }

        protected abstract E get(int pos);
    }

    private class MyEntryIterator extends MyIterator<Entry<K, V>> {
        private class FlyweightEntry implements Entry<K, V> {
            @Override
            public K getKey() {
                return myArray.getKey(pos);
            }

            @Override
            @SuppressWarnings("unchecked")
            public V getValue() {
                return (V)myArray.getValue(pos);
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
                    return myArray.getKey(pos);
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super K> action) {
            myArray.forEachKey(ImmutableLinkedHashMap.this, action);
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
                    return (V)myArray.getValue(pos);
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super V> action) {
            myArray.forEachValue(ImmutableLinkedHashMap.this, action);
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

interface ListRange {
    int getStartPos();

    int getEndPos();
}