// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.collection.impl.map;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import org.jspecify.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Concurrent map with weak keys and weak values.
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * To create, use {@link Maps#newConcurrentWeakKeyWeakValueHashMap()}
 */
public class ConcurrentWeakKeyWeakValueHashMap<K, V> extends ConcurrentWeakKeySoftValueHashMap<K, V> {
  private static final class WeakValue<K, V> extends WeakReference<V> implements ValueReference<K, V> {
    @Nullable
    private volatile KeyReference<K, V> myKeyReference = null; // can't make it final because of circular dependency of KeyReference to ValueReference

    private WeakValue(V value, ReferenceQueue<? super V> queue) {
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
      @SuppressWarnings("unchecked")
      V thatV = ((ValueReference<K, V>) o).get();
      return v != null && v.equals(thatV);
    }

    @Nullable
    @Override
    public KeyReference<K, V> getKeyReference() {
      return myKeyReference;
    }
  }

  public ConcurrentWeakKeyWeakValueHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, HashingStrategy<? super K> hashingStrategy) {
    super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Override
  KeyReference<K, V> createKeyReference(K k, V v) {
    ValueReference<K, V> valueReference = createValueReference(v, myValueQueue);
    WeakKey<K, V> keyReference = new WeakKey<>(k, valueReference, myHashingStrategy, myKeyQueue);
    if (valueReference instanceof WeakValue) {
      ((WeakValue<K, V>)valueReference).myKeyReference = keyReference;
    }
    // to avoid queueing in myValueQueue before setting its myKeyReference to not-null value
    Reference.reachabilityFence(k);
    Reference.reachabilityFence(v);
    return keyReference;
  }

  @Override
  protected ValueReference<K, V> createValueReference(V value, ReferenceQueue<? super V> queue) {
    return new WeakValue<>(value, queue);
  }
}
