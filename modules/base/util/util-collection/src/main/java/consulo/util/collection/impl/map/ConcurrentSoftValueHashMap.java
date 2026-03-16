// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.collection.impl.map;

import consulo.util.collection.Maps;
import org.jspecify.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * Concurrent strong key:K -> soft value:V map
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * To create, use {@link Maps#newConcurrentSoftValueHashMap()}
 */
public final class ConcurrentSoftValueHashMap<K, V> extends ConcurrentRefValueHashMap<K, V> {
  private static class MySoftReference<K, V> extends SoftReference<V> implements ValueReference<K, V> {
    private final K key;

    private MySoftReference(K key, V referent, ReferenceQueue<V> q) {
      super(referent, q);
      this.key = key;
    }

    @Override
    public K getKey() {
      return key;
    }

    // When referent is collected, equality should be identity-based (for the processQueues() remove this very same SoftValue)
    // otherwise it's just canonical equals on referents for replace(K,V,V) to work
    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      @SuppressWarnings("unchecked")
      ValueReference<K, V> that = (ValueReference<K, V>) o;

      V v = get();
      V thatV = that.get();
      return key.equals(that.getKey()) && v != null && v.equals(thatV);
    }
  }

  @Override
  ValueReference<K, V> createValueReference(K key, V value) {
    return new MySoftReference<>(key, value, myQueue);
  }
}
