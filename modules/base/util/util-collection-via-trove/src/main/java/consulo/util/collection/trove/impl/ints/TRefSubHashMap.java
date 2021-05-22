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
import consulo.util.collection.impl.map.RefHashMap;
import gnu.trove.THashMap;
import gnu.trove.TObjectHashingStrategy;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public class TRefSubHashMap<K, V> extends THashMap<RefHashMap.Key<K>, V> implements RefHashMap.SubMap<K, V> {
  private final RefHashMap<K, V> myMap;

  public TRefSubHashMap(int initialCapacity, float loadFactor, RefHashMap<K, V> map, HashingStrategy<? super K> hashingStrategy) {
    super(initialCapacity, loadFactor, new TObjectHashingStrategy<>() {
      @Override
      public int computeHashCode(final RefHashMap.Key<K> key) {
        return key.hashCode(); // use stored hashCode
      }

      @Override
      public boolean equals(final RefHashMap.Key<K> o1, final RefHashMap.Key<K> o2) {
        return o1 == o2 || RefHashMap.keyEqual(o1.get(), o2.get(), hashingStrategy);
      }
    });
    myMap = map;
  }

  @Override
  public void compact() {
    // do not compact the map during many gced references removal because it's bad for performance
    if (!myMap.isProcessingQueue()) {
      super.compact();
    }
  }

  @Override
  public void compactIfNecessary() {
    if (_deadkeys > _size && capacity() > 42) {
      // Compact if more than 50% of all keys are dead. Also, don't trash small maps
      compact();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void rehash(int newCapacity) {
    // rehash should discard gced keys
    // because otherwise there is a remote probability of
    // having two (Weak|Soft)Keys with accidentally equal hashCodes and different but gced key values
    int oldCapacity = _set.length;
    Object[] oldKeys = _set;
    V[] oldVals = _values;

    _set = new Object[newCapacity];
    _values = (V[])new Object[newCapacity];

    for (int i = oldCapacity; i-- > 0; ) {
      Object o = oldKeys[i];
      if (o == null || o == REMOVED) continue;
      RefHashMap.Key<K> k = (RefHashMap.Key<K>)o;
      K key = k.get();
      if (key == null) continue;
      int index = insertionIndex(k);
      if (index < 0) {
        throwObjectContractViolation(_set[-index - 1], o);
        // make 'key' alive till this point to not allow 'o.referent' to be gced
        if (key == _set) throw new AssertionError();
      }
      _set[index] = o;
      _values[index] = oldVals[i];
    }
  }
}
