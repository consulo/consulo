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
package consulo.util.interner;

import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;

import org.jspecify.annotations.Nullable;
import java.util.Set;

public interface Interner<T> {
  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Objects are cached on weak references
   * and garbage-collected when not needed anymore.
   */
  public static <T> Interner<T> createWeakInterner() {
    return new MapBasedInterner<T>(ContainerUtil.createConcurrentWeakKeyWeakValueMap());
  }

  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Objects are cached on weak references
   * and garbage-collected when not needed anymore.
   */
  public static <T> Interner<T> createWeakInterner(HashingStrategy<T> strategy) {
    return new MapBasedInterner<T>(ContainerUtil.createConcurrentWeakKeyWeakValueMap(strategy));
  }

  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Note: objects are cached inside
   * and on hard references, so even the ones that are not used anymore will be still present in the memory.
   *
   * @author peter
   */
  public static <T> Interner<T> createConcurrentHashInterner() {
    return new MapBasedInterner<>(Maps.newConcurrentHashMap());
  }

  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Note: objects are cached inside
   * and on hard references, so even the ones that are not used anymore will be still present in the memory.
   *
   * @author peter
   */
  public static <T> Interner<T> createConcurrentHashInterner(HashingStrategy<T> strategy) {
    return new MapBasedInterner<>(Maps.newConcurrentHashMap(strategy));
  }

  /**
   * Allow to reuse structurally equal objects to avoid memory being wasted on them. Note: objects are cached inside
   * and on hard references, so even the ones that are not used anymore will be still present in the memory.
   *
   * @author peter
   */
  public static <T> Interner<T> createHashInterner(HashingStrategy<T> strategy) {
    return new MapBasedInterner<>(Maps.newHashMap(strategy));
  }

  /**
   * Default interner for strings
   */
  public static Interner<String> createStringInterner() {
    return createConcurrentHashInterner();
  }

  T intern(T item);

  default void internAll(Iterable<? extends T> iterable) {
    for (T t : iterable) {
      intern(t);
    }
  }

  default void internAll(T[] values) {
    for (T t : values) {
      intern(t);
    }
  }

  /**
   * Return interned value. Null if not interned
   */
  @Nullable
  T get(T item);

  void clear();

  Set<T> getValues();
}
