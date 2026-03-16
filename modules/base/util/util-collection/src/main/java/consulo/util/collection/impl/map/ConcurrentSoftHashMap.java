// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.collection.impl.map;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * Concurrent soft key:K -> strong value:V map
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * To create, use {@link Maps#newConcurrentSoftHashMap()}
 */
public final class ConcurrentSoftHashMap<K, V> extends ConcurrentRefHashMap<K, V> {
  private static class SoftKey<K> extends SoftReference<K> implements KeyReference<K> {
    private final int myHash; // Hashcode of key, stored here since the key may be tossed by the GC
    private final HashingStrategy<? super K> myStrategy;

    private SoftKey(K k, int hash, HashingStrategy<? super K> strategy, ReferenceQueue<K> q) {
      super(k, q);
      myStrategy = strategy;
      myHash = hash;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof KeyReference)) return false;
      K key = get();
      @SuppressWarnings("unchecked")
      K otherKey = ((KeyReference<K>)o).get();
      if (key == otherKey) return true;
      if (key == null || otherKey == null) return false;
      return myStrategy.equals(key, otherKey);
    }

    @Override
    public int hashCode() {
      return myHash;
    }
  }

  public ConcurrentSoftHashMap() {
  }

  public ConcurrentSoftHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, HashingStrategy<? super K> hashingStrategy) {
    super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Override
  protected KeyReference<K> createKeyReference(K key, HashingStrategy<? super K> hashingStrategy) {
    return new SoftKey<>(key, hashingStrategy.hashCode(key), hashingStrategy, myReferenceQueue);
  }
}
