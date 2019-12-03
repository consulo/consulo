/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.util.dataholder.keyFMap;

import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An immutable map optimized for storing few {@link Key} entries with relatively rare updates
 * To construct a map, start with {@link KeyFMap#EMPTY_MAP} and call {@link #plus} and {@link #minus}
 *
 * @author peter
 */
public interface KeyFMap {
  KeyFMap EMPTY_MAP = new EmptyFMap();

  @Nonnull
  <V> KeyFMap plus(@Nonnull Key<V> key, @Nonnull V value);
  @Nonnull
  KeyFMap minus(@Nonnull Key<?> key);

  @Nullable
  <V> V get(@Nonnull Key<V> key);

  @Nonnull
  Key[] getKeys();

  String toString();

  boolean isEmpty();
}
