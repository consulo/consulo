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
package consulo.util.collection.impl.map;

import consulo.util.collection.HashingStrategy;
import consulo.util.lang.Comparing;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.util.*;

/**
 * Base class for (soft/weak) keys -> hard values map
 * Null keys are NOT allowed
 * Null values are allowed
 */
public abstract class RefHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
  public interface SubMap<K1, V1> extends Map<Key<K1>, V1> {
    void compactIfNecessary();
  }

  private final SubMap<K, V> myMap;
  private final ReferenceQueue<K> myReferenceQueue = new ReferenceQueue<>();
  private final HardKey myHardKeyInstance = new HardKey(); // "singleton"
  @Nonnull
  private final HashingStrategy<? super K> myStrategy;
  private Set<Entry<K, V>> entrySet;
  private boolean processingQueue;

  public RefHashMap(int initialCapacity, float loadFactor, @Nonnull final HashingStrategy<? super K> strategy) {
    myStrategy = strategy;
    myMap = createSubMap(initialCapacity, loadFactor, strategy);
  }

  protected abstract SubMap<K, V> createSubMap(int initialCapacity, float loadFactor, HashingStrategy<? super K> strategy);

  public RefHashMap(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, HashingStrategy.canonical());
  }

  public RefHashMap(int initialCapacity) {
    this(initialCapacity, 0.8f);
  }

  public RefHashMap() {
    this(4);
  }

  public RefHashMap(@Nonnull Map<? extends K, ? extends V> t) {
    this(Math.max(2 * t.size(), 11), 0.75f);
    putAll(t);
  }

  public RefHashMap(@Nonnull final HashingStrategy<? super K> hashingStrategy) {
    this(4, 0.8f, hashingStrategy);
  }

  public boolean isProcessingQueue() {
    return processingQueue;
  }

  public static <K> boolean keyEqual(K k1, K k2, HashingStrategy<? super K> strategy) {
    return k1 == k2 || strategy.equals(k1, k2);
  }

  public interface Key<T> {
    T get();
  }

  @Nonnull
  protected abstract <T> Key<T> createKey(@Nonnull T k, @Nonnull HashingStrategy<? super T> strategy, @Nonnull ReferenceQueue<? super T> q);

  private class HardKey implements Key<K> {
    private K myObject;
    private int myHash;

    @Override
    public K get() {
      return myObject;
    }

    private void set(@Nonnull K object) {
      myObject = object;
      myHash = myStrategy.hashCode(object);
    }

    private void clear() {
      myObject = null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Key)) return false;
      K t = myObject;
      K u = ((Key<K>)o).get();
      return keyEqual(t, u, myStrategy);
    }

    @Override
    public int hashCode() {
      return myHash;
    }
  }

  // returns true if some refs were tossed
  boolean processQueue() {
    boolean processed = false;
    try {
      processingQueue = true;
      Key<K> wk;
      while ((wk = (Key<K>)myReferenceQueue.poll()) != null) {
        removeKey(wk);
        processed = true;
      }
    }
    finally {
      processingQueue = false;
    }
    myMap.compactIfNecessary();
    return processed;
  }

  V removeKey(@Nonnull Key<K> key) {
    return myMap.remove(key);
  }

  @Nonnull
  Key<K> createKey(@Nonnull K key) {
    return createKey(key, myStrategy, myReferenceQueue);
  }

  V putKey(@Nonnull Key<K> weakKey, V value) {
    return myMap.put(weakKey, value);
  }

  @Override
  public int size() {
    return entrySet().size();
  }

  @Override
  public boolean isEmpty() {
    // make easier and alloc-free call to myMap first
    return myMap.isEmpty() || entrySet().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    if (key == null) return false;
    // optimization:
    myHardKeyInstance.set((K)key);
    boolean result = myMap.containsKey(myHardKeyInstance);
    myHardKeyInstance.clear();
    return result;
  }

  @Override
  public boolean containsValue(Object value) {
    throw RefValueHashMap.pointlessContainsValue();
  }

  @Override
  public V get(Object key) {
    if (key == null) return null;
    //noinspection unchecked
    myHardKeyInstance.set((K)key);
    V result = myMap.get(myHardKeyInstance);
    myHardKeyInstance.clear();
    return result;
  }

  @Override
  public V put(@Nonnull K key, V value) {
    processQueue();
    return putKey(createKey(key), value);
  }

  @Override
  public V remove(@Nonnull Object key) {
    processQueue();

    // optimization:
    //noinspection unchecked
    myHardKeyInstance.set((K)key);
    V result = myMap.remove(myHardKeyInstance);
    myHardKeyInstance.clear();
    return result;
  }

  @Override
  public void clear() {
    processQueue();
    myMap.clear();
  }

  private static class MyEntry<K, V> implements Entry<K, V> {
    private final Entry<?, V> ent;
    private final K key; // Strong reference to key, so that the GC will leave it alone as long as this Entry exists
    private final int myKeyHashCode;
    @Nonnull
    private final HashingStrategy<? super K> myStrategy;

    private MyEntry(@Nonnull Entry<?, V> ent, @Nonnull K key, int keyHashCode, @Nonnull HashingStrategy<? super K> strategy) {
      this.ent = ent;
      this.key = key;
      myKeyHashCode = keyHashCode;
      myStrategy = strategy;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return ent.getValue();
    }

    @Override
    public V setValue(V value) {
      return ent.setValue(value);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Entry)) return false;
      //noinspection unchecked
      Entry<K, V> e = (Entry)o;
      return keyEqual(key, e.getKey(), myStrategy) && Comparing.equal(getValue(), e.getValue());
    }

    @Override
    public int hashCode() {
      V v;
      return myKeyHashCode ^ ((v = getValue()) == null ? 0 : v.hashCode());
    }
  }

  /* Internal class for entry sets */
  private class EntrySet extends AbstractSet<Entry<K, V>> {
    private final Set<Entry<Key<K>, V>> hashEntrySet = myMap.entrySet();

    @Nonnull
    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new Iterator<Entry<K, V>>() {
        private final Iterator<Entry<Key<K>, V>> hashIterator = hashEntrySet.iterator();
        private MyEntry<K, V> next;

        @Override
        public boolean hasNext() {
          while (hashIterator.hasNext()) {
            Entry<Key<K>, V> ent = hashIterator.next();
            Key<K> wk = ent.getKey();
            K k;
            if ((k = wk.get()) == null) {
              // weak key has been cleared by GC, ignore
              continue;
            }
            next = new MyEntry<>(ent, k, wk.hashCode(), myStrategy);
            return true;
          }
          return false;
        }

        @Override
        public Entry<K, V> next() {
          if (next == null && !hasNext()) {
            throw new NoSuchElementException();
          }
          Entry<K, V> e = next;
          next = null;
          return e;
        }

        @Override
        public void remove() {
          hashIterator.remove();
        }
      };
    }

    @Override
    public boolean isEmpty() {
      return !iterator().hasNext();
    }

    @Override
    public int size() {
      int j = 0;
      for (Iterator<Entry<K, V>> i = iterator(); i.hasNext(); i.next()) j++;
      return j;
    }

    @Override
    public boolean remove(Object o) {
      processQueue();
      if (!(o instanceof Entry)) return false;
      //noinspection unchecked
      Entry<K, V> e = (Entry<K, V>)o;
      V ev = e.getValue();

      // optimization: do not recreate the key
      myHardKeyInstance.set(e.getKey());
      Key<K> key = myHardKeyInstance;

      V hv = myMap.get(key);
      boolean toRemove = hv == null ? ev == null && myMap.containsKey(key) : hv.equals(ev);
      if (toRemove) {
        myMap.remove(key);
      }
      myHardKeyInstance.clear();
      return toRemove;
    }

    @Override
    public int hashCode() {
      int h = 0;
      for (Entry<Key<K>, V> entry : hashEntrySet) {
        Key<K> wk = entry.getKey();
        if (wk == null) continue;
        Object v;
        h += wk.hashCode() ^ ((v = entry.getValue()) == null ? 0 : v.hashCode());
      }
      return h;
    }
  }


  @Nonnull
  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> es = entrySet;
    if (es == null) entrySet = es = new EntrySet();
    return es;
  }
}
