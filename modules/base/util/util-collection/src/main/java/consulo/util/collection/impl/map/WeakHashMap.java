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

import consulo.util.collection.HashingStrategy;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Weak keys hash map.
 * Custom TObjectHashingStrategy is supported.
 * Null keys are NOT allowed
 * Null values are allowed
 * <p>
 * Use this class if you need custom HashingStrategy.
 * Do not use this class if you have null keys (shame on you).
 * Otherwise it's the same as java.util.WeakHashMap, you are free to use either.
 */
public abstract class WeakHashMap<K, V> extends RefHashMap<K, V> {
  public WeakHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public WeakHashMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<? super K> strategy) {
    super(initialCapacity, loadFactor, strategy);
  }

  @Nonnull
  @Override
  protected <T> Key<T> createKey(@Nonnull T k, @Nonnull HashingStrategy<? super T> strategy, @Nonnull ReferenceQueue<? super T> q) {
    return new WeakKey<>(k, strategy, q);
  }

  private static class WeakKey<T> extends WeakReference<T> implements Key<T> {
    private final int myHash; // Hashcode of key, stored here since the key may be tossed by the GC
    @Nonnull
    private final HashingStrategy<? super T> myStrategy;

    private WeakKey(@Nonnull T k, @Nonnull HashingStrategy<? super T> strategy, @Nonnull ReferenceQueue<? super T> q) {
      super(k, q);
      myStrategy = strategy;
      myHash = strategy.hashCode(k);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Key)) return false;
      T t = get();
      T u = ((Key<T>)o).get();
      return RefHashMap.keyEqual(t, u, myStrategy);
    }

    @Override
    public int hashCode() {
      return myHash;
    }

    @Override
    public String toString() {
      Object t = get();
      return "WeakKey(" + t + ", " + myHash + ")";
    }
  }
}
