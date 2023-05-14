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
package consulo.application.internal.util;

import consulo.application.util.CachedValue;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 24-Apr-22
 */
public class CachedValueManagerHelper {
  private static final ConcurrentMap<String, Key<CachedValue<?>>> globalKeyForProvider = new ConcurrentHashMap<>();

  @Nonnull
  public static <T> Key<CachedValue<T>> getKeyForClass(@Nonnull Class<?> providerClass) {
    return getKeyForClass(providerClass, globalKeyForProvider);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T> Key<CachedValue<T>> getKeyForClass(@Nonnull Class<?> providerClass, ConcurrentMap<String, Key<CachedValue<?>>> keyForProvider) {
    String name = providerClass.getName();
    Key<CachedValue<?>> key = keyForProvider.get(name);
    if (key == null) {
      key = keyForProvider.computeIfAbsent(name, Key::create);
    }
    return (Key)key;
  }
}
