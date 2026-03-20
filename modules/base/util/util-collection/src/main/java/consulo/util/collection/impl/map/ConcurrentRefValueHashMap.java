// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.collection.impl.map;

import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class for concurrent strong key:K -> (soft/weak) value:V map
 * Null keys are NOT allowed
 * Null values are NOT allowed
 */
public abstract class ConcurrentRefValueHashMap<K, V> implements ConcurrentMap<K, V> {
  private final ConcurrentMap<K, ValueReference<K, V>> myMap = new ConcurrentHashMap<>();
  protected final ReferenceQueue<V> myQueue = new ReferenceQueue<>();

  interface ValueReference<K, V> {
    K getKey();

    V get();
  }

  // returns true if some refs were tossed
  public boolean processQueue() {
    boolean processed = false;

    while (true) {
      @SuppressWarnings("unchecked")
      ValueReference<K, V> ref = (ValueReference<K, V>)myQueue.poll();
      if (ref == null) {
        break;
      }
      myMap.remove(ref.getKey(), ref);
      processed = true;
    }
    return processed;
  }

  @Override
  public @Nullable V get(Object key) {
    ValueReference<K, V> ref = myMap.get(key);
    if (ref == null) {
      return null;
    }
    return ref.get();
  }

  @Override
  public @Nullable V put(K key, V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    ValueReference<K, V> oldRef = myMap.put(key, createValueReference(key, value));
    processQueue();
    return oldRef != null ? oldRef.get() : null;
  }

  abstract ValueReference<K, V> createValueReference(K key, V value);

  @Override
  public @Nullable V putIfAbsent(K key, V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    ValueReference<K, V> newRef = createValueReference(key, value);
    while (true) {
      processQueue();
      ValueReference<K, V> oldRef = myMap.putIfAbsent(key, newRef);
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

  @Override
  public boolean remove(Object key, Object value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    @SuppressWarnings("unchecked")
    boolean removed = myMap.remove(key, createValueReference((K)key, (V)value));
    processQueue();
    return removed;
  }

  @Override
  public @Nullable boolean replace(K key, V oldValue, V newValue) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(oldValue);
    Objects.requireNonNull(newValue);
    boolean replaced = myMap.replace(key, createValueReference(key, oldValue), createValueReference(key, newValue));
    processQueue();
    return replaced;
  }

  @Override
  public @Nullable V replace(K key, V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    ValueReference<K, V> ref = myMap.replace(key, createValueReference(key, value));
    processQueue();
    return ref == null ? null : ref.get();
  }

  @Override
  public @Nullable V remove(Object key) {
    ValueReference<K, V> ref = myMap.remove(key);
    processQueue();
    return ref == null ? null : ref.get();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> t) {
    for (Entry<? extends K, ? extends V> entry : t.entrySet()) {
      V v = entry.getValue();
      if (v != null) {
        K key = entry.getKey();
        put(key, v);
      }
    }
    processQueue();
  }

  @Override
  public void clear() {
    myMap.clear();
    processQueue();
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
  public boolean containsKey(Object key) {
    throw RefValueHashMap.pointlessContainsKey();
  }

  @Override
  public boolean containsValue(Object value) {
    throw RefValueHashMap.pointlessContainsValue();
  }

  @Override
  public Set<K> keySet() {
    return myMap.keySet();
  }

  @Override
  public Collection<V> values() {
    Collection<V> result = new ArrayList<>();
    Collection<ValueReference<K, V>> refs = myMap.values();
    for (ValueReference<K, V> ref : refs) {
      V value = ref.get();
      if (value != null) {
        result.add(value);
      }
    }
    return result;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<K> keys = keySet();
    Set<Entry<K, V>> entries = new HashSet<>();

    for (K key : keys) {
      V value = get(key);
      if (value != null) {
        entries.add(new Entry<>() {
          @Override
          public K getKey() {
            return key;
          }

          @Override
          public V getValue() {
            return value;
          }

          @Override
          public V setValue(V value) {
            throw new UnsupportedOperationException("setValue is not implemented");
          }

          @Override
          public String toString() {
            return "(" + getKey() + " : " + getValue() + ")";
          }
        });
      }
    }

    return entries;
  }

  @Override
  public String toString() {
    return "map size:" + size() + " [" + StringUtil.join(entrySet(), ",") + "]";
  }
}
