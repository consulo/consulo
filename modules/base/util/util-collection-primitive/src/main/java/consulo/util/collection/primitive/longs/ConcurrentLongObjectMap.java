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
package consulo.util.collection.primitive.longs;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

/**
 * Base interface for concurrent long key -> value:V map
 * Null values are NOT allowed
 * <p>
 * Methods are adapted from {@link java.util.concurrent.ConcurrentMap} to long keys
 *
 * @see java.util.concurrent.ConcurrentMap
 */
public interface ConcurrentLongObjectMap<V> extends Long2ObjectMap<V> {
  /**
   * @return written value
   */
  V cacheOrGet(long key, V value);
}
