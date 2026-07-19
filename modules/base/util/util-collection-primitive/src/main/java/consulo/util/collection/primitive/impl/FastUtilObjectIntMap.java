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

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.FastUtilHashingStrategies;
import consulo.util.collection.primitive.ints.IntCollection;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import org.jspecify.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * {@link ObjectIntMap} backed by a fastutil {@link Object2IntOpenCustomHashMap}.
 *
 * @author VISTALL
 * @deprecated use {@link it.unimi.dsi.fastutil.objects.Object2IntMap} directly
 */
@Deprecated
public class FastUtilObjectIntMap<K> implements ObjectIntMap<K> {
    private static final int UNKNOWN_CAPACITY = -1;

    private record SimpleEntry<T>(T key, int value) implements Entry<T> {
        @Override
        public T getKey() {
            return key();
        }

        @Override
        public int getValue() {
            return value();
        }
    }

    private final Object2IntOpenCustomHashMap<K> myDelegate;

    private @Nullable Set<Entry<K>> myEntrySet = null;
    private @Nullable IntCollection myValues = null;

    public FastUtilObjectIntMap(int capacity, HashingStrategy<K> strategy) {
        myDelegate = capacity == UNKNOWN_CAPACITY
            ? new Object2IntOpenCustomHashMap<>(FastUtilHashingStrategies.of(strategy))
            : new Object2IntOpenCustomHashMap<>(capacity, FastUtilHashingStrategies.of(strategy));
    }

    @Deprecated
    @Override
    public int getInt(K key) {
        return myDelegate.getInt(key);
    }

    @Deprecated
    @Override
    public int getIntOrDefault(K key, int defaultValue) {
        return myDelegate.getOrDefault(key, defaultValue);
    }

    @Deprecated
    @Override
    public void putInt(K key, int value) {
        myDelegate.put(key, value);
    }

    @Deprecated
    @Override
    public void putAll(ObjectIntMap<? extends K> map) {
        map.forEach(this::putInt);
    }

    @Deprecated
    @Override
    public int size() {
        return myDelegate.size();
    }

    @Deprecated
    @Override
    public void clear() {
        myDelegate.clear();
    }

    @Deprecated
    @Override
    public void forEach(ObjIntConsumer<? super K> action) {
        for (Object2IntMap.Entry<K> entry : Object2IntMaps.fastIterable(myDelegate)) {
            action.accept(entry.getKey(), entry.getIntValue());
        }
    }

    @Deprecated
    @Override
    public boolean containsKey(K key) {
        return myDelegate.containsKey(key);
    }

    @Deprecated
    @Override
    public int remove(K key) {
        return myDelegate.removeInt(key);
    }

    @Deprecated
    @Override
    public Set<K> keySet() {
        return myDelegate.keySet();
    }

    @Deprecated
    @Override
    public IntCollection values() {
        if (myValues == null) {
            myValues = new FastUtilIntCollection(myDelegate.values());
        }
        return myValues;
    }

    @Deprecated
    @Override
    public Set<Entry<K>> entrySet() {
        if (myEntrySet == null) {
            myEntrySet = new AbstractSet<>() {
                @Override
                public Iterator<Entry<K>> iterator() {
                    Iterator<Object2IntMap.Entry<K>> iterator = Object2IntMaps.fastIterator(myDelegate);
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Entry<K> next() {
                            Object2IntMap.Entry<K> entry = iterator.next();
                            return new SimpleEntry<>(entry.getKey(), entry.getIntValue());
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
