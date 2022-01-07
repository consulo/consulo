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
package consulo.util.containers;

import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.Interner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 10/02/2021
 */
public class MapBasedInterner<T> implements Interner<T> {
  private final Map<T, T> myMap;

  public MapBasedInterner(@Nonnull Map<T, T> map) {
    myMap = map;
  }

  @Override
  @Nonnull
  public T intern(@Nonnull T name) {
    return ConcurrencyUtil.cacheOrGet(myMap, name, name);
  }

  @Nullable
  @Override
  public T get(@Nonnull T item) {
    return myMap.get(item);
  }

  @Override
  public void clear() {
    myMap.clear();
  }

  @Override
  @Nonnull
  public Set<T> getValues() {
    return new HashSet<>(myMap.values());
  }
}
