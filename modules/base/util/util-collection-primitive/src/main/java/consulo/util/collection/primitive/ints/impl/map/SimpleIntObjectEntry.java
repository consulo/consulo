// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.collection.primitive.ints.impl.map;

import consulo.util.collection.primitive.ints.IntObjectMap;

import javax.annotation.Nonnull;

class SimpleIntObjectEntry<V> implements IntObjectMap.IntObjectEntry<V> {
  private final int myKey;
  private final V myValue;

  SimpleIntObjectEntry(int key, @Nonnull V value) {
    myKey = key;
    myValue = value;
  }

  @Override
  public int getKey() {
    return myKey;
  }

  @Nonnull
  @Override
  public V getValue() {
    return myValue;
  }
}