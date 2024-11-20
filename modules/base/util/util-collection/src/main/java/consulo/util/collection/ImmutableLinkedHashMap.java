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
public class ImmutableLinkedHashMap<K, V> implements Map<K, V> {
    private static final ImmutableLinkedHashMap<Object, Object> EMPTY =
        new ImmutableLinkedHashMap<>(HashingStrategy.canonical(), 0, ArrayUtil.EMPTY_OBJECT_ARRAY, ArrayUtil.EMPTY_INT_ARRAY, -1, -1);

    @Nonnull
    private final HashingStrategy<K> myStrategy;
    @Nonnull
    private final Object[] myData;
    private final int mySize, myStartPos, myEndPos;
    private final int[] myNextPos;
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
        return strategy == HashingStrategy.canonical()
            ? empty()
            : new ImmutableLinkedHashMap<>(strategy, 0, ArrayUtil.EMPTY_OBJECT_ARRAY, ArrayUtil.EMPTY_INT_ARRAY, -1, -1);
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
        if (map instanceof ImmutableLinkedHashMap ilhm && ilhm.myStrategy == strategy) {
            // Same strategy ImmutableLinkedHashMap. Reusing it.
            return (ImmutableLinkedHashMap<K, V>)ilhm;
        }
        else if (map.isEmpty()) {
            // Empty map optimization.
            return empty(strategy);
        }

