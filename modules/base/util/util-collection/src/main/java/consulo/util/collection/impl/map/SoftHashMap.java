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
import consulo.util.lang.ref.SoftReference;

import javax.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;

/**
 * Soft keys hash map.
 * Null keys are NOT allowed
 * Null values are allowed
 */
public abstract class SoftHashMap<K, V> extends RefHashMap<K, V> {
  public SoftHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public SoftHashMap(@Nonnull HashingStrategy<? super K> hashingStrategy) {
    super(hashingStrategy);
  }

  @Nonnull
  @Override
  protected <T> Key<T> createKey(@Nonnull T k, @Nonnull HashingStrategy<? super T> strategy, @Nonnull ReferenceQueue<? super T> q) {
    return new SoftKey<>(k, strategy, q);
  }

  private static class SoftKey<T> extends SoftReference<T> implements Key<T> {
    private final int myHash;  /* Hash code of key, stored here since the key may be tossed by the GC */
    @Nonnull
    private final HashingStrategy<? super T> myStrategy;

    private SoftKey(@Nonnull T k, @Nonnull HashingStrategy<? super T> strategy, @Nonnull ReferenceQueue<? super T> q) {
      super(k, q);
      myStrategy = strategy;
      myHash = strategy.hashCode(k);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Key)) return false;
      if (myHash != o.hashCode()) return false;
      T t = get();
      T u = ((Key<T>)o).get();
      if (t == null || u == null) return false;
      return keyEqual(t, u, myStrategy);
    }

    @Override
    public int hashCode() {
      return myHash;
    }

    @Override
    public String toString() {
      return "SoftHashMap.SoftKey(" + get() + ")";
    }
  }
}
