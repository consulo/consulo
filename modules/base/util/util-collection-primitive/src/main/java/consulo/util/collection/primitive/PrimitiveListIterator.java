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
package consulo.util.collection.primitive;

import java.util.ListIterator;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

/**
 * @author VISTALL
 * @since 08/05/2021
 */
public interface PrimitiveListIterator<T, T_CONS> extends ListIterator<T> {
  public static interface OfInt extends PrimitiveListIterator<Integer, IntConsumer>, PrimitiveIterator.OfInt {
    @Override
    default Integer next() {
      return PrimitiveIterator.OfInt.super.next();
    }

    @Override
    default Integer previous() {
      return previousInt();
    }

    int previousInt();

    @Override
    default void set(Integer value) {
      setInt(Objects.requireNonNull(value));
    }

    void setInt(int value);

    @Override
    default void add(Integer value) {
      addInt(Objects.requireNonNull(value));
    }

    void addInt(int value);
  }
}
