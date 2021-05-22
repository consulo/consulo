/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.util.collection.primitive.ints.impl.map;

import consulo.util.collection.ContainerUtil;
import consulo.util.collection.impl.map.RefValueHashMap;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.lang.ref.SoftReference;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.function.Supplier;

/**
 * Base class for concurrent key:int -> (weak/soft) value:V map
 * Null values are NOT allowed
 */
public abstract class ConcurrentIntKeyRefValueHashMap<V> implements ConcurrentIntObjectMap<V> {
  private final ConcurrentIntObjectHashMap<IntReference<V>> myMap = new ConcurrentIntObjectHashMap<>();
  private final ReferenceQueue<V> myQueue = new ReferenceQueue<>();

  @Nonnull
  protected abstract IntReference<V> createReference(int key, @Nonnull V value, @Nonnull ReferenceQueue<V> queue);

  interface IntReference<V> extends Supplier<V> {
    int getKey();
  }

  private void processQueue() {
    while (true) {
      //noinspection unchecked
      IntReference<V> ref = (IntReference<V>)myQueue.poll();
      if (ref == null) {
        return;
      }
      int key = ref.getKey();
      myMap.remove(key, ref);
    }
  }

  @Nonnull
  @Override
  public V cacheOrGet(int key, @Nonnull V value) {
    processQueue();
    IntReference<V> newRef = createReference(key, value, myQueue);
    while (true) {
      IntReference<V> ref = myMap.putIfAbsent(key, newRef);
      if (ref == null) return value; // there were no previous value
      V old = ref.get();
      if (old != null) return old;

      // old value has been gced; need to overwrite
      boolean replaced = myMap.replace(key, ref, newRef);
      if (replaced) {
        return value;
      }
    }
  }

  @Override
  public boolean remove(int key, @Nonnull V value) {
    processQueue();
    return myMap.remove(key, createReference(key, value, myQueue));
  }

  @Override
  public boolean replace(int key, @Nonnull V oldValue, @Nonnull V newValue) {
    processQueue();
    return myMap.replace(key, createReference(key, oldValue, myQueue), createReference(key, newValue, myQueue));
  }

  @Override
  public V put(int key, @Nonnull V value) {
    processQueue();
    IntReference<V> ref = myMap.put(key, createReference(key, value, myQueue));
    return SoftReference.deref(ref);
  }

  @Override
  public V get(int key) {
    IntReference<V> ref = myMap.get(key);
    return SoftReference.deref(ref);
  }

  @Override
  public V remove(int key) {
    processQueue();
    IntReference<V> ref = myMap.remove(key);
    return SoftReference.deref(ref);
  }

  @Override
  public boolean containsKey(int key) {
    throw RefValueHashMap.pointlessContainsKey();
  }

  @Override
  public boolean containsValue(@Nonnull Object value) {
    throw RefValueHashMap.pointlessContainsValue();
  }

  @Override
  public void clear() {
    myMap.clear();
    processQueue();
  }

  @Nonnull
  @Override
  public int[] keys() {
    return myMap.keys();
  }

  @Nonnull
  @Override
  public Set<IntObjectEntry<V>> entrySet() {
    return new MyEntrySetView();
  }

  @Nonnull
  @Override
  public IntSet keySet() {
    // todo [vistall] todo
    throw new UnsupportedOperationException("todo");
  }

  private class MyEntrySetView extends AbstractSet<IntObjectEntry<V>> {
    @Nonnull
    @Override
    public Iterator<IntObjectEntry<V>> iterator() {
      return entriesIterator();
    }

    @Override
    public int size() {
      return ConcurrentIntKeyRefValueHashMap.this.size();
    }
  }

  @Nonnull
  private Iterator<IntObjectEntry<V>> entriesIterator() {
    final Iterator<IntObjectEntry<IntReference<V>>> entryIterator = myMap.entrySet().iterator();
    return new Iterator<>() {
      private IntObjectEntry<V> nextVEntry;
      private IntObjectEntry<IntReference<V>> nextReferenceEntry;
      private IntObjectEntry<IntReference<V>> lastReturned;

      {
        nextAliveEntry();
      }

      @Override
      public boolean hasNext() {
        return nextVEntry != null;
      }

      @Override
      public IntObjectEntry<V> next() {
        if (!hasNext()) throw new NoSuchElementException();
        IntObjectEntry<V> result = nextVEntry;
        lastReturned = nextReferenceEntry;
        nextAliveEntry();
        return result;
      }

      private void nextAliveEntry() {
        while (entryIterator.hasNext()) {
          IntObjectEntry<IntReference<V>> entry = entryIterator.next();
          final V v = entry.getValue().get();
          if (v == null) {
            continue;
          }
          final int key = entry.getKey();
          nextVEntry = new SimpleIntObjectEntry<>(key, v);
          nextReferenceEntry = entry;
          return;
        }
        nextVEntry = null;
      }

      @Override
      public void remove() {
        IntObjectEntry<IntReference<V>> last = lastReturned;
        if (last == null) throw new NoSuchElementException();
        myMap.replaceNode(last.getKey(), null, last.getValue());
      }
    };
  }

  @Override
  public int size() {
    processQueue();
    return myMap.size();
  }

  @Override
  public boolean isEmpty() {
    processQueue();
    return myMap.isEmpty();
  }

  @Nonnull
  public Iterator<V> elementsIterator() {
    final Iterator<IntReference<V>> elementRefs = myMap.values().iterator();
    return new Iterator<V>() {
      private V findNextRef() {
        while (elementRefs.hasNext()) {
          IntReference<V> result = elementRefs.next();
          V v = result.get();
          if (v != null) return v;
        }
        return null;
      }

      private V next = findNextRef();

      @Override
      public boolean hasNext() {
        return next != null;
      }

      @Override
      public V next() {
        if (next == null) throw new NoSuchElementException();
        V v = next;
        next = findNextRef();
        return v;
      }
    };
  }


  @Override
  public V putIfAbsent(int key, @Nonnull V value) {
    IntReference<V> newRef = createReference(key, value, myQueue);
    while (true) {
      processQueue();
      IntReference<V> oldRef = myMap.putIfAbsent(key, newRef);
      if (oldRef == null) return null;
      V oldVal = oldRef.get();
      if (oldVal == null) {
        if (myMap.replace(key, oldRef, newRef)) return null;
      }
      else {
        return oldVal;
      }
    }
  }

  @Nonnull
  @Override
  public Collection<V> values() {
    Set<V> result = new HashSet<>();
    ContainerUtil.addAll(result, elementsIterator());
    return result;
  }
}