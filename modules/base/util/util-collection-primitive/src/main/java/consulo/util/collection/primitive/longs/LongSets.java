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
package consulo.util.collection.primitive.longs;

import consulo.util.collection.impl.CollectionFactory;
import consulo.util.collection.primitive.impl.PrimitiveCollectionFactory;

/**
 * @author VISTALL
 * @since 17/05/2021
 */
public final class LongSets {
  private static PrimitiveCollectionFactory ourFactory = (PrimitiveCollectionFactory)CollectionFactory.get();

  public static LongSet newHashSet() {
    return ourFactory.newLongHashSet(CollectionFactory.UNKNOWN_CAPACITY);
  }

  public static LongSet newHashSet(int capacity) {
    return ourFactory.newLongHashSet(capacity);
  }
}
