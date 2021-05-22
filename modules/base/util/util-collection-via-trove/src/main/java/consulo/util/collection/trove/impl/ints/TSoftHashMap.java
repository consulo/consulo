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
package consulo.util.collection.trove.impl.ints;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.map.SoftHashMap;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public class TSoftHashMap<K, V> extends SoftHashMap<K, V> {
  public TSoftHashMap(int initialCapacity) {
    super(initialCapacity);
  }

  public TSoftHashMap(@Nonnull HashingStrategy<? super K> hashingStrategy) {
    super(hashingStrategy);
  }

  @Override
  protected SubMap<K, V> createSubMap(int initialCapacity, float loadFactor, HashingStrategy<? super K> strategy) {
    return new TRefSubHashMap<>(initialCapacity, loadFactor, this, strategy);
  }
}
