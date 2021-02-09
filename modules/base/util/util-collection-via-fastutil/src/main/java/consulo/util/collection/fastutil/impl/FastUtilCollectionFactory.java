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
package consulo.util.collection.fastutil.impl;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.CollectionFactory;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public class FastUtilCollectionFactory implements CollectionFactory {
  @Override
  public <T> Set<T> newHashSetWithStrategy(int capacity, @Nullable Collection<? extends T> inner, HashingStrategy<T> strategy) {
    if (inner != null) {
      return new ObjectOpenCustomHashSet<>(inner, mapStrategy(strategy));
    }
    else if (capacity == UNKNOWN_CAPACITY) {
      return new ObjectOpenCustomHashSet<T>(mapStrategy(strategy));
    }
    else {
      return new ObjectOpenCustomHashSet<>(capacity, mapStrategy(strategy));
    }
  }

  @Override
  public <K, V> Map<K, V> newHashMapWithStrategy(@Nullable Map<? extends K, ? extends V> inner, @Nonnull HashingStrategy<K> hashingStrategy) {
    if(inner != null) {
      return new Object2ObjectOpenCustomHashMap<>(inner, mapStrategy(hashingStrategy));
    }
    return new Object2ObjectOpenCustomHashMap<>(mapStrategy(hashingStrategy));
  }

  @Override
  public <K, V> Map<K, V> newWeakHashMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<? super K> strategy) {
    // todo [VISTALL] not implemented
    throw new UnsupportedOperationException();
  }

  @Override
  public <K, V> Map<K, V> newSoftHashMap(@Nonnull HashingStrategy<? super K> strategy) {
    // todo [VISTALL] not implemented
    throw new UnsupportedOperationException();
  }

  private <K> Hash.Strategy<K> mapStrategy(HashingStrategy<K> hashingStrategy) {
    return new Hash.Strategy<>() {
      @Override
      public int hashCode(K k) {
        return hashingStrategy.hashCode(k);
      }

      @Override
      public boolean equals(K o1, K o2) {
        return hashingStrategy.equals(o1, o2);
      }
    };
  }
}
