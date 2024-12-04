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
package consulo.util.collection.impl.map;

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.ImmutableLinkedHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * <p>Utility class which stores key/value pairs in open-addressed hash-table with linear probing and links them in a singly linked list
 * in the insertion order. So during iteration these key/value entries may be returned in the same order they were inserted.
 * Custom equals/hashCode strategy is supported.</p>
 *
 * <p>Hash-table is created 25%-50% full (hash-table size is power of 2 for efficiency) and grows up to 50% full, then it needs
 * to be recreated. Filling open-addressed hash-table above 50% limit may create long hash collisions and is undesirable. Linked list
 * of entries is wrapped, the last entry references the first entry in the list, so only the end list position is stored. Position
 * is an index of a key in the data array (and in position/hash array as well). Each entry stores its hashCode and it is used during
 * hash-table resize. Also during resize we don't compare keys by .equals() but only by ==. This prevents calling any methods
 * on key objects which could produce exceptions during resize (which is undesirable since resize may occur asynchronously after GC).</p>
 *
 * <p>This class is intended to work in pair with {@link ImmutableLinkedHashMap}. There may be several {@code ImmutableLinkedHashMap}s
 * pointing to the same {@code ReusableLinkedHashtable}. Only one of {@code ImmutableLinkedHashMap}s would have all the key/value
 * entries of the whole hash-table. This map is called master-map. Other maps are sattelites.</p>
 *
 * <p>If master-map is garbage-collected but some of sattelite maps remain in memory, some of the key/value entries become unreachable
 * but still referenced from hash-table. This creates memory leak and one of this hash-table purposes is to resolve such leaks.
 * While creating hashtable Range map must provide an instance implementing {@link ReusableLinkedHashtableUser} interface. This instance
 * must be able to detach from the hash-table via creating new one containing only the key/value entries owned by the map.</p>
 *
 * <p>Links to maps using this hash-table are stored in {@code WeakReference}s, so maps are free to be garbage-collected.
 * But when GC collects such a map it notifies hash-table via thread which reads {@link ReferenceQueue} connected to {@link WeakReference}.
 * So hash-table can check if master-map referencing it is still present or the master-map was garbage-collected.
 * If there's no master-map present, hash-table detaches all the remaining sattelite maps preventing memory leaks.</p>
 *
 * @author UNV
 * @see ImmutableLinkedHashMap
 * @see HashingStrategy
 * @since 2024-11-20
 */
public class ReusableLinkedHashtable<K, V> implements ReusableLinkedHashtableRange {
    @Nonnull
    protected static final ReferenceQueue<ReusableLinkedHashtableUser> QUEUE = new ReferenceQueue<>();

    protected static final ReusableLinkedHashtable<Object, Object> EMPTY =
        new ReusableLinkedHashtable<>(HashingStrategy.canonical(), ArrayUtil.EMPTY_OBJECT_ARRAY, ArrayUtil.EMPTY_INT_ARRAY, 0);

    @Nonnull
    protected final HashingStrategy<K> myStrategy;
    @Nonnull
    protected final Object[] myData;
    @Nonnull
    protected final int[] myNextPosAndHash;
    private int mySize, myEndPos = -1;

    private Range myLargestRange = null;

    protected abstract class MyIterator<E> implements Iterator<E> {
        protected final int myStartPos, myEndPos;
        protected int pos = -1;

        protected MyIterator(ReusableLinkedHashtableRange range) {
            myStartPos = range.getStartPos();
            myEndPos = range.getEndPos();
        }

        @Override
        public boolean hasNext() {
            return pos != myEndPos;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            pos = pos < 0 ? myStartPos : myNextPosAndHash[pos];
            return get(pos);
        }

        protected abstract E get(int pos);
    }

    public class KeyIterator extends MyIterator<K> {
        public KeyIterator(ReusableLinkedHashtableRange range) {
            super(range);
        }

        @Override
        protected K get(int pos) {
            return ReusableLinkedHashtable.this.getKey(pos);
        }
    }

    public class ValueIterator extends MyIterator<V> {
        public ValueIterator(ReusableLinkedHashtableRange range) {
            super(range);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected V get(int pos) {
            return (V)ReusableLinkedHashtable.this.getValue(pos);
        }
    }

    public class EntryIterator extends MyIterator<Map.Entry<K, V>> {
        private class FlyweightEntry implements Map.Entry<K, V> {
            @Override
            public K getKey() {
                return ReusableLinkedHashtable.this.getKey(pos);
            }

