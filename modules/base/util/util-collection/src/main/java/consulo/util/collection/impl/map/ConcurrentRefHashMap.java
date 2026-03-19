// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.collection.impl.map;

import consulo.util.collection.HashingStrategy;

import org.jspecify.annotations.Nullable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class for concurrent (soft/weak) key:K -> strong value:V map
 * Null keys are NOT allowed
 * Null values are NOT allowed
 */
abstract class ConcurrentRefHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, HashingStrategy<K> {
  @FunctionalInterface
  interface KeyReference<K> {
    @Nullable
    K get();

    // In case of gced reference, equality must be identity-based (to be able to remove stale key in processQueue), otherwise it's myHashingStrategy-based
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
  }

  final ReferenceQueue<K> myReferenceQueue = new ReferenceQueue<>();
  private final ConcurrentMap<KeyReference<K>, V> myMap; // hashing strategy must be canonical, we compute corresponding hash codes using our own myHashingStrategy
  private final HashingStrategy<? super K> myHashingStrategy;

  static final float DEFAULT_LOAD_FACTOR = 0.75f;
  static final int DEFAULT_CAPACITY = 16;
  static final int DEFAULT_CONCURRENCY_LEVEL = Math.min(Runtime.getRuntime().availableProcessors(), 4);

  ConcurrentRefHashMap() {
    this(DEFAULT_CAPACITY);
  }

  ConcurrentRefHashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  private ConcurrentRefHashMap(int initialCapacity, float loadFactor) {
    //noinspection unchecked
    this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, null);
  }

  public ConcurrentRefHashMap(HashingStrategy<? super K> hashingStrategy) {
    this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, hashingStrategy);
  }

  ConcurrentRefHashMap(
    int initialCapacity,
    float loadFactor,
    int concurrencyLevel,
    @Nullable HashingStrategy<? super K> hashingStrategy
  ) {
    myHashingStrategy = hashingStrategy == null ? this : hashingStrategy;
    myMap = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
  }

  abstract KeyReference<K> createKeyReference(K key, HashingStrategy<? super K> hashingStrategy);

  private KeyReference<K> createKeyReference(K key) {
    return createKeyReference(Objects.requireNonNull(key), myHashingStrategy);
  }

  // returns true if some keys were processed
  public boolean processQueue() {
    KeyReference<K> wk;
    boolean processed = false;
    //noinspection unchecked
    while ((wk = (KeyReference<K>)myReferenceQueue.poll()) != null) {
      myMap.remove(wk);
      processed = true;
    }
    return processed;
  }

  /**
   * approx value, this method is unreliable anyway because some key could GCed any moment
   */
  @Override
  public int size() {
    return myMap.size();
  }

  @Override
  public boolean isEmpty() {
    // make easier and alloc-free call to myMap first
    return myMap.isEmpty() || entrySet().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    if (myMap.isEmpty()) {
      return false;
    }
    HardKey<K> hardKey = createHardKey(key);
    try {
      return myMap.containsKey(hardKey);
    }
    finally {
      hardKey.clear();
    }
  }

  @Override
  public boolean containsValue(Object value) {
    throw RefValueHashMap.pointlessContainsValue();
  }

  private static class HardKey<K> extends SoftReference<K> implements KeyReference<K> {
    private @Nullable K myKey = null;
    private int myHash = 0;

    private HardKey() {
      super(null, null);
    }

    void setKey(@Nullable K key, int hash) {
      myKey = key;
      myHash = hash;
    }

    @Nullable
    @Override
    public K get() {
      return myKey;
    }

    /**
     * @see ConcurrentSoftHashMap.SoftKey#equals(Object)
     * @see ConcurrentWeakHashMap.WeakKey#equals(Object)
     */
    @SuppressWarnings("EqualsDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
      return o.equals(this);
    }

    @Override
    public int hashCode() {
      return myHash;
    }

    @Override
    public void clear() {
      setKey(null, 0);
    }
  }

  private static final ThreadLocal<HardKey<?>> HARD_KEY = ThreadLocal.withInitial(HardKey::new);

  private HardKey<K> createHardKey(Object o) {
    @SuppressWarnings("unchecked")
    K key = Objects.requireNonNull((K) o);
    @SuppressWarnings("unchecked")
    HardKey<K> hardKey = (HardKey<K>)HARD_KEY.get();
    hardKey.setKey(key, myHashingStrategy.hashCode(key));
    return hardKey;
  }

  @Nullable
  @Override
  public V get(Object key) {
    if (myMap.isEmpty()) {
      return null;
    }
    HardKey<K> hardKey = createHardKey(key);
    try {
      return myMap.get(hardKey);
    }
    finally {
      hardKey.clear();
    }
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    KeyReference<K> weakKey = createKeyReference(key);
    V prev = myMap.put(weakKey, Objects.requireNonNull(value));
    processQueue();
    return prev;
  }

  @Nullable
  @Override
  public V remove(Object key) {
    HardKey<?> hardKey = createHardKey(key);
    try {
      return myMap.remove(hardKey);
    }
    finally {
      processQueue();
      hardKey.clear();
    }
  }

  @Override
  public void clear() {
    myMap.clear();
    processQueue();
  }

  private static final class RefEntry<K, V> implements Map.Entry<K, V> {
    private final Map.Entry<?, V> ent;
    /**
     * Strong reference to key, so that the GC will leave it alone as long as this Entry exists
     */
    private final @Nullable K key;

    RefEntry(Entry<?, V> ent, @Nullable K key) {
      this.ent = ent;
      this.key = key;
    }

    @Nullable
    @Override
    public K getKey() {
      return key;
    }

    @Nullable
    @Override
    public V getValue() {
      return ent.getValue();
    }

    @Nullable
    @Override
    public V setValue(V value) {
      return ent.setValue(value);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) return false;
      @SuppressWarnings("unchecked")
      Map.Entry<K,V> e = (Map.Entry<K,V>)o;
      return Objects.equals(key, e.getKey()) && Objects.equals(getValue(), e.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(getValue());
    }
  }

  /* Internal class for entry sets */
  private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
    private final Set<Map.Entry<KeyReference<K>, V>> hashEntrySet = myMap.entrySet();

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      return new Iterator<>() {
        private final Iterator<Map.Entry<KeyReference<K>, V>> hashIterator = hashEntrySet.iterator();
        private @Nullable RefEntry<K, V> next;

        @Override
        public boolean hasNext() {
          while (hashIterator.hasNext()) {
            Map.Entry<KeyReference<K>, V> ent = hashIterator.next();
            KeyReference<K> wk = ent.getKey();
            K k = null;
            if (wk != null && (k = wk.get()) == null) {
              /* Weak key has been cleared by GC */
              continue;
            }
            next = new RefEntry<>(ent, k);
            return true;
          }
          return false;
        }

        @Override
        public Map.@Nullable Entry<K, V> next() {
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
      for (Entry<KeyReference<K>, V> ent : hashEntrySet) {
        KeyReference<K> wk = ent.getKey();
        if (wk != null && wk.get() == null) {
          continue;
        }
        return false;
      }
      return true;
    }

    @Override
    public int size() {
      int j = 0;
      for (Iterator<Entry<K, V>> i = iterator(); i.hasNext(); i.next()) j++;
      return j;
    }

    @Override
    public boolean remove(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      @SuppressWarnings("unchecked")
      Map.Entry<K, V> e = (Map.Entry<K, V>)o;

      V ev = e.getValue();
      HardKey<K> key = createHardKey(e.getKey());
      try {
        V hv = myMap.get(key);
        boolean toRemove = hv == null ? ev == null && myMap.containsKey(key) : hv.equals(ev);
        if (toRemove) {
          myMap.remove(key);
        }
        processQueue();
        return toRemove;
      }
      finally {
        key.clear();
      }
    }

    @Override
    public int hashCode() {
      int h = 0;
      for (Map.Entry<KeyReference<K>, V> entry : hashEntrySet) {
        KeyReference<K> wk = entry.getKey();
        if (wk == null) continue;
        h += wk.hashCode() ^ Objects.hashCode(entry.getValue());
      }
      return h;
    }
  }

  private @Nullable Set<Map.Entry<K, V>> entrySet = null;

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    Set<Entry<K, V>> es = entrySet;
    if (es == null) entrySet = es = new EntrySet();
    return es;
  }

  @Override
  public V putIfAbsent(K key, V value) {
    V prev = myMap.putIfAbsent(createKeyReference(key), Objects.requireNonNull(value));
    processQueue();
    return prev;
  }

  @Override
  public boolean remove(Object key, Object value) {
    HardKey<K> hardKey = createHardKey(key);
    try {
      boolean removed = myMap.remove(hardKey, Objects.requireNonNull(value));
      processQueue();
      return removed;
    }
    finally {
      hardKey.clear();
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    HardKey<K> hardKey = createHardKey(key);
    try {
      boolean replaced = myMap.replace(hardKey, Objects.requireNonNull(oldValue), Objects.requireNonNull(newValue));
      processQueue();
      return replaced;
    }
    finally {
      hardKey.clear();
    }
  }

  @Override
  public V replace(K key, V value) {
    HardKey<K> hardKey = createHardKey(key);
    try {
      V replaced = myMap.replace(hardKey, Objects.requireNonNull(value));
      processQueue();
      return replaced;
    }
    finally {
      hardKey.clear();
    }
  }

  // MAKE SURE IT CONSISTENT WITH consulo.ide.impl.idea.util.containers.ConcurrentHashMap
  @Override
  public int hashCode(@Nullable K object) {
    int h = object == null ? 0 : object.hashCode();
    h += ~(h << 9);
    h ^= h >>> 14;
    h += h << 4;
    h ^= h >>> 10;
    return h;
  }

  @Override
  public boolean equals(@Nullable K o1, @Nullable K o2) {
    return Objects.equals(o1, o2);
  }
}
