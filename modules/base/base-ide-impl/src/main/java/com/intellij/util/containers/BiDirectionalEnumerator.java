/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class BiDirectionalEnumerator<T> extends Enumerator<T> {
  @Nonnull
  private final IntObjectMap<T> myIntToObjectMap;

  public BiDirectionalEnumerator(int expectNumber, @Nonnull HashingStrategy<T> strategy) {
    super(expectNumber, strategy);

    myIntToObjectMap = IntMaps.newIntObjectHashMap(expectNumber);
  }

  @Override
  public int enumerateImpl(T object) {
    int index = super.enumerateImpl(object);
    myIntToObjectMap.put(index, object);
    return index;
  }

  @Override
  public void clear() {
    super.clear();
    myIntToObjectMap.clear();
  }

  @Nonnull
  public T getValue(int index) {
    T hash = myIntToObjectMap.get(index);
    if (hash == null) {
      throw new RuntimeException("Can not find value by index " + index);
    }
    return hash;
  }

  public void forEachValue(@Nonnull Consumer<T> procedure) {
    myIntToObjectMap.values().forEach(procedure);
  }
}
