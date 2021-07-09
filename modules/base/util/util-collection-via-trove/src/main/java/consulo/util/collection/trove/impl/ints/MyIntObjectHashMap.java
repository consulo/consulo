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
package consulo.util.collection.trove.impl.ints;

import consulo.util.collection.primitive.ints.AbstractIntSet;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.collection.primitive.ints.IntSet;
import gnu.trove.TIntHashingStrategy;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TObjectHash;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public class MyIntObjectHashMap<V> extends TIntObjectHashMap<V> implements IntObjectMap<V> {
  private static record IntObjectEntryRecord<V1>(int key, V1 value) implements IntObjectEntry<V1> {
    @Override
    public int getKey() {
      return key();
    }

    @Override
    public V1 getValue() {
      return value();
    }
  }

  private static class MyEntryIterator<V1> implements Iterator<IntObjectEntry<V1>>  {
    private TIntObjectIterator<V1> myIterator;

    private MyEntryIterator(TIntObjectIterator<V1> iterator) {
      myIterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return myIterator.hasNext();
    }

    @Override
    public IntObjectEntry<V1> next() {
      myIterator.advance();

      int key = myIterator.key();
      Object value = myIterator.value();
      return new IntObjectEntryRecord<>(key, nullize(value));
    }
  }

  private class MyEntrySet extends AbstractSet<IntObjectEntry<V>> {

    @Nonnull
    @Override
    public Iterator<IntObjectEntry<V>> iterator() {
      return new MyEntryIterator<>(MyIntObjectHashMap.this.iterator());
    }

    @Override
    public int size() {
      return MyIntObjectHashMap.this.size();
    }
  }

  private static class MyValuesIterator<V1> implements Iterator<V1> {
    private TIntObjectIterator<V1> myIterator;

    private MyValuesIterator(TIntObjectIterator<V1> iterator) {
      myIterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return myIterator.hasNext();
    }

    @Override
    public V1 next() {
      myIterator.advance();

      return nullize(myIterator.value());
    }
  }

  private class MyValues extends AbstractCollection<V> {

    @Nonnull
    @Override
    public Iterator<V> iterator() {
      return new MyValuesIterator<>(MyIntObjectHashMap.this.iterator());
    }

    @Override
    public int size() {
      return MyIntObjectHashMap.this.size();
    }
  }

  private static class MyKeySetIterator implements PrimitiveIterator.OfInt {
    private TIntObjectIterator<?> myIterator;

    private MyKeySetIterator(TIntObjectIterator<?> iterator) {
      myIterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return myIterator.hasNext();
    }

    @Override
    public int nextInt() {
      myIterator.advance();
      return myIterator.key();
    }

    @Override
    public void remove() {
      myIterator.remove();
    }
  }

  private class MyKeySet extends AbstractIntSet {
    @Nonnull
    @Override
    public PrimitiveIterator.OfInt iterator() {
      return new MyKeySetIterator(MyIntObjectHashMap.this.iterator());
    }

    @Override
    public int size() {
      return MyIntObjectHashMap.this.size();
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T nullize(Object value) {
    if(value == TObjectHash.NULL) {
      return null;
    }
    return (T)value;
  }

  private Set<IntObjectEntry<V>> myEntrySet;
  private Collection<V> myValues;
  private IntSet myKeySet;

  public MyIntObjectHashMap() {
  }

  public MyIntObjectHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public MyIntObjectHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public MyIntObjectHashMap(TIntHashingStrategy strategy) {
    super(strategy);
  }

  public MyIntObjectHashMap(int initialCapacity, TIntHashingStrategy strategy) {
    super(initialCapacity, strategy);
  }

  public MyIntObjectHashMap(int initialCapacity, float loadFactor, TIntHashingStrategy strategy) {
    super(initialCapacity, loadFactor, strategy);
  }

  @Override
  public V put(int key, V value) {
    return super.put(key, value);
  }

  @Nonnull
  @Override
  public Set<IntObjectEntry<V>> entrySet() {
    if(myEntrySet == null) {
      myEntrySet = new MyEntrySet();
    }
    return myEntrySet;
  }

  @Nonnull
  @Override
  public Collection<V> values() {
    if(myValues == null) {
      myValues = new MyValues();
    }
    return myValues;
  }

  @Nonnull
  @Override
  public IntSet keySet() {
    if(myKeySet == null) {
      myKeySet = new MyKeySet();
    }
    return myKeySet;
  }
}
