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
package consulo.util.collection.primitive.ints.impl.set;

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.impl.IntCollectionImpls;

import javax.annotation.Nonnull;
import java.util.PrimitiveIterator;

/**
 * @author VISTALL
 * @since 08/05/2021
 */
public class EmptyIntSet implements IntSet {
  public static final EmptyIntSet INSTANCE = new EmptyIntSet();

  @Override
  public boolean add(int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(int value) {
    return false;
  }

  @Override
  public int[] toArray() {
    return ArrayUtil.EMPTY_INT_ARRAY;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public void clear() {

  }

  @Nonnull
  @Override
  public PrimitiveIterator.OfInt iterator() {
    return new PrimitiveIterator.OfInt() {
      @Override
      public int nextInt() {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean hasNext() {
        return false;
      }
    };
  }

  @Override
  public int hashCode() {
    return IntCollectionImpls.hashCode(this);
  }
}
