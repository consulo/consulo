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

import consulo.util.collection.impl.map.RefValueHashMap;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.lang.ref.SoftReference;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jspecify.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Base class for concurrent key:int -> (weak/soft) value:V map
 * Null values are NOT allowed
 */
public abstract class ConcurrentIntKeyRefValueHashMap<V> extends AbstractInt2ObjectMap<V> implements ConcurrentIntObjectMap<V> {
  private final ConcurrentIntObjectHashMap<IntReference<V>> myMap = new ConcurrentIntObjectHashMap<>();
  private final ReferenceQueue<V> myQueue = new ReferenceQueue<>();

  protected abstract IntReference<V> createReference(int key, @Nullable V value, ReferenceQueue<V> queue);

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

  @Override
  public V cacheOrGet(int key, V value) {
    processQueue();
    IntReference<V> newRef = createReference(key, value, myQueue);
    while (true) {
      IntReference<V> ref = myMap.putIfAbsent(key, newRef);
      if (ref == null) return value; // there were no previous value
      V old = ref.get();
      if (old != null) return old;

      // old value has been gc-ed; need to overwrite
      boolean replaced = myMap.replace(key, ref, newRef);
      if (replaced) {
        return value;
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean remove(int key, @Nullable Object value) {
    processQueue();
    return myMap.remove(key, createReference(key, (V)value, myQueue));
  }

  @Override
  public boolean replace(int key, @Nullable V oldValue, @Nullable V newValue) {
    processQueue();
    return myMap.replace(key, createReference(key, oldValue, myQueue), createReference(key, newValue, myQueue));
  }

  @Override
  public @Nullable V put(int key, @Nullable V value) {
    processQueue();
    IntReference<V> ref = myMap.put(key, createReference(key, value, myQueue));
    return SoftReference.deref(ref);
  }

  @Override
  public @Nullable V get(int key) {
    IntReference<V> ref = myMap.get(key);
    return SoftReference.deref(ref);
  }

  @Override
  public @Nullable V remove(int key) {
    processQueue();
    IntReference<V> ref = myMap.remove(key);
    return SoftReference.deref(ref);
  }

  @Override
  public boolean containsKey(int key) {
    throw RefValueHashMap.pointlessContainsKey();
  }

  @Override
  public boolean containsValue(Object value) {
    throw RefValueHashMap.pointlessContainsValue();
  }

  @Override
  public void clear() {
    myMap.clear();
    processQueue();
  }

  public int[] keys() {
    return myMap.keys();
  }

  @Override
  public ObjectSet<Int2ObjectMap.Entry<V>> int2ObjectEntrySet() {
    return new MyEntrySetView();
  }

  private class MyEntrySetView extends AbstractObjectSet<Int2ObjectMap.Entry<V>> {
    @Override
    public ObjectIterator<Int2ObjectMap.Entry<V>> iterator() {
      return entriesIterator();
    }

    @Override
    public int size() {
      return ConcurrentIntKeyRefValueHashMap.this.size();
    }
  }

  private ObjectIterator<Int2ObjectMap.Entry<V>> entriesIterator() {
    final ObjectIterator<Int2ObjectMap.Entry<IntReference<V>>> entryIterator = myMap.int2ObjectEntrySet().iterator();
    return new ObjectIterator<>() {
      private Int2ObjectMap.@Nullable Entry<V> nextVEntry = null;
      private Int2ObjectMap.@Nullable Entry<IntReference<V>> nextReferenceEntry = null;
      private Int2ObjectMap.@Nullable Entry<IntReference<V>> lastReturned = null;

      {
        nextAliveEntry();
      }

      @Override
      public boolean hasNext() {
        return nextVEntry != null;
      }

      @Override
      public Int2ObjectMap.Entry<V> next() {
        if (!hasNext()) throw new NoSuchElementException();
        Int2ObjectMap.Entry<V> result = Objects.requireNonNull(nextVEntry);
        lastReturned = nextReferenceEntry;
        nextAliveEntry();
        return result;
      }

      private void nextAliveEntry() {
        while (entryIterator.hasNext()) {
          Int2ObjectMap.Entry<IntReference<V>> entry = entryIterator.next();
          V v = Objects.requireNonNull(entry.getValue()).get();
          if (v == null) {
            continue;
          }
          int key = entry.getIntKey();
          nextVEntry = new AbstractInt2ObjectMap.BasicEntry<>(key, v);
          nextReferenceEntry = entry;
          return;
        }
        nextVEntry = null;
      }

      @Override
      public void remove() {
        Int2ObjectMap.Entry<IntReference<V>> last = lastReturned;
        if (last == null) throw new NoSuchElementException();
        myMap.replaceNode(last.getIntKey(), null, last.getValue());
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

  @Override
  public @Nullable V putIfAbsent(int key, @Nullable V value) {
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
}
