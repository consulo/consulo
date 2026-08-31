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

import consulo.util.collection.primitive.ints.IntCollection;

import java.util.PrimitiveIterator;

/**
 * {@link IntCollection} view backed by a fastutil int collection.
 *
 * @author VISTALL
 * @deprecated use {@link it.unimi.dsi.fastutil.ints.IntCollection} directly
 */
@Deprecated
public class FastUtilIntCollection implements IntCollection {
    protected final it.unimi.dsi.fastutil.ints.IntCollection myDelegate;

    FastUtilIntCollection(it.unimi.dsi.fastutil.ints.IntCollection delegate) {
        myDelegate = delegate;
    }

    @Deprecated
    @Override
    public boolean add(int value) {
        return myDelegate.add(value);
    }

    @Deprecated
    @Override
    public boolean remove(int value) {
        return myDelegate.rem(value);
    }

    @Deprecated
    @Override
    public boolean contains(int value) {
        return myDelegate.contains(value);
    }

    @Deprecated
    @Override
    public int[] toArray() {
        return myDelegate.toIntArray();
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
    public PrimitiveIterator.OfInt iterator() {
        it.unimi.dsi.fastutil.ints.IntIterator iterator = myDelegate.iterator();
        return new PrimitiveIterator.OfInt() {
            @Override
            public int nextInt() {
                return iterator.nextInt();
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
}
