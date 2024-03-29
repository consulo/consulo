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
import consulo.util.collection.Maps;
import consulo.util.lang.Comparing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class for concurrent (soft/weak) key:K -> strong value:V map
 * Null keys are allowed
 * Null values are NOT allowed
 */
public abstract class ConcurrentRefHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, HashingStrategy<K> {
  final ReferenceQueue<K> myReferenceQueue = new ReferenceQueue<>();
  private final ConcurrentMap<KeyReference<K>, V> myMap; // hashing strategy must be canonical, we compute corresponding hash codes using our own myHashingStrategy
  @Nonnull
  private final HashingStrategy<? super K> myHashingStrategy;

  @FunctionalInterface
  interface KeyReference<K> {
    K get();

    // In case of gced reference, equality must be identity-based (to be able to remove stale key in processQueue), otherwise it's myHashingStrategy-based
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
  }

  @Nonnull
  abstract KeyReference<K> createKeyReference(@Nonnull K key, @Nonnull HashingStrategy<? super K> hashingStrategy);

  private static final HardKey<?> NULL_KEY = new HardKey<Object>() {
    @Override
    public Object get() {
      return null;
    }

    @Override
    void setKey(Object key, int hash) {
    }
  };

  @Nonnull
  private KeyReference<K> createKeyReference(@Nullable K key) {
    if (key == null) {
      //noinspection unchecked
      return (KeyReference<K>)NULL_KEY;
    }
    return createKeyReference(key, myHashingStrategy);
  }

  // returns true if some keys were processed
  boolean processQueue() {
    KeyReference<K> wk;
    boolean processed = false;
    //noinspection unchecked
    while ((wk = (KeyReference<K>)myReferenceQueue.poll()) != null) {
      myMap.remove(wk);
      processed = true;
    }
    return processed;
  }

  private static final float LOAD_FACTOR = 0.75f;
  static final int DEFAULT_CAPACITY = 16;
  static final int DEFAULT_CONCURRENCY_LEVEL = Math.min(Runtime.getRuntime().availableProcessors(), 4);

  ConcurrentRefHashMap() {
    this(DEFAULT_CAPACITY);
  }

  ConcurrentRefHashMap(int initialCapacity) {
    this(initialCapacity, LOAD_FACTOR);
  }

  private static final HashingStrategy<?> THIS = new HashingStrategy<Object>() {
    @Override
    public int hashCode(Object object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o1, Object o2) {
      throw new UnsupportedOperationException();
    }
  };

