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

import jakarta.annotation.Nonnull;

public class OneElementFMap<V> implements KeyFMap {
  private final Key myKey;
  private final V myValue;

  public OneElementFMap(@Nonnull Key key, @Nonnull V value) {
    myKey = key;
    myValue = value;
  }

  @Nonnull
  @Override
  public <V> KeyFMap plus(@Nonnull Key<V> key, @Nonnull V value) {
    if (myKey == key) return new OneElementFMap<V>(key, value);
    return new PairElementsFMap(myKey, myValue, key, value);
  }

  @Nonnull
  @Override
  public KeyFMap minus(@Nonnull Key<?> key) {
    return key == myKey ? KeyFMap.EMPTY_MAP : this;
  }

  @Override
  public <V> V get(@Nonnull Key<V> key) {
    //noinspection unchecked
    return myKey == key ? (V)myValue : null;
  }

  @Nonnull
  @Override
  public Key[] getKeys() {
    return new Key[] { myKey };
  }

  @Override
  public String toString() {
    return "<" + myKey + " -> " + myValue+">";
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  public Key getKey() {
    return myKey;
  }

  public V getValue() {
    return myValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OneElementFMap)) return false;

    OneElementFMap map = (OneElementFMap)o;

    if (myKey != map.myKey) return false;
    if (!myValue.equals(map.myValue)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myKey.hashCode();
    result = 31 * result + myValue.hashCode();
    return result;
  }
}
