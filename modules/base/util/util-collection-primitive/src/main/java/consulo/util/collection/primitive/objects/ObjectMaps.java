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
package consulo.util.collection.primitive.objects;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.CollectionFactory;
import consulo.util.collection.primitive.impl.PrimitiveCollectionFactory;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 10/02/2021
 */
public final class ObjectMaps {
  private static PrimitiveCollectionFactory ourFactory = (PrimitiveCollectionFactory)CollectionFactory.get();

  @Nonnull
  public static <K> ObjectIntMap<K> newObjectIntHashMap() {
    return newObjectIntHashMap(CollectionFactory.UNKNOWN_CAPACITY, HashingStrategy.canonical());
  }

  @Nonnull
  public static <K> ObjectIntMap<K> newObjectIntHashMap(int capacity) {
    return newObjectIntHashMap(capacity, HashingStrategy.canonical());
  }

  @Nonnull
  public static <K> ObjectIntMap<K> newObjectIntHashMap(@Nonnull HashingStrategy<K> strategy) {
    return newObjectIntHashMap(CollectionFactory.UNKNOWN_CAPACITY, strategy);
  }

  @Nonnull
  public static <K> ObjectIntMap<K> newObjectIntHashMap(int capacity, @Nonnull HashingStrategy<K> strategy) {
    return ourFactory.newObjectIntHashMap(capacity, strategy);
  }

  @Nonnull
  public static <K> ObjectIntMap<K> unmodified(@Nonnull ObjectIntMap<K> map) {
    return new UnmodifiedObjectIntMap<>(map);
  }

  public static void trimToSize(ObjectIntMap<?> map) {
  }
}
