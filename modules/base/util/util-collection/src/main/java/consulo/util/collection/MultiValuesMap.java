/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import jakarta.annotation.Nullable;

import java.util.*;

public class MultiValuesMap<K, V>{
  private final Map<K, Collection<V>> myBaseMap;
  private final boolean myOrdered;

  public MultiValuesMap() {
    this(false);
  }

  public MultiValuesMap(boolean ordered) {
    myOrdered = ordered;
    myBaseMap = ordered ? new LinkedHashMap<>() : new HashMap<>();
  }

  public void putAll(K key, Collection<V> values) {
    for (V value : values) {
      put(key, value);
    }
  }

  @SafeVarargs
  public final void putAll(K key, V... values) {
    for (V value : values) {
      put(key, value);
    }
  }

  public void put(K key, V value) {
    if (!myBaseMap.containsKey(key)) {
      myBaseMap.put(key, myOrdered ? new LinkedHashSet<>() : new HashSet<>());
    }

    myBaseMap.get(key).add(value);
  }

  @Nullable
  public Collection<V> get(K key){
    return myBaseMap.get(key);
  }

  public Set<K> keySet() {
    return myBaseMap.keySet();
  }

  public Collection<V> values() {
    Set<V> result = myOrdered ? new LinkedHashSet<>() : new HashSet<>();
    for (Collection<V> values : myBaseMap.values()) {
      result.addAll(values);
    }

    return result;
  }

  public void remove(K key, V value) {
    if (!myBaseMap.containsKey(key)) return;
    Collection<V> values = myBaseMap.get(key);
    values.remove(value);
    if (values.isEmpty()) {
      myBaseMap.remove(key);
    }
  }

  public void clear() {
    myBaseMap.clear();
  }

  @Nullable
  public Collection<V> removeAll(K key) {
    return myBaseMap.remove(key);
  }

  public Set<Map.Entry<K, Collection<V>>> entrySet() {
    return myBaseMap.entrySet();
  }

  public boolean isEmpty() {
    return myBaseMap.isEmpty();
  }

  public boolean containsKey(K key) {
    return myBaseMap.containsKey(key);
  }

  public Collection<V> collectValues() {
    Collection<V> result = new HashSet<>();
    for (Collection<V> v : myBaseMap.values()) {
      result.addAll(v);
    }

    return result;
  }

  @Nullable
  public V getFirst(K key) {
    Collection<V> values = myBaseMap.get(key);
    return values == null || values.isEmpty() ? null : values.iterator().next();
  }


}
