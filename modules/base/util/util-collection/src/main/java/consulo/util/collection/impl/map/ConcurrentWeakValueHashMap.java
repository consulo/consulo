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

import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Concurrent strong key:K -> weak value:V map
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * Use {@link ContainerUtil#createConcurrentWeakValueMap()} to create this
 */
public final class ConcurrentWeakValueHashMap<K, V> extends ConcurrentRefValueHashMap<K, V> {
  private static class MyWeakReference<K, V> extends WeakReference<V> implements ValueReference<K, V> {
    private final K key;

    private MyWeakReference(@Nonnull K key, @Nonnull V referent, @Nonnull ReferenceQueue<V> q) {
      super(referent, q);
      this.key = key;
    }

    @Nonnull
    @Override
    public K getKey() {
      return key;
    }

    // When referent is collected, equality should be identity-based (for the processQueues() remove this very same SoftValue)
    // otherwise it's just canonical equals on referents for replace(K,V,V) to work
    public final boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      @SuppressWarnings("unchecked") ValueReference<K, V> that = (ValueReference)o;

      V v = get();
      V thatV = that.get();
      return key.equals(that.getKey()) && v != null && thatV != null && v.equals(thatV);
    }
  }

  @Nonnull
  @Override
  public ValueReference<K, V> createValueReference(@Nonnull K key, @Nonnull V value) {
    return new MyWeakReference<>(key, value, myQueue);
  }
}
