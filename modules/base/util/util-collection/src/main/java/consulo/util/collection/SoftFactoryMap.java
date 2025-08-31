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

package consulo.util.collection;

import consulo.util.lang.ObjectUtil;

import java.util.concurrent.ConcurrentMap;

/**
 * @author peter
 */
public abstract class SoftFactoryMap<T, V> {
  private final ConcurrentMap<T, V> myMap = Maps.newConcurrentWeakKeySoftValueHashMap();

  protected abstract V create(T key);

  public final V get(T key) {
    V v = myMap.get(key);
    if (v != null) {
      return v == ObjectUtil.NULL ? null : v;
    }

    V value = create(key);
    V toPut = value == null ? (V)ObjectUtil.NULL : value;
    V prev = myMap.putIfAbsent(key, toPut);
    return prev == null || prev == ObjectUtil.NULL ? value : prev;
  }

  public void clear() {
    myMap.clear();
  }
}