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
package consulo.util.collection.primitive.ints;

import consulo.util.collection.impl.CollectionFactory;
import consulo.util.collection.primitive.impl.PrimitiveCollectionFactory;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08/05/2021
 */
public class IntSets {
  private static final PrimitiveCollectionFactory ourFactory = (PrimitiveCollectionFactory)CollectionFactory.get();

  @Nonnull
  public static IntSet newHashSet() {
    return ourFactory.newIntHashSet(CollectionFactory.UNKNOWN_CAPACITY, null);
  }

  @Nonnull
  public static IntSet newHashSet(int capacity) {
    return ourFactory.newIntHashSet(capacity, null);
  }

  @Nonnull
  public static IntSet newHashSet(int[] array) {
    return ourFactory.newIntHashSet(CollectionFactory.UNKNOWN_CAPACITY, array);
  }
}
