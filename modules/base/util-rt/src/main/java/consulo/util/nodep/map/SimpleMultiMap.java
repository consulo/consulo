/*
 * Copyright 2013-2019 consulo.io
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
package consulo.util.nodep.map;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 2019-07-18
 */
public class SimpleMultiMap<K, V> implements Map<K, Collection<V>> {
  @Nonnull
  public static <K1, V1> SimpleMultiMap<K1, V1> emptyMap() {
    return new SimpleMultiMap<K1, V1>(Collections.<K1, Collection<V1>>emptyMap());
  }

  @Nonnull
  public static <K1, V1> SimpleMultiMap<K1, V1> newHashMap() {
    return new SimpleMultiMap<K1, V1>(new HashMap<K1, Collection<V1>>());
  }

  private final Map<K, Collection<V>> myMap;

  private SimpleMultiMap(@Nonnull Map<K, Collection<V>> delegate) {
    myMap = delegate;
  }

  @Override
  public int size() {
    return myMap.size();
  }

  @Override
  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return myMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    for (Collection<V> vs : myMap.values()) {
      if (vs.contains(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nonnull
  public Collection<V> get(Object key) {
    Collection<V> vs = myMap.get(key);
    return vs == null ? Collections.<V>emptyList() : vs;
  }

  public void putValue(K key, V value) {
    Collection<V> vs = myMap.get(key);
    if (vs == null) {
      myMap.put(key, vs = new ArrayList<V>());
    }

    vs.add(value);
  }

  @Override
  public Collection<V> put(K key, Collection<V> value) {
    for (V v : value) {
      putValue(key, v);
    }

    return Collections.emptyList();
  }

  @Override
  public void putAll(@Nonnull Map<? extends K, ? extends Collection<V>> map) {
    for (Entry<? extends K, ? extends Collection<V>> e : map.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public Collection<V> remove(Object key) {
    return myMap.remove(key);
  }

  @Override
  public void clear() {
    myMap.clear();
  }

  @Nonnull
  @Override
  public Set<K> keySet() {
    return myMap.keySet();
  }

  @Nonnull
  @Override
  public Collection<Collection<V>> values() {
    return myMap.values();
  }

  @Nonnull
  @Override
  public Set<Entry<K, Collection<V>>> entrySet() {
    return myMap.entrySet();
  }
}
