/*
 * Copyright 2013-2022 consulo.io
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
package consulo.component.extension;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 23-Jun-22
 */
public final class ExtensionPointCacheKey<E, R> {
  @Nonnull
  public static <E1, R1> ExtensionPointCacheKey<E1, R1> create(@Nonnull String keyName, @Nonnull Function<List<E1>, R1> factory) {
    return new ExtensionPointCacheKey<>(keyName, factory);
  }

  @Nonnull
  public static <E1, K1> ExtensionPointCacheKey<E1, Map<K1, E1>> groupBy(@Nonnull String keyName, @Nonnull Function<E1, K1> keyMapper) {
    return create(keyName, extensions -> {
      Map<K1, E1> map = new HashMap<>();
      for (E1 extension : extensions) {
        map.put(keyMapper.apply(extension), extension);
      }
      return map;
    });
  }

  private final String myKeyName;
  private final Function<List<E>, R> myFactory;

  private ExtensionPointCacheKey(String keyName, Function<List<E>, R> factory) {
    myKeyName = keyName;
    myFactory = factory;
  }

  @Nonnull
  public String getKeyName() {
    return myKeyName;
  }

  @Nonnull
  public Function<List<E>, R> getFactory() {
    return myFactory;
  }

  @Override
  public String toString() {
    return myKeyName;
  }
}
