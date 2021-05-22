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

import consulo.util.collection.primitive.longs.impl.map.ConcurrentLongObjectHashMap;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09/02/2021
 */
public final class LongMaps {
  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentLongObjectMap<V> newConcurrentLongObjectHashMap() {
    return new ConcurrentLongObjectHashMap<V>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentLongObjectMap<V> newConcurrentLongObjectHashMap(int initialCapacity) {
    return new ConcurrentLongObjectHashMap<V>(initialCapacity);
  }
}
