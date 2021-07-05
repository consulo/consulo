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
package consulo.util.collection.trove.impl.objects;

import consulo.util.collection.primitive.ints.AbstractIntCollection;
import consulo.util.collection.primitive.ints.IntCollection;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * @author VISTALL
 * @since 08/05/2021
 */
public class MyObjectIntHashMap<K> extends TObjectIntHashMap<K> implements ObjectIntMap<K> {
  private class MyEntrySet extends AbstractSet<Entry<K>> {
    @Nonnull
    @Override
    public Iterator<Entry<K>> iterator() {
      return new EntryIter<>(MyObjectIntHashMap.this.iterator());
    }

    @Override
    public int size() {
      return MyObjectIntHashMap.this.size();
    }
  }

  private class MyKeySet extends AbstractSet<K> {
    @Nonnull
    @Override
    public Iterator<K> iterator() {
      return new KeyIter<>(MyObjectIntHashMap.this.iterator());
    }

    @Override
    public int size() {
      return MyObjectIntHashMap.this.size();
    }
  }

  private class MyValueCollection extends AbstractIntCollection {

    @Nonnull
    @Override
    public PrimitiveIterator.OfInt iterator() {
      return new ValueIter<>(MyObjectIntHashMap.this.iterator());
    }

    @Override
    public int size() {
      return MyObjectIntHashMap.this.size();
    }
  }

  private class ValueIter<T> implements PrimitiveIterator.OfInt {
    private TObjectIntIterator<T> myIterator;

    private ValueIter(TObjectIntIterator<T> iterator) {
      myIterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return myIterator.hasNext();
    }

    @Override
    public int nextInt() {
      myIterator.advance();
      return myIterator.value();
    }
  }

  private class KeyIter<T> implements Iterator<T> {
    private TObjectIntIterator<T> myIterator;

    private KeyIter(TObjectIntIterator<T> iterator) {
      myIterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return myIterator.hasNext();
    }

    @Override
    public T next() {
      myIterator.advance();
      return myIterator.key();
    }
  }

  private static class EntryIter<T> implements Iterator<Entry<T>> {
    private TObjectIntIterator<T> myIterator;

    private EntryIter(TObjectIntIterator<T> iterator) {
      myIterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return myIterator.hasNext();
    }

    @Override
    public Entry<T> next() {
      myIterator.advance();
      T key = myIterator.key();
      int value = myIterator.value();
      return new SimpleEntry<>(key, value);
    }
  }

  private static record SimpleEntry<T>(T key, int value) implements Entry<T>{
    @Override
    public T getKey() {
      return key();
    }

    @Override
    public int getValue() {
      return value();
    }
  }

  private MyEntrySet myEntrySet;
  private MyKeySet myKeySet;
  private MyValueCollection myValueCollection;

  public MyObjectIntHashMap() {
  }

  public MyObjectIntHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public MyObjectIntHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public MyObjectIntHashMap(TObjectHashingStrategy<K> strategy) {
    super(strategy);
  }

  public MyObjectIntHashMap(int initialCapacity, TObjectHashingStrategy<K> strategy) {
    super(initialCapacity, strategy);
  }

  public MyObjectIntHashMap(int initialCapacity, float loadFactor, TObjectHashingStrategy<K> strategy) {
    super(initialCapacity, loadFactor, strategy);
  }

  @Override
  public void forEach(ObjIntConsumer<? super K> action) {
    for (TObjectIntIterator<K> iter = iterator(); iter.hasNext(); ) {
      iter.advance();

      K key = iter.key();
      int value = iter.value();

      action.accept(key, value);
    }
  }

  @Override
  public int getInt(K key) {
    return get(key);
  }

  @Override
  public int getIntOrDefault(K key, int defaultValue) {
    int index = index(key);
    return index < 0 ? defaultValue : _values[index];
  }

  @Override
  public void putInt(K key, int value) {
    put(key, value);
  }

  @Override
  public void putAll(@Nonnull ObjectIntMap<? extends K> map) {
    ensureCapacity(size() + map.size());
    forEach(this::putInt);
  }

  @Override
  public Set<Entry<K>> entrySet() {
    if (myEntrySet == null) {
      myEntrySet = new MyEntrySet();
    }
    return myEntrySet;
  }

  @Override
  public Set<K> keySet() {
    if(myKeySet == null) {
      myKeySet = new MyKeySet();
    }
    return myKeySet;
  }

  @Nonnull
  @Override
  public IntCollection values() {
    if(myValueCollection == null) {
      myValueCollection = new MyValueCollection();
    }
    return myValueCollection;
  }
}