        // Creating from scratch. Arrays are 33% full.
        int size = map.size();
        Object[] newData = new Object[(size << 2) + (size << 1)];
        int[] newNextPos = new int[(size << 1) + size];
        int newStartPos = -1, newEndPos = -1;
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            newEndPos = insert(strategy, newData, newNextPos, newEndPos, ensureKeyIsNotNull(entry.getKey()), entry.getValue());
            if (newStartPos < 0) {
                newStartPos = newEndPos;
            }
        }
        newNextPos[newEndPos >>> 1] = newStartPos;
        return new ImmutableLinkedHashMap<>(strategy, size, newData, newNextPos, newStartPos, newEndPos);
    }

    private ImmutableLinkedHashMap(
        @Nonnull HashingStrategy<K> strategy,
        int size,
        @Nonnull Object[] data,
        @Nonnull int[] nextPos,
        int startPos,
        int endPos
    ) {
        this.myStrategy = strategy;
        this.mySize = size;
        this.myData = data;
        this.myNextPos = nextPos;
        this.myStartPos = startPos;
        this.myEndPos = endPos;
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
        synchronized (myData) {
            keyPos = tablePos(myStrategy, myData, key);
            if (keyPos >= 0 && !isSafelyAppendable()) {
                checkUnsafePossibleHit = true;
            }
        }

        if (checkUnsafePossibleHit) {
            // We're not in the largest map sharing current arrays. So we need to check if found key is actually in our list.
            return isInList(keyPos) ? withoutRecreate(keyPos) : this;
        }

        if (keyPos < 0) {
            // Element is not found, return current map.
            return this;
        }
        else if (mySize == 1) {
            // Removing the last element in the map — return empty map.
            return empty(myStrategy);
        }
        else if (keyPos == myStartPos) {
            // Removing the first element in the list. Reuse arrays, just mark second element in the list as first.
            int newStartPos = myNextPos[myStartPos >>> 1];
            return new ImmutableLinkedHashMap<>(myStrategy, mySize - 1, myData, myNextPos, newStartPos, myEndPos);
        }
        else if (keyPos == myEndPos) {
            // Removing the last element in the list. Reuse arrays, just mark as last an element in the list before last.
            int newEndPos = myStartPos;
            while (true) {
                int nextPos = myNextPos[newEndPos >>> 1];
                if (nextPos == myEndPos) {
                    break;
                }
                newEndPos = nextPos;
            }
            return new ImmutableLinkedHashMap<>(myStrategy, mySize - 1, myData, myNextPos, myStartPos, newEndPos);
        }

        return withoutRecreate(keyPos);
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
        synchronized (myData) {
            keyPos = tablePos(myStrategy, myData, key);
            if (keyPos >= 0 && !isSafelyAppendable()) {
                checkUnsafePossibleDuplication = true;
            }
        }

        if (checkUnsafePossibleDuplication) {
            // We're not in the largest map sharing current arrays. So we need to check if found key is actually in our list.
            if (isInList(keyPos)) {
                if (myData[keyPos + 1] == value) {
                    // Value is the same. Reusing current map.
                    return this;
                }
                return withDuplicatedKey(keyPos, value);
            }
            return withNoDuplicationRecreate(key, value);
        }

        if (keyPos >= 0) {
            if (myData[keyPos + 1] == value) {
                // Value is the same. Reusing current map.
                return this;
            }
            else {
                return withDuplicatedKey(keyPos, value);
            }
        }

        return withNoDuplicationTryReusing(~keyPos, key, value);
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
     *         plus all the mappings of the supplied map.
     */
    @Nonnull
    public ImmutableLinkedHashMap<K, V> withAll(@Nonnull Map<? extends K, ? extends V> map) {
        if (isEmpty()) {
            // Optimization for empty current map.
            return fromMap(myStrategy, map);
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
        synchronized (myData) {
            keyPos = tablePos(myStrategy, myData, (K)key);
            if (keyPos < 0) {
                return false;
            }
            if (isSafelyAppendable()) {
                return true;
            }
        }
        // We're not in the largest map sharing current arrays. So we need to check if found key is actually in our list.
        return isInList(keyPos);
    }

    @Override
    public boolean containsValue(Object value) {
        if (mySize > 0) {
            for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
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
        synchronized (myData) {
            keyPos = tablePos(myStrategy, myData, (K)key);
            if (keyPos < 0) {
                return defaultValue;
            }
            if (isSafelyAppendable()) {
                return (V)myData[keyPos + 1];
            }
        }

        // We're not in the largest map sharing current arrays. So we need to check if found key is actually in our list.
        return isInList(keyPos) ? (V)myData[keyPos + 1] : defaultValue;
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
        if (mySize > 0) {
            for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                action.accept((K)myData[pos], (V)myData[pos + 1]);
                if (pos == endPos) {
                    break;
                }
            }
        }
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
        int h = 0;
        if (mySize > 0) {
            for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                h += myStrategy.hashCode((K)myData[pos]) ^ Objects.hashCode(myData[pos + 1]);
                if (pos == endPos) {
                    break;
                }
            }
        }
        return h;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (!(obj instanceof Map map && size() == map.size())) {
            return false;
        }
        if (mySize > 0) {
            for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                if (!Objects.equals(map.get(myData[pos]), myData[pos + 1])) {
                    return false;
                }
                if (pos == endPos) {
                    break;
                }
            }
        }
        return true;
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

    /**
     * <p>Checks if we can safely reuse current hash-table for appending new entries without arrays recreation.
     * This is only possible if no elements were added after the last element or before the first element of the list
     * (the last element points to the first element).</p>
     *
     * <p>Must be called from synchronized context!</p>
     *
     * @return {@code true} if we can safely reuse this hash-table for adding new elements. {@code false} if arrays need to be recreated.
     */
    private boolean isSafelyAppendable() {
        return myNextPos[myEndPos >>> 1] == myStartPos;
    }

    /**
     * <p>Upper size limit for current hash-table arrays. Set to 50% to prevent long hash collisions.
     * Arrays must be recreated after reaching this limit.</p>
     *
     * @return Current hash-table filling limit before forcing arrays recreation.
     */
    private int maxSafeSize() {
        return myNextPos.length >> 1;
    }

    private boolean isInList(int keyPos) {
        if (mySize > 0) {
            for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                if (keyPos == pos) {
                    return true;
                }
                if (pos == endPos) {
                    break;
                }
            }
        }
        return false;
    }

    private void cleanUpUnsuccessfulAppend(int endPos) {
        for (int cleaningPos = myEndPos, nextCleaningPos; cleaningPos != endPos; cleaningPos = nextCleaningPos) {
            nextCleaningPos = myNextPos[cleaningPos >>> 1];
            myData[nextCleaningPos] = null;
            myData[nextCleaningPos + 1] = null;
        }
        myNextPos[myEndPos >>> 1] = myStartPos;
    }

    @Contract(pure = true)
    @Nonnull
    @SuppressWarnings("unchecked")
    private ImmutableLinkedHashMap<K, V> withoutRecreate(int keyPos) {
        // Recreating arrays.
        Object[] newData = new Object[(mySize - 1) << 3];
        int[] newNextPos = new int[(mySize - 1) << 2];
        int newStartPos = -1, newEndPos = -1;
        for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
            if (pos != keyPos) {
                newEndPos = insert(myStrategy, newData, newNextPos, newEndPos, (K)myData[pos], myData[pos + 1]);
                if (newStartPos < 0) {
                    newStartPos = newEndPos;
                }
            }
            if (pos == endPos) {
                break;
            }
        }
        newNextPos[newEndPos >>> 1] = newStartPos;
        return new ImmutableLinkedHashMap<>(myStrategy, mySize - 1, newData, newNextPos, newStartPos, newEndPos);
    }

    private ImmutableLinkedHashMap<K, V> withDuplicatedKey(int keyPos, @Nullable V value) {
        Object[] newData = new Object[myData.length];
        int[] newNextPos = new int[myNextPos.length];
        for (int oldPos = myStartPos, endPos = myEndPos; ; oldPos = myNextPos[oldPos >>> 1]) {
            newData[oldPos] = myData[oldPos];
            newData[oldPos + 1] = myData[oldPos + 1];
            newNextPos[oldPos >>> 1] = myNextPos[oldPos >>> 1];
            if (oldPos == endPos) {
                break;
            }
        }
        newData[keyPos + 1] = value;
        int newStartPos = myStartPos, newEndPos = myEndPos;
        //noinspection StatementWithEmptyBody
        if (mySize == 1 || newEndPos == keyPos) {
            // Single-entry map or key hit was at the end of the list. No need to change linked list.
        }
        else if (newStartPos == keyPos) {
            // Start mark points to the key hit. New entry is added to the end of the list, so shift the start of the list.
            newStartPos = newNextPos[newStartPos >>> 1];
            newNextPos[newEndPos >>> 1] = keyPos;
            newEndPos = keyPos;
        }
        else {
            // Find previous entry in the list to remove link to the duplicated key. Move this entry to the end of the list.
            int prevPos = newStartPos;
            while (true) {
                int followingPos = newNextPos[prevPos >>> 1];
                if (followingPos == keyPos) {
                    newNextPos[prevPos >>> 1] = newNextPos[followingPos >>> 1];
                    break;
                }
                prevPos = followingPos;
            }
            newNextPos[newEndPos >>> 1] = keyPos;
            newEndPos = keyPos;
        }
        return new ImmutableLinkedHashMap<>(myStrategy, mySize, newData, newNextPos, newStartPos, newEndPos);
    }

    @SuppressWarnings("unchecked")
    private ImmutableLinkedHashMap<K, V> withNoDuplicationTryReusing(int keyPos, @Nonnull K key, @Nullable V value) {
        int newSize = mySize + 1;
        if (newSize < maxSafeSize()) {
            synchronized (myData) {
                if (isSafelyAppendable()) {
                    // Arrays are less than 50% full. Reusing arrays.
                    myData[keyPos] = key;
                    myData[keyPos + 1] = value;
                    myNextPos[myEndPos >>> 1] = keyPos;
                    myNextPos[keyPos >>> 1] = myStartPos;
                    return new ImmutableLinkedHashMap<>(myStrategy, newSize, myData, myNextPos, myStartPos, keyPos);
                }
            }
        }

        return withNoDuplicationRecreate(key, value);
    }

    @SuppressWarnings("unchecked")
    private ImmutableLinkedHashMap<K, V> withNoDuplicationRecreate(@Nonnull K key, @Nullable V value) {
        // Recreating arrays. 25% full.
        int newSize = mySize + 1;
        Object[] newData = new Object[newSize << 3];
        int[] newNextPos = new int[newSize << 2];
        int newStartPos = -1, newEndPos = -1;
        if (mySize > 0) {
            for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                newEndPos = insert(myStrategy, newData, newNextPos, newEndPos, (K)myData[pos], myData[pos + 1]);
                if (newStartPos < 0) {
                    newStartPos = newEndPos;
                }
                if (pos == endPos) {
                    break;
                }
            }
        }
        newEndPos = insert(myStrategy, newData, newNextPos, newEndPos, key, value);
        if (newStartPos < 0) {
            newStartPos = newEndPos;
        }
        newNextPos[newEndPos >>> 1] = newStartPos;
        return new ImmutableLinkedHashMap<>(myStrategy, newSize, newData, newNextPos, newStartPos, newEndPos);
    }

    private ImmutableLinkedHashMap<K, V> withAllTryReusing(@Nonnull Map<? extends K, ? extends V> map) {
        int newSize = mySize + map.size();
        if (newSize < maxSafeSize()) {
            synchronized (myData) {
                if (isSafelyAppendable()) {
                    // Arrays are less than 50% full and we're in the largest map using current arrays. Reusing arrays.
                    int newEndPos = myEndPos;
                    boolean keyDuplication = false;
                    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
                        K key = entry.getKey();
                        if (key == null) {
                            // Null key is encountered during appending values to arrays. Cleaning up and throwing exception.
                            cleanUpUnsuccessfulAppend(newEndPos);
                            throwKeyIsNull();
                        }
                        int pos = tablePos(myStrategy, myData, key);
                        if (pos >= 0) {
                            if (myData[pos + 1] != entry.getValue()) {
                                // Value is the same. Nothing to add.
                                newSize--;
                                continue;
                            }
                            else {
                                // Duplicated key with different value found: cleaning up filled values from arrays. Then creating new arrays.
                                keyDuplication = true;
                                cleanUpUnsuccessfulAppend(newEndPos);
                                break;
                            }
                        }
                        pos = ~pos;
                        myData[pos] = key;
                        myData[pos + 1] = entry.getValue();
                        myNextPos[newEndPos >>> 1] = pos;
                        newEndPos = pos;
                    }

                    if (!keyDuplication) {
                        if (newSize == mySize) {
                            // All key/value pairs from the supplied map were the same as in current map
                            return this;
                        }
                        else {
                            return new ImmutableLinkedHashMap<>(myStrategy, newSize, myData, myNextPos, myStartPos, newEndPos);
                        }
                    }
                }
            }
        }

        return withAllRecreate(map);
    }

    private ImmutableLinkedHashMap<K, V> withAllRecreate(@Nonnull Map<? extends K, ? extends V> map) {
        int newSize = mySize + map.size();
        Object[] newData = new Object[newSize << 3];
        int[] newNextPos = new int[newSize << 2];
        int newStartPos = -1, newEndPos = -1;
        if (mySize > 0) {
            for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                @SuppressWarnings("unchecked")
                K key = (K)myData[pos];
                if (map.containsKey(key)) {
                    newSize--;
                }
                else {
                    newEndPos = insert(myStrategy, newData, newNextPos, newEndPos, key, myData[pos + 1]);
                    if (newStartPos < 0) {
                        newStartPos = newEndPos;
                    }
                }
                if (pos == endPos) {
                    break;
                }
            }
        }
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            K key = ensureKeyIsNotNull(entry.getKey());
            V value = entry.getValue();
            newEndPos = insert(myStrategy, newData, newNextPos, newEndPos, key, value);
            if (newStartPos < 0) {
                newStartPos = newEndPos;
            }
        }
        newNextPos[newEndPos >>> 1] = newStartPos;
        return new ImmutableLinkedHashMap<>(myStrategy, newSize, newData, newNextPos, newStartPos, newEndPos);
    }

    /**
     * Must be called only from synchronized block or during arrays recreation!
     */
    @SuppressWarnings("unchecked")
    private static <K> int tablePos(HashingStrategy<K> strategy, Object[] data, K key) {
        int length = data.length;
        if (length == 0) {
            return -1;
        }

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
    private static <K> int insert(HashingStrategy<K> strategy, Object[] data, int[] nextPos, int prevPos, K key, Object value) {
        int insertPos = tablePos(strategy, data, key);
        insertPos = ~insertPos;
        assert insertPos >= 0;
        data[insertPos] = key;
        data[insertPos + 1] = value;
        if (prevPos >= 0) {
            nextPos[prevPos >>> 1] = insertPos;
        }
        return insertPos;
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
            pos = pos < 0 ? myStartPos : myNextPos[pos >>> 1];
            return get(pos);
        }

        protected abstract E get(int offset);
    }

    private class MyEntryIterator extends MyIterator<Entry<K, V>> {
        private class FlyweightEntry implements Entry<K, V> {
            @Override
            @SuppressWarnings("unchecked")
            public K getKey() {
                return (K)myData[pos];
            }

            @Override
            @SuppressWarnings("unchecked")
            public V getValue() {
                return (V)myData[pos + 1];
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
                @SuppressWarnings("unchecked")
                protected K get(int offset) {
                    return (K)myData[offset];
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super K> action) {
            if (mySize > 0) {
                for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                    action.accept((K)myData[pos]);
                    if (pos == endPos) {
                        break;
                    }
                }
            }
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
                protected V get(int offset) {
                    return (V)myData[offset + 1];
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEach(Consumer<? super V> action) {
            if (mySize > 0) {
                for (int pos = myStartPos, endPos = myEndPos; ; pos = myNextPos[pos >>> 1]) {
                    action.accept((V)myData[pos + 1]);
                    if (pos == endPos) {
                        break;
                    }
                }
            }
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
