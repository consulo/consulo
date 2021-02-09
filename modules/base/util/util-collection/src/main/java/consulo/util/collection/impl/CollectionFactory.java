/*
 * Copyright 2013-2021 consulo.io
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
package consulo.util.collection.impl;

import consulo.util.collection.HashingStrategy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public interface CollectionFactory {
  static CollectionFactory get() {
    // TODO impl it!
    throw new UnsupportedOperationException("TODO ");
  }

  int UNKNOWN_CAPACITY = -1;

  <T> Set<T> newHashSetWithStrategy(int capacity, @Nullable Collection<? extends T> inner, HashingStrategy<T> strategy);

  <K, V> Map<K, V> newHashMapWithStrategy(@Nullable Map<? extends K, ? extends V> inner, @Nonnull HashingStrategy<K> hashingStrategy);

  <K, V> Map<K, V> newWeakHashMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<? super K> strategy);

  <K, V> Map<K, V> newSoftHashMap(@Nonnull HashingStrategy<? super K> strategy);
}
