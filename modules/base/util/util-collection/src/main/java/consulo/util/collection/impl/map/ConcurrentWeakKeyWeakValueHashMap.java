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
import consulo.util.collection.HashingStrategy;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Concurrent map with weak keys and weak values.
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * Use {@link ContainerUtil#createConcurrentWeakKeyWeakValueMap()} to create this
 */
public class ConcurrentWeakKeyWeakValueHashMap<K, V> extends ConcurrentWeakKeySoftValueHashMap<K, V> {
  public ConcurrentWeakKeyWeakValueHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final HashingStrategy<? super K> hashingStrategy) {
    super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  private static class WeakValue<K, V> extends WeakReference<V> implements ValueReference<K, V> {
    @Nonnull
    private volatile KeyReference<K, V> myKeyReference; // can't make it final because of circular dependency of KeyReference to ValueReference

    private WeakValue(@Nonnull V value, @Nonnull ReferenceQueue<? super V> queue) {
      super(value, queue);
    }

    // When referent is collected, equality should be identity-based (for the processQueues() remove this very same SoftValue)
    // otherwise it's just canonical equals on referents for replace(K,V,V) to work
    @Override
    public final boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null) return false;

      V v = get();
      Object thatV = ((ValueReference)o).get();
      return v != null && thatV != null && v.equals(thatV);
    }

    @Nonnull
    @Override
    public KeyReference<K, V> getKeyReference() {
      return myKeyReference;
    }
  }

  @Override
  @Nonnull
  public KeyReference<K, V> createKeyReference(@Nonnull K k, @Nonnull final V v) {
    final ValueReference<K, V> valueReference = createValueReference(v, myValueQueue);
    WeakKey<K, V> keyReference = new WeakKey<>(k, valueReference, myHashingStrategy, myKeyQueue);
    if (valueReference instanceof WeakValue) {
      ((WeakValue)valueReference).myKeyReference = keyReference;
    }
    return keyReference;
  }

  @Override
  @Nonnull
  protected ValueReference<K, V> createValueReference(@Nonnull V value, @Nonnull ReferenceQueue<? super V> queue) {
    return new WeakValue<>(value, queue);
  }
}
