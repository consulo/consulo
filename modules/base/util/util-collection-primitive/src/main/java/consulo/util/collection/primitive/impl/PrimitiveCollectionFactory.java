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
package consulo.util.collection.primitive.impl;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.CollectionFactory;
import consulo.util.collection.primitive.ints.IntIntMap;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.longs.LongSet;
import consulo.util.collection.primitive.objects.ObjectIntMap;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/02/2021
 */
public abstract class PrimitiveCollectionFactory extends CollectionFactory {
  @Nonnull
  public abstract <V> IntObjectMap<V> newIntObjectHashMap(int capacity);

  @Nonnull
  public abstract <K> ObjectIntMap<K> newObjectIntHashMap(int capacity, HashingStrategy<K> strategy);

  public abstract IntSet newIntHashSet(int capacity, int[] array);

  public abstract IntIntMap newIntIntHashMap(int capacity);

  @Nonnull
  public abstract LongSet newLongHashSet(int capacity);

  public void trimToSize(IntIntMap map) {
  }
}
