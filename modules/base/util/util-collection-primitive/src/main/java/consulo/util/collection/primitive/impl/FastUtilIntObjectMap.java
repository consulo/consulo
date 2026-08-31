/*
 * Copyright 2013-2026 consulo.io
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
package consulo.util.collection.primitive.impl;

import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.collection.primitive.ints.IntSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * {@link IntObjectMap} backed by a fastutil {@link Int2ObjectOpenHashMap}.
 *
 * @author VISTALL
 * @deprecated use {@link it.unimi.dsi.fastutil.ints.Int2ObjectMap} directly
 */
@Deprecated
public class FastUtilIntObjectMap<V> implements IntObjectMap<V> {
    private static final int UNKNOWN_CAPACITY = -1;

    private record IntObjectEntryRecord<V1>(int key, @Nullable V1 value) implements IntObjectEntry<V1> {
        @Override
        public int getKey() {
            return key();
        }

        @Override
        public @Nullable V1 getValue() {
            return value();
        }
    }

    private final Int2ObjectOpenHashMap<V> myDelegate;

    private @Nullable Set<IntObjectEntry<V>> myEntrySet = null;
    private @Nullable IntSet myKeySet = null;

    public FastUtilIntObjectMap(int capacity) {
        myDelegate = capacity == UNKNOWN_CAPACITY ? new Int2ObjectOpenHashMap<>() : new Int2ObjectOpenHashMap<>(capacity);
    }

    @Deprecated
    @Override
    public @Nullable V put(int key, @Nullable V value) {
        return myDelegate.put(key, value);
    }

    @Deprecated
    @Override
    public @Nullable V get(int key) {
        return myDelegate.get(key);
    }

    @Deprecated
    @Override
    public boolean containsKey(int key) {
        return myDelegate.containsKey(key);
    }

    @Deprecated
    @Override
    public boolean containsValue(V value) {
        return myDelegate.containsValue(value);
    }

    @Deprecated
    @Override
    public @Nullable V remove(int key) {
        return myDelegate.remove(key);
    }

    @Deprecated
    @Override
    public int size() {
        return myDelegate.size();
    }

    @Deprecated
    @Override
    public boolean isEmpty() {
        return myDelegate.isEmpty();
    }

    @Deprecated
    @Override
    public void clear() {
        myDelegate.clear();
    }

    @Deprecated
    @Override
    public int[] keys() {
        return myDelegate.keySet().toIntArray();
    }

    @Deprecated
    @Override
    public IntSet keySet() {
        if (myKeySet == null) {
            myKeySet = new FastUtilIntSet(myDelegate.keySet());
        }
        return myKeySet;
    }

    @Deprecated
    @Override
    public Collection<V> values() {
        return myDelegate.values();
    }

    @Deprecated
    @Override
    public Set<IntObjectEntry<V>> entrySet() {
        if (myEntrySet == null) {
            myEntrySet = new AbstractSet<>() {
                @Override
                public Iterator<IntObjectEntry<V>> iterator() {
                    Iterator<Int2ObjectMap.Entry<V>> iterator = Int2ObjectMaps.fastIterator(myDelegate);
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public IntObjectEntry<V> next() {
                            Int2ObjectMap.Entry<V> entry = iterator.next();
                            return new IntObjectEntryRecord<>(entry.getIntKey(), entry.getValue());
                        }
                    };
                }

                @Override
                public int size() {
                    return myDelegate.size();
                }
            };
        }
        return myEntrySet;
    }
}
