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

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.internal.KeyRegistry;

import javax.annotation.Nonnull;

import static consulo.util.dataholder.keyFMap.ArrayBackedFMap.getKeysByIndices;

class MapBackedFMap implements KeyFMap {
  private static final KeyRegistry ourRegistry = KeyRegistry.ourInstance;

  private final IntObjectMap<Object> myMap;

  private MapBackedFMap(@Nonnull MapBackedFMap oldMap, final int exclude) {
    myMap = IntMaps.newIntObjectHashMap(oldMap.size());
    oldMap.myMap.forEach((key, val) -> {
      if (key != exclude) myMap.put(key, val);
      assert key >= 0 : key;
    });
    assert size() > ArrayBackedFMap.ARRAY_THRESHOLD;
  }

  MapBackedFMap(@Nonnull int[] keys, int newKey, @Nonnull Object[] values, @Nonnull Object newValue) {
    myMap = IntMaps.newIntObjectHashMap(keys.length + 1);
    for (int i = 0; i < keys.length; i++) {
      int key = keys[i];
      Object value = values[i];
      myMap.put(key, value);
      assert key >= 0 : key;
    }
    myMap.put(newKey, newValue);
    assert newKey >= 0 : newKey;
    assert size() > ArrayBackedFMap.ARRAY_THRESHOLD;
  }

  @Nonnull
  @Override
  public <V> KeyFMap plus(@Nonnull Key<V> key, @Nonnull V value) {
    int keyCode = key.hashCode();
    assert keyCode >= 0 : key;
    @SuppressWarnings("unchecked") V oldValue = (V)myMap.get(keyCode);
    if (value == oldValue) return this;
    MapBackedFMap newMap = new MapBackedFMap(this, -1);
    newMap.myMap.put(keyCode, value);
    return newMap;
  }

  @Nonnull
  @Override
  public KeyFMap minus(@Nonnull Key<?> key) {
    int oldSize = size();
    int keyCode = key.hashCode();
    if (!myMap.containsKey(keyCode)) {
      return this;
    }
    if (oldSize == ArrayBackedFMap.ARRAY_THRESHOLD + 1) {
      int[] keys = myMap.keys();
      Object[] values = myMap.values().toArray(Object[]::new);
      int i = ArrayUtil.indexOf(keys, keyCode);
      keys = ArrayUtil.remove(keys, i);
      values = ArrayUtil.remove(values, i);
      return new ArrayBackedFMap(keys, values);
    }
    return new MapBackedFMap(this, keyCode);
  }

  @Override
  public <V> V get(@Nonnull Key<V> key) {
    //noinspection unchecked
    return (V)myMap.get(key.hashCode());
  }

  @Nonnull
  @Override
  public Key[] getKeys() {
    return getKeysByIndices(myMap.keys());
  }

  public int size() {
    return myMap.size();
  }

  @Override
  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder s = new StringBuilder();
    myMap.forEach((key, value) -> {
      s.append(s.length() == 0 ? "" : ", ").append(ourRegistry.getKeyByIndex(key)).append(" -> ").append(value);
    });
    return "[" + s.toString() + "]";
  }
}
