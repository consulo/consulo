// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.collection.impl.map;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import org.jspecify.annotations.Nullable;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Concurrent map with weak keys and soft values.
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * To create, use {@link Maps#newConcurrentWeakKeySoftValueHashMap()}
 */
public class ConcurrentWeakKeySoftValueHashMap<K, V> implements ConcurrentMap<K, V> {
  private final ConcurrentMap<KeyReference<K, V>, ValueReference<K, V>> myMap;
  final ReferenceQueue<K> myKeyQueue = new ReferenceQueue<>();
  final ReferenceQueue<V> myValueQueue = new ReferenceQueue<>();
  final HashingStrategy<? super K> myHashingStrategy;

  public ConcurrentWeakKeySoftValueHashMap(
      int initialCapacity,
      float loadFactor,
      int concurrencyLevel,
      HashingStrategy<? super K> hashingStrategy
  ) {
    myHashingStrategy = hashingStrategy;
    ConcurrentHashMap<KeyReference<K, V>, ValueReference<K, V>> map =
        new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    myMap = map;
  }

  public interface KeyReference<K, V> extends Supplier<K> {
    @Override
    @Nullable K get();

    ValueReference<K, V> getValueReference(); // no strong references

    // MUST work even with gced references for the code in processQueue to work
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
  }

  public interface ValueReference<K, V> extends Supplier<V> {
    @Nullable KeyReference<K, V> getKeyReference(); // no strong references

    @Override
    V get();
  }

  static final class WeakKey<K, V> extends WeakReference<K> implements KeyReference<K, V> {
    private final int myHash; // Hash code of the key, stored here since the key may be tossed by the GC
    private final HashingStrategy<? super K> myStrategy;
    private final ValueReference<K, V> myValueReference;

    WeakKey(K k, ValueReference<K, V> valueReference, HashingStrategy<? super K> strategy, ReferenceQueue<? super K> queue) {
      super(Objects.requireNonNull(k), queue);
      myValueReference = valueReference;
      myHash = strategy.hashCode(k);
      myStrategy = strategy;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof KeyReference)) {
        return false;
      }
      K t = get();
      @SuppressWarnings("unchecked")
      K other = ((KeyReference<K, V>) o).get();
      if (t == null || other == null) {
        return false;
      }
      if (t == other) {
        return true;
      }
      return myHash == o.hashCode() && myStrategy.equals(t, other);
    }

    @Override
    public int hashCode() {
      return myHash;
    }

    @Override
    public ValueReference<K, V> getValueReference() {
      return myValueReference;
    }
  }

  static final class SoftValue<K, V> extends SoftReference<V> implements ValueReference<K, V> {
    // can't make it final because of circular dependency of KeyReference to ValueReference
    volatile @Nullable KeyReference<K, V> myKeyReference = null;

    private SoftValue(V value, ReferenceQueue<? super V> queue) {
      super(Objects.requireNonNull(value), queue);
    }

    // When referent is collected, equality should be identity-based (for the processQueues() remove this very same SoftValue)
    // otherwise it's just canonical equals on referents for replace(K,V,V) to work
    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }

      V v = get();
      Object thatV = ((ValueReference<K, V>)o).get();
      return v != null && v.equals(thatV);
    }

    @Override
    public @Nullable KeyReference<K, V> getKeyReference() {
      return myKeyReference;
    }
  }

  KeyReference<K, V> createKeyReference(K k, V v) {
    ValueReference<K, V> valueReference = createValueReference(v, myValueQueue);
    KeyReference<K, V> keyReference = new WeakKey<>(k, valueReference, myHashingStrategy, myKeyQueue);
    if (valueReference instanceof SoftValue) {
      ((SoftValue<K, V>) valueReference).myKeyReference = keyReference;
    }
    // to avoid queueing in myValueQueue before setting its myKeyReference to not-null value
    Reference.reachabilityFence(k);
    Reference.reachabilityFence(v);
    return keyReference;
  }

  protected ValueReference<K, V> createValueReference(V value, ReferenceQueue<? super V> queue) {
    return new SoftValue<>(value, queue);
  }

  @Override
  public int size() {
    return myMap.size();
  }

  @Override
  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  @Override
  public void clear() {
    myMap.clear();
    processQueue();
  }

  /////////////////////////////
  private static class HardKey<K, V> extends PhantomReference<K> implements KeyReference<K, V> {
    private @Nullable K myKey = null;
    private int myHash;

    HardKey() {
      super(null, null);
    }

    private void set(K key, int hash) {
      myKey = key;
      myHash = hash;
    }

    @Override
    public void clear() {
      myKey = null;
    }

    @Override
    public @Nullable K get() {
      return myKey;
    }

    /**
     * @see WeakKey#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
      return o.equals(this);
    }

    @Override
    public int hashCode() {
      return myHash;
    }

    @Override
    public ValueReference<K, V> getValueReference() {
      throw new UnsupportedOperationException();
    }
  }

  private static final ThreadLocal<HardKey<?, ?>> HARD_KEY = ThreadLocal.withInitial(HardKey::new);

  private HardKey<K, V> createHardKey(Object o) {
    @SuppressWarnings("unchecked")
    K key = Objects.requireNonNull((K) o);
    @SuppressWarnings("unchecked")
    HardKey<K, V> hardKey = (HardKey<K, V>) HARD_KEY.get();
    hardKey.set(key, myHashingStrategy.hashCode(key));
    return hardKey;
  }

///////////////////////////////////

  @Override
  public @Nullable V get(Object key) {
    HardKey<K, V> hardKey = createHardKey(key);
    try {
      ValueReference<K, V> valueReference = myMap.get(hardKey);
      return valueReference == null ? null : valueReference.get();
    }
    finally {
      hardKey.clear();
    }
  }

  @Override
  public boolean containsKey(Object key) {
    throw RefValueHashMap.pointlessContainsKey();
  }

  @Override
  public boolean containsValue(Object value) {
    throw RefValueHashMap.pointlessContainsValue();
  }

  @Override
  public @Nullable V remove(Object key) {
    HardKey<K, V> hardKey = createHardKey(key);
    try {
      ValueReference<K, V> valueReference = myMap.remove(hardKey);
      return valueReference == null ? null : valueReference.get();
    }
    finally {
      hardKey.clear();
      processQueue();
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public @Nullable V put(K key, V value) {
    KeyReference<K, V> keyReference = createKeyReference(key, value);
    ValueReference<K, V> valueReference = keyReference.getValueReference();
    ValueReference<K, V> prevValReference = myMap.put(keyReference, valueReference);
    processQueue();
    return prevValReference == null ? null : prevValReference.get();
  }

  public boolean processQueue() {
    boolean removed = false;
    KeyReference<K, V> keyReference;
    //noinspection unchecked
    while ((keyReference = (KeyReference<K, V>) myKeyQueue.poll()) != null) {
      ValueReference<K, V> valueReference = keyReference.getValueReference();
      removed |= myMap.remove(keyReference, valueReference);
    }

    ValueReference<K, V> valueReference;
    //noinspection unchecked
    while ((valueReference = (ValueReference<K, V>) myValueQueue.poll()) != null) {
      keyReference = valueReference.getKeyReference();
      // keyReference could be null when createValueReference() was called and abandoned immediately, e.g. in replace(K, V)
      // in this case just ignore this ref, it's not in the map anyway
      if (keyReference != null) {
        removed |= myMap.remove(keyReference, valueReference);
      }
    }

    return removed;
  }

  @Override
  public Set<K> keySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<V> values() {
    List<V> values = new ArrayList<>();
    for (ValueReference<K, V> valueReference : myMap.values()) {
      V v = valueReference == null ? null : valueReference.get();
      if (v != null) {
        values.add(v);
      }
    }
    return values;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable boolean remove(Object key, Object value) {
    Objects.requireNonNull(value);
    HardKey<K, V> hardKey = createHardKey(key);
    try {
      ValueReference<K, V> valueReference = myMap.get(hardKey);
      V v = valueReference == null ? null : valueReference.get();
      return value.equals(v) && myMap.remove(hardKey, valueReference);
    }
    finally {
      hardKey.clear();
      processQueue();
    }
  }

  @Override
  public @Nullable V putIfAbsent(K key, V value) {
    KeyReference<K, V> keyRef = createKeyReference(key, value);
    ValueReference<K, V> newRef = keyRef.getValueReference();
    V prev;
    while (true) {
      ValueReference<K, V> oldRef = myMap.putIfAbsent(keyRef, newRef);
      if (oldRef == null) {
        prev = null;
        break;
      }
      final V oldVal = oldRef.get();
      if (oldVal == null) {
        if (myMap.replace(keyRef, oldRef, newRef)) {
          prev = null;
          break;
        }
      }
      else {
        prev = oldVal;
        break;
      }
      processQueue();
    }
    processQueue();
    return prev;
  }

  @Override
  public @Nullable boolean replace(K key, V oldValue, V newValue) {
    HardKey<K, V> oldKeyReference = createHardKey(key);
    ValueReference<K, V> oldValueReference;
    try {
      oldValueReference = createValueReference(oldValue, myValueQueue);
      ValueReference<K, V> newValueReference = createValueReference(newValue, myValueQueue);

      boolean replaced = myMap.replace(oldKeyReference, oldValueReference, newValueReference);
      processQueue();
      return replaced;
    }
    finally {
      oldKeyReference.clear();
      // we must not let these values got into a ref queue while performing operations with them
      Reference.reachabilityFence(oldValue);
      Reference.reachabilityFence(newValue);
    }
  }

  @Override
  public @Nullable V replace(K key, V value) {
    HardKey<K, V> keyReference = createHardKey(key);
    try {
      ValueReference<K, V> valueReference = createValueReference(value, myValueQueue);
      ValueReference<K, V> result = myMap.replace(keyReference, valueReference);
      V prev = result == null ? null : result.get();
      processQueue();
      return prev;
    }
    finally {
      keyReference.clear();
      // we must not let these values got into a ref queue while performing operations with them
      Reference.reachabilityFence(value);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }
}
