/*
 * Copyright 2013-2021 consulo.io
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
package consulo.util.collection.trove.impl.longs;

import consulo.util.collection.primitive.longs.LongSet;
import consulo.util.collection.primitive.longs.impl.LongCollectionImpls;
import gnu.trove.TLongHashSet;
import gnu.trove.TLongIterator;

import javax.annotation.Nonnull;
import java.util.PrimitiveIterator;

/**
 * @author VISTALL
 * @since 17/05/2021
 */
public class MyLongHashSet implements LongSet {
  static class Iter implements PrimitiveIterator.OfLong {

    private final TLongIterator myIterator;

    public Iter(TLongIterator iterator) {
      myIterator = iterator;
    }

    @Override
    public long nextLong() {
      return myIterator.next();
    }

    @Override
    public boolean hasNext() {
      return myIterator.hasNext();
    }

    @Override
    public void remove() {
      myIterator.remove();
    }
  }

  private final TLongHashSet mySet;

  public MyLongHashSet() {
    mySet = new TLongHashSet();
  }

  public MyLongHashSet(int capacity) {
    mySet = new TLongHashSet(capacity);
  }

  @Override
  public boolean add(long value) {
    return mySet.add(value);
  }

  @Override
  public boolean remove(long value) {
    return mySet.remove(value);
  }

  @Override
  public boolean contains(long value) {
    return mySet.contains(value);
  }

  @Override
  public long[] toArray() {
    return mySet.toArray();
  }

  @Override
  public int size() {
    return mySet.size();
  }

  @Override
  public void clear() {
    mySet.clear();
  }

  @Nonnull
  @Override
  public PrimitiveIterator.OfLong iterator() {
    return new Iter(mySet.iterator());
  }

  @Override
  public int hashCode() {
    return LongCollectionImpls.hashCode(this);
  }
}
