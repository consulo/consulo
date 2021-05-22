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
import java.util.*;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public abstract class CollectionFactory {
  private static CollectionFactory ourFactory;

  public static CollectionFactory get() {
    if (ourFactory == null) {
      for (CollectionFactory factory : ServiceLoader.load(CollectionFactory.class, CollectionFactory.class.getClassLoader())) {
        ourFactory = factory;
        break;
      }
    }
    return Objects.requireNonNull(ourFactory);
  }

  public static final int UNKNOWN_CAPACITY = -1;

  public abstract <T> Set<T> newHashSetWithStrategy(int capacity, @Nullable Collection<? extends T> inner, HashingStrategy<T> strategy);

  public abstract <K, V> Map<K, V> newHashMapWithStrategy(int capacity, float loadFactor, @Nullable Map<? extends K, ? extends V> inner, @Nonnull HashingStrategy<K> hashingStrategy);

  public abstract <K, V> Map<K, V> newWeakHashMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<? super K> strategy);

  public abstract <K, V> Map<K, V> newSoftHashMap(@Nonnull HashingStrategy<? super K> strategy);

  public abstract void trimToSize(Map<?, ?> map);
}
