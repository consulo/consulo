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
package consulo.util.collection;

import consulo.util.collection.impl.CollectionFactory;
import consulo.util.collection.impl.map.ConcurrentHashMap;
import consulo.util.collection.impl.set.WeakHashSet;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 16/01/2021
 */
public final class Sets {
  private static CollectionFactory ourFactory = CollectionFactory.get();

  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <T> Set<T> newWeakHashSet() {
    return new WeakHashSet<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> newHashSet(@Nonnull HashingStrategy<T> hashingStrategy) {
    return newHashSet(CollectionFactory.UNKNOWN_CAPACITY, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> newHashSet(@Nonnull Collection<? extends T> items, @Nonnull HashingStrategy<T> hashingStrategy) {
    return ourFactory.newHashSetWithStrategy(CollectionFactory.UNKNOWN_CAPACITY, items, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K> Set<K> newHashSet(int initialCapacity, @Nonnull HashingStrategy<K> hashingStrategy) {
    return ourFactory.newHashSetWithStrategy(initialCapacity, null, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> newLinkedHashSet(@Nonnull HashingStrategy<T> hashingStrategy) {
    return Collections.newSetFromMap(Maps.newLinkedHashMap(hashingStrategy));
  }

  @Nonnull
  @Contract(pure = true)
  public static <K> Set<K> newIdentityHashSet() {
    return newHashSet(CollectionFactory.UNKNOWN_CAPACITY, HashingStrategy.identity());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K> Set<K> newIdentityHashSet(int initialCapacity) {
    return newHashSet(initialCapacity, HashingStrategy.identity());
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> newConcurrentHashSet() {
    return ConcurrentHashMap.newKeySet();
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> newConcurrentHashSet(@Nonnull HashingStrategy<T> hashStrategy) {
    return Collections.newSetFromMap(Maps.newConcurrentHashMap(hashStrategy));
  }
}
