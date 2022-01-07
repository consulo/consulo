/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.concurrency;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.collection.Sets;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Creates various concurrent collections (e.g maps, sets) which can be customized with {@link HashingStrategy}
 */
@Deprecated
public class ConcurrentCollectionFactory {
  @Nonnull
  @Contract(pure = true)
  public static <T, V> ConcurrentMap<T, V> createMap(@Nonnull HashingStrategy<T> hashStrategy) {
    return Maps.newConcurrentHashMap(hashStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> ConcurrentMap<T, V> createMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull HashingStrategy<T> hashStrategy) {
    return Maps.newConcurrentHashMap(initialCapacity, loadFactor, concurrencyLevel, hashStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> createConcurrentSet(@Nonnull HashingStrategy<T> hashStrategy) {
    return Sets.newConcurrentHashSet(hashStrategy);
  }
}
