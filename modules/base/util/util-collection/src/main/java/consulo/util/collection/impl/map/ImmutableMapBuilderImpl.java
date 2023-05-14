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
package consulo.util.collection.impl.map;

import consulo.util.collection.ImmutableMapBuilder;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImmutableMapBuilderImpl<K, V> implements ImmutableMapBuilder<K, V> {
  private final Map<K, V> myMap = new HashMap<>();

  @Override
  public ImmutableMapBuilder<K, V> put(K key, V value) {
    myMap.put(key, value);
    return this;
  }

  @Nonnull
  @Override
  @Contract(pure = true)
  public Map<K, V> build() {
    return Collections.unmodifiableMap(myMap);
  }
}
