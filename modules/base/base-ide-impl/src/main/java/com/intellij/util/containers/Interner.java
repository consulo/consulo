/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util.containers;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.containers.MapBasedInterner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public interface Interner<T> {
  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Objects are cached on weak references
   * and garbage-collected when not needed anymore.
   */
  @Nonnull
  public static <T> Interner<T> createWeakInterner() {
    return new MapBasedInterner<T>(ContainerUtil.createConcurrentWeakKeyWeakValueMap());
  }

  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Objects are cached on weak references
   * and garbage-collected when not needed anymore.
   */
  @Nonnull
  public static <T> Interner<T> createWeakInterner(@Nonnull HashingStrategy<T> strategy) {
    return new MapBasedInterner<T>(ContainerUtil.createConcurrentWeakKeyWeakValueMap(strategy));
  }

  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Note: objects are cached inside
   * and on hard references, so even the ones that are not used anymore will be still present in the memory.
   *
   * @author peter
   */
  @Nonnull
  public static <T> Interner<T> createConcurrentHashInterner() {
    return new MapBasedInterner<>(Maps.newConcurrentHashMap());
  }

  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Note: objects are cached inside
   * and on hard references, so even the ones that are not used anymore will be still present in the memory.
   *
   * @author peter
   */
  @Nonnull
  public static <T> Interner<T> createConcurrentHashInterner(@Nonnull HashingStrategy<T> strategy) {
    return new MapBasedInterner<>(Maps.newConcurrentHashMap(strategy));
  }

  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Note: objects are cached inside
   * and on hard references, so even the ones that are not used anymore will be still present in the memory.
   *
   * @author peter
   */
  @Nonnull
  public static <T> Interner<T> createHashInterner(@Nonnull HashingStrategy<T> strategy) {
    return new MapBasedInterner<>(Maps.newHashMap(strategy));
  }

  /**
   * Default interner for strings
   */
  @Nonnull
  public static Interner<String> createStringInterner() {
    return createConcurrentHashInterner();
  }

  @Nonnull
  T intern(@Nonnull T item);

  default void internAll(@Nonnull Iterable<? extends T> iterable) {
    for (T t : iterable) {
      intern(t);
    }
  }

  default void internAll(@Nonnull T[] values) {
    for (T t : values) {
      intern(t);
    }
  }

  /**
   * Return interned value. Null if not interned
   */
  @Nullable
  T get(@Nonnull T item);

  void clear();

  @Nonnull
  Set<T> getValues();
}
