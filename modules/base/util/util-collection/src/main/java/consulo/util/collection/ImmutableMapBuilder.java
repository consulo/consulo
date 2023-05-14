/*
 * Copyright 2013-2023 consulo.io
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
package consulo.util.collection;

import consulo.util.collection.impl.map.ImmutableMapBuilderImpl;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import java.util.Map;

public interface ImmutableMapBuilder<K, V> {
  @Nonnull
  @Contract(pure = true)
  public static <K, V> ImmutableMapBuilder<K, V> newBuilder() {
    return new ImmutableMapBuilderImpl<>();
  }

  @Nonnull
  ImmutableMapBuilder<K, V> put(K key, V value);

  @Contract(pure = true)
  @Nonnull
  Map<K, V> build();
}
