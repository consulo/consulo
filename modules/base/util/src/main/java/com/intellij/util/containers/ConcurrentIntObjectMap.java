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
package com.intellij.util.containers;

import javax.annotation.Nonnull;

import java.util.Enumeration;

/**
 * Base interface for concurrent int key -> value:V map
 * Null values are NOT allowed
 * <p>
 * Methods are adapted from {@link java.util.concurrent.ConcurrentMap} to integer keys
 *
 * @see java.util.concurrent.ConcurrentMap
 */
public interface ConcurrentIntObjectMap<V> extends IntObjectMap<V> {
  /**
   * @return written value
   */
  @Nonnull
  V cacheOrGet(int key, @Nonnull V value);

  boolean remove(int key, @Nonnull V value);

  boolean replace(int key, @Nonnull V oldValue, @Nonnull V newValue);

  @Nonnull
  Enumeration<V> elements();

  /**
   * @return Approximate number of elements in the map.
   * The usage is discouraged since
   * First, in concurrent context it doesn't have much sense
   * and Second, for weak- or soft- keyed maps it returns the total number of references
   * rather than alive values because otherwise it would be too expensive
   */
  @Override
  int size();

  /**
   * @return the previous value associated with the specified key,
   * or {@code null} if there was no mapping for the key
   */
  V putIfAbsent(int key, @Nonnull V value);

}
