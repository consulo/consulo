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

import consulo.util.collection.primitive.ints.BiIntConsumer;
import consulo.util.collection.primitive.ints.IntIntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * {@link IntIntMap} backed by a fastutil {@link Int2IntOpenHashMap}.
 *
 * @author VISTALL
 * @deprecated use {@link it.unimi.dsi.fastutil.ints.Int2IntMap} directly
 */
@Deprecated
public class FastUtilIntIntMap implements IntIntMap {
    private static final int UNKNOWN_CAPACITY = -1;

    private final Int2IntOpenHashMap myDelegate;

    public FastUtilIntIntMap(int capacity) {
        myDelegate = capacity == UNKNOWN_CAPACITY ? new Int2IntOpenHashMap() : new Int2IntOpenHashMap(capacity);
    }

    @Deprecated
    @Override
    public void putInt(int key, int value) {
        myDelegate.put(key, value);
    }

    @Deprecated
    @Override
    public int getInt(int key) {
        return myDelegate.get(key);
    }

    @Deprecated
    @Override
    public void clear() {
        myDelegate.clear();
    }

    @Deprecated
    @Override
    public int size() {
        return myDelegate.size();
    }

    @Deprecated
    @Override
    public int remove(int key) {
        return myDelegate.remove(key);
    }

    @Deprecated
    @Override
    public boolean containsKey(int key) {
        return myDelegate.containsKey(key);
    }

    @Deprecated
    @Override
    public void trimToSize() {
        myDelegate.trim();
    }

    @Deprecated
    @Override
    public void forEach(BiIntConsumer consumer) {
        for (Int2IntMap.Entry entry : Int2IntMaps.fastIterable(myDelegate)) {
            consumer.accept(entry.getIntKey(), entry.getIntValue());
        }
    }

    @Deprecated
    @Override
    public int[] keys() {
        return myDelegate.keySet().toIntArray();
    }
}
