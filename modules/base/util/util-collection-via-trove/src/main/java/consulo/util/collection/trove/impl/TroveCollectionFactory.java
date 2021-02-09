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
package consulo.util.collection.trove.impl;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.CollectionFactory;
import consulo.util.collection.trove.impl.ints.TSoftHashMap;
import consulo.util.collection.trove.impl.ints.TWeakHashMap;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public class TroveCollectionFactory implements CollectionFactory {
  @Override
  public <T> Set<T> newHashSetWithStrategy(int capacity, @Nullable Collection<? extends T> inner, HashingStrategy<T> strategy) {
    if(inner != null) {
      return new THashSet<T>(inner, mapStrategy(strategy));
    }
    else if (capacity == UNKNOWN_CAPACITY) {
      return new THashSet<>(mapStrategy(strategy));
    }
    else {
      return new THashSet<T>(capacity, mapStrategy(strategy));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Map<K, V> newHashMapWithStrategy(@Nullable Map<? extends K, ? extends V> inner, @Nonnull HashingStrategy<K> hashingStrategy) {
    if(inner != null) {
      Map parameterTypeFix = inner;
      return new THashMap<>(parameterTypeFix, mapStrategy(hashingStrategy));
    }
    return new THashMap<>(mapStrategy(hashingStrategy));
  }

  @Override
  public <K, V> Map<K, V> newWeakHashMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<? super K> strategy) {
    return new TWeakHashMap<K, V>(initialCapacity, loadFactor, strategy);
  }

  @Override
  public <K, V> Map<K, V> newSoftHashMap(@Nonnull HashingStrategy<? super K> strategy) {
    return new TSoftHashMap<K, V>(strategy);
  }

  private <K> TObjectHashingStrategy<K> mapStrategy(HashingStrategy<K> hashingStrategy) {
    return new TObjectHashingStrategy<>() {
      @Override
      public int computeHashCode(K k) {
        return hashingStrategy.hashCode(k);
      }

      @Override
      public boolean equals(K o1, K o2) {
        return hashingStrategy.equals(o1, o2);
      }
    };
  }
}
