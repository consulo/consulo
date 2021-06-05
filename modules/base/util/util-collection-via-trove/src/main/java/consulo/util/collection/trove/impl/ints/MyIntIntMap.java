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
package consulo.util.collection.trove.impl.ints;

import consulo.util.collection.primitive.ints.BiIntConsumer;
import consulo.util.collection.primitive.ints.IntIntMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08/05/2021
 */
public class MyIntIntMap extends TIntIntHashMap implements IntIntMap {
  public MyIntIntMap() {
  }

  public MyIntIntMap(int initialCapacity) {
    super(initialCapacity);
  }

  @Override
  public void putInt(int key, int value) {
    put(key, value);
  }

  @Override
  public int getInt(int key) {
    return get(key);
  }

  @Override
  public void forEach(@Nonnull BiIntConsumer consumer) {
    TIntIntIterator iterator = iterator();

    while (iterator.hasNext()) {
      iterator.advance();

      int key = iterator.key();
      int value = iterator.value();

      consumer.accept(key, value);
    }
  }
}
