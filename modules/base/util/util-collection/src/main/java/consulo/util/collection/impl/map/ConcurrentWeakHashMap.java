// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.collection.impl.map;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import org.jspecify.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Concurrent weak key:K -> strong value:V map.
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * To create, use {@link Maps#newConcurrentWeakHashMap()}
 */
public final class ConcurrentWeakHashMap<K, V> extends ConcurrentRefHashMap<K, V> {
  private static final class WeakKey<K> extends WeakReference<K> implements KeyReference<K> {
    private final int myHash; /* Hashcode of key, stored here since the key may be tossed by the GC */
    private final HashingStrategy<? super K> myStrategy;

    private WeakKey(K k, int hash, HashingStrategy<? super K> strategy, ReferenceQueue<K> q) {
      super(k, q);
      myStrategy = strategy;
      myHash = hash;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof KeyReference)) {
        return false;
      }
      K key = get();
      @SuppressWarnings("unchecked")
      K otherKey = ((KeyReference<K>) o).get();
      if (key == null || otherKey == null) {
        return false;
      }
      return key == otherKey || myStrategy.equals(key, otherKey);
    }

    @Override
    public int hashCode() {
      return myHash;
    }
  }

  public ConcurrentWeakHashMap(float loadFactor) {
    this(ConcurrentRefHashMap.DEFAULT_CAPACITY, loadFactor, ConcurrentRefHashMap.DEFAULT_CONCURRENCY_LEVEL, HashingStrategy.canonical());
  }

  public ConcurrentWeakHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, HashingStrategy<? super K> hashingStrategy) {
    super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  public ConcurrentWeakHashMap(HashingStrategy<? super K> hashingStrategy) {
    super(hashingStrategy);
  }

  @Override
  protected KeyReference<K> createKeyReference(K key, HashingStrategy<? super K> hashingStrategy) {
    return new WeakKey<>(key, hashingStrategy.hashCode(key), hashingStrategy, myReferenceQueue);
  }
}
