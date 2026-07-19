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

import consulo.util.collection.primitive.longs.LongSet;
import consulo.util.collection.primitive.longs.impl.LongCollectionImpls;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.PrimitiveIterator;

/**
 * {@link LongSet} backed by a fastutil {@link LongOpenHashSet}.
 *
 * @author VISTALL
 * @deprecated use {@link it.unimi.dsi.fastutil.longs.LongSet} directly
 */
@Deprecated
public class FastUtilLongSet implements LongSet {
    private static final int UNKNOWN_CAPACITY = -1;

    private final LongOpenHashSet mySet;

    public FastUtilLongSet(int capacity) {
        mySet = capacity == UNKNOWN_CAPACITY ? new LongOpenHashSet() : new LongOpenHashSet(capacity);
    }

    @Deprecated
    @Override
    public boolean add(long value) {
        return mySet.add(value);
    }

    @Deprecated
    @Override
    public boolean remove(long value) {
        return mySet.rem(value);
    }

    @Deprecated
    @Override
    public boolean contains(long value) {
        return mySet.contains(value);
    }

    @Deprecated
    @Override
    public long[] toArray() {
        return mySet.toLongArray();
    }

    @Deprecated
    @Override
    public int size() {
        return mySet.size();
    }

    @Deprecated
    @Override
    public void clear() {
        mySet.clear();
    }

    @Deprecated
    @Override
    public PrimitiveIterator.OfLong iterator() {
        LongIterator iterator = mySet.iterator();
        return new PrimitiveIterator.OfLong() {
            @Override
            public long nextLong() {
                return iterator.nextLong();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    @Deprecated
    @Override
    public int hashCode() {
        return LongCollectionImpls.hashCode(this);
    }
}
