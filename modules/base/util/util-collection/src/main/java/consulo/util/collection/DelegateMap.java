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
package consulo.util.collection;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 06/05/2021
 */
public class DelegateMap<K, V> implements Map<K, V> {
  private Map<K, V> myDelegate;

  public DelegateMap(Map<K, V> delegate) {
    myDelegate = delegate;
  }

  @Override
  public int size() {
    return myDelegate.size();
  }

  @Override
  public boolean isEmpty() {
    return myDelegate.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return myDelegate.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return myDelegate.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return myDelegate.get(key);
  }

  @Override
  public V put(K key, V value) {
    return myDelegate.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return myDelegate.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    myDelegate.putAll(m);
  }

  @Override
  public void clear() {
    myDelegate.clear();
  }

  @Nonnull
  @Override
  public Set<K> keySet() {
    return myDelegate.keySet();
  }

  @Nonnull
  @Override
  public Collection<V> values() {
    return myDelegate.values();
  }

  @Nonnull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return myDelegate.entrySet();
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    return myDelegate.getOrDefault(key, defaultValue);
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    myDelegate.forEach(action);
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    myDelegate.replaceAll(function);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return myDelegate.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return myDelegate.remove(key, value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return myDelegate.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    return myDelegate.replace(key, value);
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    return myDelegate.computeIfAbsent(key, mappingFunction);
  }

  @Override
  public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return myDelegate.computeIfPresent(key, remappingFunction);
  }

  @Override
  public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return myDelegate.compute(key, remappingFunction);
  }

  @Override
  public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    return myDelegate.merge(key, value, remappingFunction);
  }
}