  private ConcurrentRefHashMap(int initialCapacity, float loadFactor) {
    //noinspection unchecked
    this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, (HashingStrategy<? super K>)THIS);
  }

  public ConcurrentRefHashMap(@Nonnull final HashingStrategy<? super K> hashingStrategy) {
    this(DEFAULT_CAPACITY, LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, hashingStrategy);
  }

  public ConcurrentRefHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull HashingStrategy<? super K> hashingStrategy) {
    myHashingStrategy = hashingStrategy == THIS ? this : hashingStrategy;
    myMap = Maps.newConcurrentHashMap(initialCapacity, loadFactor, concurrencyLevel);
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
  public boolean containsKey(@Nullable Object key) {
    HardKey<K> hardKey = createHardKey(key);
    boolean result = myMap.containsKey(hardKey);
    releaseHardKey(hardKey);
    return result;
  }

  @Override
  public boolean containsValue(Object value) {
    throw RefValueHashMap.pointlessContainsValue();
  }

  private static class HardKey<K> implements KeyReference<K> {
    private K myKey;
    private int myHash;

    void setKey(K key, final int hash) {
      myKey = key;
      myHash = hash;
    }

    @Override
    public K get() {
      return myKey;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
      return o.equals(this); // see consulo.ide.impl.idea.util.containers.ConcurrentSoftHashMap.SoftKey or consulo.ide.impl.idea.util.containers.ConcurrentWeakHashMap.WeakKey
    }

    @Override
    public int hashCode() {
      return myHash;
    }
  }

  private static final ThreadLocal<HardKey<?>> HARD_KEY = ThreadLocal.withInitial(HardKey::new);

  @Nonnull
  private HardKey<K> createHardKey(@Nullable Object o) {
    if (o == null) {
      //noinspection unchecked
      return (HardKey<K>)NULL_KEY;
    }
    //noinspection unchecked
    K key = (K)o;
    //noinspection unchecked
    HardKey<K> hardKey = (HardKey<K>)HARD_KEY.get();
    hardKey.setKey(key, myHashingStrategy.hashCode(key));
    return hardKey;
  }

  private static void releaseHardKey(@Nonnull HardKey<?> key) {
    key.setKey(null, 0);
  }

  @Override
  public V get(@Nullable Object key) {
    HardKey<K> hardKey = createHardKey(key);
    V result = myMap.get(hardKey);
    releaseHardKey(hardKey);
    return result;
  }

  @Override
  public V put(@Nullable K key, @Nonnull V value) {
    processQueue();
    KeyReference<K> weakKey = createKeyReference(key);
    return myMap.put(weakKey, value);
  }

  @Override
  public V remove(@Nullable Object key) {
    processQueue();

    HardKey<?> hardKey = createHardKey(key);
    V result = myMap.remove(hardKey);
    releaseHardKey(hardKey);
    return result;
  }

  @Override
  public void clear() {
    processQueue();
    myMap.clear();
  }

  private static class RefEntry<K, V> implements Map.Entry<K, V> {
    private final Map.Entry<?, V> ent;
    private final K key; /* Strong reference to key, so that the GC
                                 will leave it alone as long as this Entry
                                 exists */

    RefEntry(@Nonnull Map.Entry<?, V> ent, @Nullable K key) {
      this.ent = ent;
      this.key = key;
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
    public V setValue(@Nonnull V value) {
      return ent.setValue(value);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) return false;
      //noinspection unchecked
      Map.Entry<K, V> e = (Map.Entry<K, V>)o;
      return Comparing.equal(key, e.getKey()) && Comparing.equal(getValue(), e.getValue());
    }

    @Override
    public int hashCode() {
      Object v;
      return (key == null ? 0 : key.hashCode()) ^ ((v = getValue()) == null ? 0 : v.hashCode());
    }
  }

  /* Internal class for entry sets */
  private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
    private final Set<Map.Entry<KeyReference<K>, V>> hashEntrySet = myMap.entrySet();

    @Nonnull
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      return new Iterator<Map.Entry<K, V>>() {
        private final Iterator<Map.Entry<KeyReference<K>, V>> hashIterator = hashEntrySet.iterator();
        private RefEntry<K, V> next;

        @Override
        public boolean hasNext() {
          while (hashIterator.hasNext()) {
            Map.Entry<KeyReference<K>, V> ent = hashIterator.next();
            KeyReference<K> wk = ent.getKey();
            K k = null;
            if (wk != null && (k = wk.get()) == null && wk != NULL_KEY) {
              /* Weak key has been cleared by GC */
              continue;
            }
            next = new RefEntry<>(ent, k);
            return true;
          }
          return false;
        }

        @Override
        public Map.Entry<K, V> next() {
          if (next == null && !hasNext()) {
            throw new NoSuchElementException();
          }
          RefEntry<K, V> e = next;
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
      if (!(o instanceof Map.Entry)) return false;
      //noinspection unchecked
      Map.Entry<K, V> e = (Map.Entry<K, V>)o;
      V ev = e.getValue();

      HardKey<K> key = createHardKey(e.getKey());

      V hv = myMap.get(key);
      boolean toRemove = hv == null ? ev == null && myMap.containsKey(key) : hv.equals(ev);
      if (toRemove) {
        myMap.remove(key);
      }

      releaseHardKey(key);
      return toRemove;
    }

    @Override
    public int hashCode() {
      int h = 0;
      for (Map.Entry<KeyReference<K>, V> entry : hashEntrySet) {
        KeyReference<K> wk = entry.getKey();
        if (wk == null) continue;
        Object v;
        h += wk.hashCode() ^ ((v = entry.getValue()) == null ? 0 : v.hashCode());
      }
      return h;
    }
  }

  private Set<Map.Entry<K, V>> entrySet;

  @Nonnull
  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    Set<Entry<K, V>> es = entrySet;
    if (es == null) entrySet = es = new EntrySet();
    return es;
  }

  @Override
  public V putIfAbsent(@Nullable final K key, @Nonnull V value) {
    processQueue();
    return myMap.putIfAbsent(createKeyReference(key), value);
  }

  @Override
  public boolean remove(@Nullable final Object key, @Nonnull Object value) {
    processQueue();
    //noinspection unchecked
    return myMap.remove(createKeyReference((K)key), value);
  }

  @Override
  public boolean replace(@Nullable final K key, @Nonnull final V oldValue, @Nonnull final V newValue) {
    processQueue();
    return myMap.replace(createKeyReference(key), oldValue, newValue);
  }

  @Override
  public V replace(@Nullable final K key, @Nonnull final V value) {
    processQueue();
    return myMap.replace(createKeyReference(key), value);
  }

  // MAKE SURE IT CONSISTENT WITH consulo.ide.impl.idea.util.containers.ConcurrentHashMap
  @Override
  public int hashCode(final K object) {
    int h = object.hashCode();
    h += ~(h << 9);
    h ^= h >>> 14;
    h += h << 4;
    h ^= h >>> 10;
    return h;
  }

  @Override
  public boolean equals(final K o1, final K o2) {
    return o1.equals(o2);
  }
}
