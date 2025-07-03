/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.util.collection;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public abstract class SLRUCache<K, V> extends SLRUMap<K,V> {
  protected SLRUCache(final int protectedQueueSize, final int probationalQueueSize) {
    super(protectedQueueSize, probationalQueueSize);
  }

  protected SLRUCache(final int protectedQueueSize, final int probationalQueueSize, HashingStrategy<K> hashingStrategy) {
    super(protectedQueueSize, probationalQueueSize, hashingStrategy);
  }

  @Nonnull
  public abstract V createValue(K key);

  @Override
  @Nonnull
  public V get(K key) {
    V value = super.get(key);
    if (value != null) {
      return value;
    }

    value = createValue(key);
    put(key, value);

    return value;
  }

  @Nullable
  public V getIfCached(K key) {
    return super.get(key);
  }
}