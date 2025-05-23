/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.util.lang.DeprecatedMethodException;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * a Map which computes the value associated with the key (via {@link #create(Object)} method) on first {@link #get(Object)} access.
 * NOT THREAD SAFE.
 * For thread-safe alternative please use {@link ConcurrentFactoryMap}
 */
@Deprecated
@DeprecationInfo("Use map#compureIfAbsent() method")
public abstract class FactoryMap<K, V> implements Map<K, V> {
  private Map<K, V> myMap;

  /**
   * @deprecated Use {@link #create(Function)} instead
   */
  @Deprecated
  public FactoryMap() {
    DeprecatedMethodException.report("Use FactoryMap.create*() instead");
  }

  private FactoryMap(boolean safe) {
  }

  @Nonnull
  protected Map<K, V> createMap() {
    return new HashMap<>();
  }

  @Nullable
  protected abstract V create(K key);

  @Override
  public V get(Object key) {
    Map<K, V> map = getMap();
    K k = notNull(key);
    V value = map.get(k);
    if (value == null) {
      value = create((K)key);
      V v = notNull(value);
      map.put(k, v);
    }
    return nullize(value);
  }

  private Map<K, V> getMap() {
    Map<K, V> map = myMap;
    if (map == null) {
      myMap = map = createMap();
    }
    return map;
  }

  private static <T> T FAKE_NULL() {
    //noinspection unchecked
    return (T)ObjectUtil.NULL;
  }

  private static <T> T notNull(final Object key) {
    //noinspection unchecked
    return key == null ? FAKE_NULL() : (T)key;
  }

  @Nullable
  private static <T> T nullize(T value) {
    return value == FAKE_NULL() ? null : value;
  }

  @Override
  public final boolean containsKey(Object key) {
    return getMap().containsKey(notNull(key));
  }

  @Override
  public V put(K key, V value) {
    K k = notNull(key);
    V v = notNull(value);
    v = getMap().put(k, v);
    return nullize(v);
  }

  @Override
  public V remove(Object key) {
    V v = getMap().remove(key);
    return nullize(v);
  }

  @Nonnull
  @Override
  public Set<K> keySet() {
    final Set<K> ts = getMap().keySet();
    K nullKey = FAKE_NULL();
    if (ts.contains(nullKey)) {
      final java.util.HashSet<K> hashSet = new HashSet<>(ts);
      hashSet.remove(nullKey);
      hashSet.add(null);
      return hashSet;
    }
    return ts;
  }

  public boolean removeValue(Object value) {
    Object t = notNull(value);
    //noinspection SuspiciousMethodCalls
    return getMap().values().remove(t);
  }


  @Override
  public void clear() {
    getMap().clear();
  }

  @Override
  public int size() {
    return getMap().size();
  }

  @Override
  public boolean isEmpty() {
    return getMap().isEmpty();
  }

  @Override
  public boolean containsValue(final Object value) {
    return getMap().containsValue(value);
  }

  @Override
  public void putAll(@Nonnull final Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Nonnull
  @Override
  public Collection<V> values() {
    return ContainerUtil.map(getMap().values(), FactoryMap::nullize);
  }

  @Nonnull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return ContainerUtil.map2Set(getMap().entrySet(), entry -> new AbstractMap.SimpleEntry<>(nullize(entry.getKey()), nullize(entry.getValue())));
  }

  @Nonnull
  public static <K, V> Map<K, V> create(@Nonnull final Function<? super K, ? extends V> computeValue) {
    return new FactoryMap<K, V>(true) {
      @Nullable
      @Override
      protected V create(K key) {
        return computeValue.apply(key);
      }
    };
  }

  @Nonnull
  public static <K, V> Map<K, V> createMap(@Nonnull final Function<? super K, ? extends V> computeValue, @Nonnull final Supplier<? extends Map<K, V>> mapCreator) {
    return new FactoryMap<K, V>(true) {
      @Nullable
      @Override
      protected V create(K key) {
        return computeValue.apply(key);
      }

      @Nonnull
      @Override
      protected Map<K, V> createMap() {
        return mapCreator.get();
      }
    };
  }
}
