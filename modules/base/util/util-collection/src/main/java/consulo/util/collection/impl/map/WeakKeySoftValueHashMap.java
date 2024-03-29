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

import consulo.util.collection.Maps;

import jakarta.annotation.Nonnull;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;

public final class WeakKeySoftValueHashMap<K, V> extends RefKeyRefValueHashMap<K, V> implements Map<K, V> {
  public WeakKeySoftValueHashMap() {
    super((RefHashMap<K, ValueReference<K, V>>)Maps.<K, ValueReference<K, V>>newWeakHashMap());
  }

  private static class SoftValueReference<K, V> extends SoftReference<V> implements ValueReference<K, V> {
    @Nonnull
    private final RefHashMap.Key<K> key;

    private SoftValueReference(@Nonnull RefHashMap.Key<K> key, V referent, ReferenceQueue<? super V> q) {
      super(referent, q);
      this.key = key;
    }

    @Nonnull
    @Override
    public RefHashMap.Key<K> getKey() {
      return key;
    }
  }

  @Nonnull
  @Override
  protected ValueReference<K, V> createValueReference(@Nonnull RefHashMap.Key<K> key, V referent, ReferenceQueue<? super V> q) {
    return new SoftValueReference<>(key, referent, q);
  }
}
