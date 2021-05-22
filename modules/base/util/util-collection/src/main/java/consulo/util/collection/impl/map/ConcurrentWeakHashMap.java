/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Concurrent weak key:K -> strong value:V map.
 * Null keys are allowed
 * Null values are NOT allowed
 */
public final class ConcurrentWeakHashMap<K, V> extends ConcurrentRefHashMap<K, V> {
  private static class WeakKey<K> extends WeakReference<K> implements KeyReference<K> {
    private final int myHash; /* Hashcode of key, stored here since the key may be tossed by the GC */
    @Nonnull
    private final HashingStrategy<? super K> myStrategy;

    private WeakKey(@Nonnull K k, final int hash, @Nonnull HashingStrategy<? super K> strategy, @Nonnull ReferenceQueue<K> q) {
      super(k, q);
      myStrategy = strategy;
      myHash = hash;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof KeyReference)) return false;
      K t = get();
      //noinspection unchecked
      K u = ((KeyReference<K>)o).get();
      if (t == null || u == null) return false;
      return t == u || myStrategy.equals(t, u);
    }

    @Override
    public int hashCode() {
      return myHash;
    }
  }

  @Nonnull
  @Override
  protected KeyReference<K> createKeyReference(@Nonnull K key, @Nonnull HashingStrategy<? super K> hashingStrategy) {
    return new WeakKey<>(key, hashingStrategy.hashCode(key), hashingStrategy, myReferenceQueue);
  }

  public ConcurrentWeakHashMap(float loadFactor) {
    this(ConcurrentRefHashMap.DEFAULT_CAPACITY, loadFactor, ConcurrentRefHashMap.DEFAULT_CONCURRENCY_LEVEL, HashingStrategy.canonical());
  }

  public ConcurrentWeakHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull HashingStrategy<? super K> hashingStrategy) {
    super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  public ConcurrentWeakHashMap(@Nonnull HashingStrategy<? super K> hashingStrategy) {
    super(hashingStrategy);
  }
}
