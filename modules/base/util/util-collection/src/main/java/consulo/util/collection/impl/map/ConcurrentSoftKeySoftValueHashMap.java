// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.collection.impl.map;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import org.jspecify.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Objects;

/**
 * Concurrent map with soft keys and soft values.
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * To create, use {@link Maps#newConcurrentSoftKeySoftValueHashMap()}
 */
public class ConcurrentSoftKeySoftValueHashMap<K, V> extends ConcurrentWeakKeySoftValueHashMap<K, V> {
  private static class SoftKey<K, V> extends SoftReference<K> implements KeyReference<K, V> {
    private final int myHash; // Hash code of the key, stored here since the key may be tossed by the GC
    private final HashingStrategy<? super K> myStrategy;
    private final ValueReference<K, V> myValueReference;

    SoftKey(K k, ValueReference<K, V> valueReference, HashingStrategy<? super K> strategy, ReferenceQueue<? super K> queue) {
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
      K other = ((KeyReference<K, V>)o).get();
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

  public ConcurrentSoftKeySoftValueHashMap(
    int initialCapacity,
    float loadFactor,
    int concurrencyLevel,
    HashingStrategy<? super K> hashingStrategy
  ) {
    super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Override
  public KeyReference<K, V> createKeyReference(K k, V v) {
    ValueReference<K, V> valueReference = createValueReference(v, myValueQueue);
    SoftKey<K, V> keyReference = new SoftKey<>(k, valueReference, myHashingStrategy, myKeyQueue);
    if (valueReference instanceof SoftValue softValue) {
      softValue.myKeyReference = keyReference;
    }
    return keyReference;
  }
}
