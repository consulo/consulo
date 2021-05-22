/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import java.lang.ref.SoftReference;

/**
 * Concurrent map with soft keys and soft values.
 * Null keys are NOT allowed
 * Null values are NOT allowed
 * To instantiate use {@link ContainerUtil#createConcurrentSoftKeySoftValueMap(int, float, int, TObjectHashingStrategy)}
 */
public class ConcurrentSoftKeySoftValueHashMap<K, V> extends ConcurrentWeakKeySoftValueHashMap<K, V> {
  public ConcurrentSoftKeySoftValueHashMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final HashingStrategy<? super K> hashingStrategy) {
    super(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  private static class SoftKey<K, V> extends SoftReference<K> implements KeyReference<K, V> {
    private final int myHash; // Hash code of the key, stored here since the key may be tossed by the GC
    private final HashingStrategy<? super K> myStrategy;
    @Nonnull
    private final ValueReference<K, V> myValueReference;

    SoftKey(@Nonnull K k, @Nonnull ValueReference<K, V> valueReference, @Nonnull HashingStrategy<? super K> strategy, @Nonnull ReferenceQueue<? super K> queue) {
      super(k, queue);
      myValueReference = valueReference;
      myHash = strategy.hashCode(k);
      myStrategy = strategy;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof KeyReference)) return false;
      K t = get();
      K other = ((KeyReference<K, V>)o).get();
      if (t == null || other == null) return false;
      if (t == other) return true;
      return myHash == o.hashCode() && myStrategy.equals(t, other);
    }

    @Override
    public int hashCode() {
      return myHash;
    }

    @Nonnull
    @Override
    public ValueReference<K, V> getValueReference() {
      return myValueReference;
    }
  }

  @Override
  @Nonnull
  public KeyReference<K, V> createKeyReference(@Nonnull K k, @Nonnull final V v) {
    final ValueReference<K, V> valueReference = createValueReference(v, myValueQueue);
    SoftKey<K, V> keyReference = new SoftKey<>(k, valueReference, myHashingStrategy, myKeyQueue);
    if (valueReference instanceof SoftValue) {
      ((SoftValue)valueReference).myKeyReference = keyReference;
    }
    return keyReference;
  }
}
