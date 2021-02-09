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

import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.collection.primitive.ints.IntSet;
import gnu.trove.TIntObjectHashMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public class THashIntObjectMap<V> extends TIntObjectHashMap<V> implements IntObjectMap<V> {
  @Override
  public V put(int key, V value) {
    return super.put(key, value);
  }

  @Nonnull
  @Override
  public Set<IntObjectEntry<V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Collection<V> values() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public IntSet keySet() {
    throw new UnsupportedOperationException();
  }
}
