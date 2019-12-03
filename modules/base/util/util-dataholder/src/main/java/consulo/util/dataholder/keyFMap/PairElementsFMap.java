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

public class PairElementsFMap implements KeyFMap {
  private final Key key1;
  private final Key key2;
  private final Object value1;
  private final Object value2;

  PairElementsFMap(@Nonnull Key key1, @Nonnull Object value1, @Nonnull Key key2, @Nonnull Object value2) {
    this.key1 = key1;
    this.value1 = value1;
    this.key2 = key2;
    this.value2 = value2;
    assert key1 != key2;
  }

  @Nonnull
  @Override
  public <V> KeyFMap plus(@Nonnull Key<V> key, @Nonnull V value) {
    if (key == key1) return new PairElementsFMap(key, value, key2, value2);
    if (key == key2) return new PairElementsFMap(key, value, key1, value1);
    return new ArrayBackedFMap(new int[]{key1.hashCode(), key2.hashCode(), key.hashCode()}, new Object[]{value1, value2, value});
  }

  @Nonnull
  @Override
  public KeyFMap minus(@Nonnull Key<?> key) {
    if (key == key1) return new OneElementFMap<>(key2, value2);
    if (key == key2) return new OneElementFMap<>(key1, value1);
    return this;
  }

  @Override
  public <V> V get(@Nonnull Key<V> key) {
    //noinspection unchecked
    return key == key1 ? (V)value1 : key == key2 ? (V)value2 : null;
  }

  @Nonnull
  @Override
  public Key[] getKeys() {
    return new Key[] { key1, key2 };
  }

  @Override
  public String toString() {
    return "Pair: (" + key1 + " -> " + value1 + "; " + key2 + " -> " + value2 + ")";
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  public Key getKey1() {
    return key1;
  }

  public Key getKey2() {
    return key2;
  }

  public Object getValue1() {
    return value1;
  }

  public Object getValue2() {
    return value2;
  }
}
