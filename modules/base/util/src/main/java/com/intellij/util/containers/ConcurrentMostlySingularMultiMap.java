/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.util.ConcurrencyUtil;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentMostlySingularMultiMap<K, V> extends MostlySingularMultiMap<K, V> {
  @Nonnull
  @Override
  protected Map<K, Object> createMap() {
    return ContainerUtil.newConcurrentMap();
  }

  @Override
  public void add(@Nonnull K key, @Nonnull V value) {
    ConcurrentMap<K, Object> map = (ConcurrentMap<K, Object>)myMap;
    while (true) {
      Object current = map.get(key);
      if (current == null) {
        if (ConcurrencyUtil.cacheOrGet(map, key, value) == value) break;
      }
      else if (current instanceof MostlySingularMultiMap.ValueList) {
        ValueList<?> curList = (ValueList)current;
        ValueList<Object> newList = new ValueList<Object>(curList.size() + 1);
        newList.addAll(curList);
        newList.add(value);
        if (map.replace(key, curList, newList)) break;
      }
      else {
        ValueList<Object> newList = new ValueList<Object>(2);
        newList.add(current);
        newList.add(value);
        if (map.replace(key, current, newList)) break;
      }
    }
  }

  @Override
  public void compact() {
    // not implemented
  }

  public boolean replace(@Nonnull K key, @Nonnull Collection<V> expectedValue, @Nonnull Collection<V> newValue) {
    ConcurrentMap<K, Object> map = (ConcurrentMap<K, Object>)myMap;
    Object newValueToPut = newValue.isEmpty() ? null : newValue.size() == 1 ? newValue.iterator().next() : new ValueList<Object>(newValue);

    Object oldValue = map.get(key);
    List<V> oldCollection = rawValueToCollection(oldValue);
    if (!oldCollection.equals(expectedValue)) return false;

    if (oldValue == null) {
      return newValueToPut == null || map.putIfAbsent(key, newValueToPut) == null;
    }
    if (newValueToPut == null) {
      return map.remove(key, oldValue);
    }
    return map.replace(key, oldValue, newValueToPut);
  }

  @Override
  public void addAll(MostlySingularMultiMap<K, V> other) {
    throw new AbstractMethodError("Not yet re-implemented for concurrency");
  }

  @Override
  public boolean remove(@Nonnull K key, @Nonnull V value) {
    throw new AbstractMethodError("Not yet re-implemented for concurrency");
  }
}