            @Override
            @SuppressWarnings("unchecked")
            public V getValue() {
                return (V)ReusableLinkedHashtable.this.getValue(pos);
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

        public EntryIterator(ReusableLinkedHashtableRange range) {
            super(range);
        }

        @Override
        protected Map.Entry<K, V> get(int offset) {
            return entry;
        }
    }

    private ReusableLinkedHashtable(@Nonnull HashingStrategy<K> strategy, @Nonnull Object[] data, @Nonnull int[] nextPosAndHash, int size) {
        myStrategy = strategy;
        myData = data;
        myNextPosAndHash = nextPosAndHash;
        mySize = size;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <K, V> ReusableLinkedHashtable<K, V> empty() {
        return (ReusableLinkedHashtable<K, V>)EMPTY;
    }

    @Nonnull
    public static <K, V> ReusableLinkedHashtable<K, V> empty(HashingStrategy<K> strategy) {
        return strategy == HashingStrategy.canonical()
            ? empty()
            : new ReusableLinkedHashtable<>(strategy, ArrayUtil.EMPTY_OBJECT_ARRAY, ArrayUtil.EMPTY_INT_ARRAY, 0);
    }

    public static <K, V> ReusableLinkedHashtable<K, V> blankOfSize(HashingStrategy<K> strategy, int size) {
        if (size == 0) {
            return empty(strategy);
        }

        int arraySize = ceilToPowerOf2(size) << 2;
        return new ReusableLinkedHashtable<>(strategy, new Object[arraySize], new int[arraySize], 0);
    }

    public ReusableLinkedHashtable<K, V> blankOfSize(int size) {
        return blankOfSize(myStrategy, size);
    }

    public ReusableLinkedHashtable<K, V> copy() {
        ReusableLinkedHashtable<K, V> newTable =
            new ReusableLinkedHashtable<>(myStrategy, myData.clone(), myNextPosAndHash.clone(), mySize);
        newTable.myEndPos = myEndPos;
        return newTable;
    }

    @SuppressWarnings("unchecked")
    public ReusableLinkedHashtable<K, V> copyOfSize(int maxSize) {
        if (canResizeTableTo(maxSize)) {
            return copy();
        }

        ReusableLinkedHashtable<K, V> newTable = blankOfSize(maxSize);
        if (mySize > 0) {
            for (int pos = getStartPos(), endPos = myEndPos; ; pos = myNextPosAndHash[pos]) {
                newTable.insertByIdentity(myNextPosAndHash[pos + 1], (K)myData[pos], (V)myData[pos + 1]);
                if (pos == endPos) {
                    break;
                }
            }
        }
        return newTable;
    }

    public ReusableLinkedHashtable<K, V> copyReversed() {
        ReusableLinkedHashtable<K, V> newTable = copy();
        newTable.reverse();
        return newTable;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public ReusableLinkedHashtable<K, V> copyRangeWithout(
        int maxSize,
        ReusableLinkedHashtableRange range,
        Set<? extends K> excludeKeys,
        int excludePos
    ) {
        ReusableLinkedHashtable<K, V> newTable = blankOfSize(maxSize);
        int startPos = range.getStartPos();
        if (startPos >= 0) {
            for (int pos = startPos, endPos = range.getEndPos(); ; pos = myNextPosAndHash[pos]) {
                K key = (K)myData[pos];
                if ((excludeKeys == null || !excludeKeys.contains(key)) && pos != excludePos) {
                    newTable.insertByIdentity(myNextPosAndHash[pos + 1], key, (V)myData[pos + 1]);
                }
                if (pos == endPos) {
                    break;
                }
            }
        }
        return newTable;
    }

    /**
     * Links specified user of this hash-table via {@code WeakReference} to prevent memory-leaks.
     *
     * @param map user of this hash-table to be monitored to prevent memory-leaks.
     */
    public Range rangeFor(ReusableLinkedHashtableUser map) {
        return rangeFor(map, getSize(), getStartPos(), getEndPos());
    }

    public Range rangeFor(ReusableLinkedHashtableUser map, int size, int startPos, int endPos) {
        return new Range(map, myLargestRange, size, startPos, endPos);
    }

    @Nonnull
    public HashingStrategy<K> getStrategy() {
        return myStrategy;
    }

    public int hashCode(K key) {
        return myStrategy.hashCode(key);
    }

    public int getSize() {
        return mySize;
    }

    /**
     * <p>Checks if new size doesn't exceed upper size limit for current hash-table. Set to 50% to prevent long hash collisions.
     * Hash-table must be recreated after reaching this limit.</p>
     */
    public boolean canResizeTableTo(int newSize) {
        int maxSafeSize = myData.length >> 2;
        return newSize < maxSafeSize;
    }

    @Override
    public int getStartPos() {
        int endPos = myEndPos;
        return endPos >= 0 ? myNextPosAndHash[endPos] : -1;
    }

    @Override
    public int getEndPos() {
        return myEndPos;
    }

    /**
     * <p>Getting position of specified key in the hash-table either for getting existing key/value entry or inserting new one.</p>
     *
     * @param key key to be found.
     * @return Position of existing key or negation of position where this key may be inserted. Negated position is less than 0, so
     * {@code getPos(key) < 0} means that there's no such element in the hash-table yet.
     */
    @SuppressWarnings("unchecked")
    public int getPos(K key) {
        return getPos(myStrategy.hashCode(key), key);
    }

    @SuppressWarnings("unchecked")
    public int getPos(int hashCode, K key) {
        Object[] data = myData;
        int length = data.length;
        if (length == 0) {
            return -1;
        }

        HashingStrategy<K> strategy = myStrategy;
        for (int lengthMask = length - 1, pos = (hashCode << 1) & lengthMask; ; pos = (pos + 2) & lengthMask) {
            K candidate = (K)data[pos];
            if (candidate == null) {
                return ~pos;
            }
            else if (strategy.equals(candidate, key)) {
                return pos;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public int getPosByIdentity(int hashCode, K key) {
        Object[] data = this.myData;
        int length = data.length;
        if (length == 0) {
            return -1;
        }

        for (int lengthMask = length - 1, pos = (hashCode << 1) & lengthMask; ; pos = (pos + 2) & lengthMask) {
            K candidate = (K)data[pos];
            if (candidate == null) {
                return ~pos;
            }
            else if (candidate == key) {
                return pos;
            }
        }
    }

    /**
     * Must be called only from hash-table recreation!
     */
    public ReusableLinkedHashtable<K, V> insertNullable(@Nullable K key, @Nullable V value) {
        if (key == null) {
            throw new IllegalArgumentException("Null keys are not supported");
        }
        return insert(this.myStrategy.hashCode(key), key, value);
    }

    /**
     * Must be called only from hash-table recreation!
     */
    public ReusableLinkedHashtable<K, V> insert(int hashCode, @Nonnull K key, @Nullable V value) {
        return insertAtPos(~getPos(hashCode, key), hashCode, key, value);
    }

    /**
     * Must be called only from hash-table recreation!
     */
    public ReusableLinkedHashtable<K, V> insertByIdentity(int hashCode, @Nonnull K key, @Nullable V value) {
        return insertAtPos(~getPosByIdentity(hashCode, key), hashCode, key, value);
    }

    /**
     * Must be called only from hash-table recreation!
     */
    public ReusableLinkedHashtable<K, V> insertAtPos(int insertPos, int hashCode, @Nonnull K key, @Nullable V value) {
        myData[insertPos] = key;
        myData[insertPos + 1] = value;
        if (myEndPos >= 0) {
            int startPos = myNextPosAndHash[myEndPos];
            myNextPosAndHash[myEndPos] = insertPos;
            myNextPosAndHash[insertPos] = startPos;
        }
        else {
            myNextPosAndHash[insertPos] = insertPos;
        }
        myNextPosAndHash[insertPos + 1] = hashCode;
        myEndPos = insertPos;
        mySize++;
        return this;
    }

    /**
     * <p>Sets entry value and moves the entry to the end of the list.</p>
     *
     * <p>Must be called only from hash-table recreation!</p>
     */
    public ReusableLinkedHashtable<K, V> setValueAtPos(int keyPos, V value) {
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
                int pos = myNextPosAndHash[prevPos];
                if (pos == keyPos) {
                    myNextPosAndHash[prevPos] = myNextPosAndHash[pos];
                    break;
                }
                prevPos = pos;
            }
            myNextPosAndHash[myEndPos] = keyPos;
            myNextPosAndHash[keyPos] = startPos;
        }
        myEndPos = keyPos;

        return this;
    }

    /**
     * <p>Cleaning-up unsuccessful addition of new entries to the map.</p>
     *
     * @param startPosExcluding position of the last entry which must be left in the list.
     * @param endPosIncluding   position until which entries must be removed, including this one.
     */
    public void removeRange(int startPosExcluding, int endPosIncluding) {
        for (int cleaningPos = startPosExcluding, nextCleaningPos; cleaningPos != endPosIncluding; cleaningPos = nextCleaningPos) {
            nextCleaningPos = myNextPosAndHash[cleaningPos];
            myData[nextCleaningPos] = null;
            myData[nextCleaningPos + 1] = null;
            mySize--;
        }
        myNextPosAndHash[startPosExcluding] = myNextPosAndHash[endPosIncluding];
    }

    /**
     * <p>Checks if specified position is within specified entry list range.</p>
     *
     * @param range     key/value entry list range (start and end position) to be checked.
     * @param searchPos position to be be searched within key/value entry list.
     * @return {@code true} if position is present in the list, {@code false} otherwise.
     */
    public boolean isInList(ReusableLinkedHashtableRange range, int searchPos) {
        int startPos = range.getStartPos();
        if (startPos >= 0) {
            for (int pos = startPos, endPos = range.getEndPos(); ; pos = myNextPosAndHash[pos]) {
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

    /**
     * <p>Checks if specified position is within specified entry list range.</p>
     *
     * @param range key/value entry list range (start and end position) to be checked.
     * @param value value to be searched within this range.
     * @return {@code true} if the value was found in the range, {@code false} otherwise.
     */
    public boolean isValueInList(ReusableLinkedHashtableRange range, Object value) {
        int startPos = range.getStartPos();
        if (startPos >= 0) {
            for (int pos = startPos, endPos = range.getEndPos(); ; pos = myNextPosAndHash[pos]) {
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

    @SuppressWarnings("unchecked")
    public K getKey(int keyPos) {
        return (K)myData[keyPos];
    }

    public Object getValue(int keyPos) {
        return myData[keyPos + 1];
    }

    @SuppressWarnings("unchecked")
    public void forEach(ReusableLinkedHashtableRange range, BiConsumer<? super K, ? super V> action) {
        if (mySize > 0) {
            for (int pos = range.getStartPos(), endPos = range.getEndPos(); ; pos = myNextPosAndHash[pos]) {
                action.accept((K)myData[pos], (V)myData[pos + 1]);
                if (pos == endPos) {
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void forEachKey(ReusableLinkedHashtableRange range, Consumer<? super K> action) {
        if (mySize > 0) {
            for (int pos = range.getStartPos(), endPos = range.getEndPos(); ; pos = myNextPosAndHash[pos]) {
                action.accept((K)myData[pos]);
                if (pos == endPos) {
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <V> void forEachValue(ReusableLinkedHashtableRange range, Consumer<? super V> action) {
        if (mySize > 0) {
            for (int pos = range.getStartPos(), endPos = range.getEndPos(); ; pos = myNextPosAndHash[pos]) {
                action.accept((V)myData[pos + 1]);
                if (pos == endPos) {
                    break;
                }
            }
        }
    }

    protected void reverse() {
        if (mySize <= 0) {
            return;
        }

        int endPos = myEndPos, startPos = getStartPos(), prevPos = endPos, pos = startPos;
        while (true) {
            int nextPos = myNextPosAndHash[pos];
            myNextPosAndHash[pos] = prevPos;
            if (pos == endPos) {
                break;
            }
            prevPos = pos;
            pos = nextPos;
        }
        myEndPos = startPos;
    }

    public class Range extends WeakReference<ReusableLinkedHashtableUser> implements ReusableLinkedHashtableRange {
        protected Range myPrevious, myNext;
        public final int myStartPos, myEndPos, mySize;

        protected Range(ReusableLinkedHashtableUser referent, Range next, int size, int startPos, int endPos) {
            super(referent, QUEUE);

            synchronized (ReusableLinkedHashtable.this) {
                if (next != null) {
                    Range previous = next.myPrevious;
                    myPrevious = previous;
                    myNext = next;
                    next.myPrevious = this;
                    previous.myNext = this;
                    if (size == getSize()) {
                        myLargestRange = this;
                    }
                }
                else {
                    myPrevious = this;
                    myNext = this;
                    myLargestRange = this;
                }
            }

            mySize = size;
            myStartPos = startPos;
            myEndPos = endPos;
        }

        public ReusableLinkedHashtable<K, V> getTable() {
            return ReusableLinkedHashtable.this;
        }

        @Override
        public int getStartPos() {
            return myStartPos;
        }

        @Override
        public int getEndPos() {
            return myEndPos;
        }

        public int getPosBefore(int targetPos) {
            for (int newEndPos = myStartPos, nextPos; ; newEndPos = nextPos) {
                nextPos = myNextPosAndHash[newEndPos];
                if (nextPos == targetPos) {
                    return newEndPos;
                }
            }
        }

        /**
         * <p>Checks if new size doesn't exceed upper size limit for current hash-table. Set to 50% to prevent long hash collisions.
         * Hash-table must be recreated after reaching this limit.</p>
         */
        public boolean canResizeTableTo(int newSize) {
            return ReusableLinkedHashtable.this.canResizeTableTo(newSize) && isMasterRange();
        }

        public boolean isMasterRange() {
            return mySize == getSize();
        }

        public int getPosAfter(int targetPos) {
            return myNextPosAndHash[targetPos];
        }

        public boolean isInList(int keyPos) {
            return ReusableLinkedHashtable.this.isInList(this, keyPos);
        }

        public boolean isValueInList(Object value) {
            return ReusableLinkedHashtable.this.isValueInList(this, value);
        }

        @Nonnull
        public ReusableLinkedHashtable<K, V> copy() {
            return copyRangeWithout(mySize, this, null, -1);
        }

        @Nonnull
        public ReusableLinkedHashtable<K, V> copyReversed() {
            if (isMasterRange()) {
                return ReusableLinkedHashtable.this.copyReversed();
            }

            ReusableLinkedHashtable<K, V> newTable = copy();
            newTable.reverse();
            return newTable;
        }

        @Nonnull
        @SuppressWarnings("unchecked")
        public ReusableLinkedHashtable<K, V> copyWithout(int maxSize, Set<? extends K> excludeKeys, int excludePos) {
            return copyRangeWithout(maxSize, this, excludeKeys, excludePos);
        }

        @SuppressWarnings("unchecked")
        public void forEach(BiConsumer<? super K, ? super V> action) {
            ReusableLinkedHashtable.this.forEach(this, action);
        }

        @SuppressWarnings("unchecked")
        public void forEachKey(Consumer<? super K> action) {
            ReusableLinkedHashtable.this.forEachKey(this, action);
        }

        @SuppressWarnings("unchecked")
        public void forEachValue(Consumer<? super V> action) {
            ReusableLinkedHashtable.this.forEachValue(this, action);
        }

        @Nonnull
        public Iterator<Map.Entry<K, V>> entryIterator() {
            return new EntryIterator(this);
        }

        @Nonnull
        public Iterator<K> keyIterator() {
            return new KeyIterator(this);
        }

        @Nonnull
        public Iterator<V> valueIterator() {
            return new ValueIterator(this);
        }

        protected void preventMemoryLeaks() {
            Range detach = null;

            synchronized (ReusableLinkedHashtable.this) {
                if (myPrevious != this) {
                    myPrevious.myNext = myNext;
                }

                if (myNext != this) {
                    myNext.myPrevious = myPrevious;
                }

                if (myLargestRange == this) {
                    myLargestRange = null;
                    detach = myNext != this ? myNext : null;
                }
            }

            if (detach != null) {
                Range range = detach;
                do {
                    ReusableLinkedHashtableUser map = range.get();
                    if (map != null) {
                        map.detachFromTable();
                    }
                    range = range.myNext;
                }
                while (range != detach);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ReusableLinkedHashtable.class);

    static {
        Thread.ofVirtual()
            .name("ReusableLinkehHashtable memory leaks resolver")
            .start(() -> {
                //noinspection InfiniteLoopStatement
                while (true) {
                    try {
                        ReusableLinkedHashtable.Range range = (ReusableLinkedHashtable.Range)QUEUE.remove();
                        range.preventMemoryLeaks();
                    }
                    catch (InterruptedException ignore) {
                    }
                    catch (Exception e) {
                        LOG.error("Exception while trying to prevent memory leaks", e);
                    }
                }
            });
    }

    private static int ceilToPowerOf2(int number) {
        int r = number - 1;
        r = r | (r >> 1);
        r = r | (r >> 2);
        r = r | (r >> 4);
        r = r | (r >> 8);
        r = r | (r >> 16);
        return r + 1;
    }
}
