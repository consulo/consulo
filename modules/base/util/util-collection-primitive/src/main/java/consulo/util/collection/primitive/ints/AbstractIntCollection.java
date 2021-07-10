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
package consulo.util.collection.primitive.ints;

import consulo.util.collection.primitive.ints.impl.IntCollectionImpls;

import java.util.PrimitiveIterator;

/**
 * @author VISTALL
 * @since 05/06/2021
 */
public abstract class AbstractIntCollection implements IntCollection {
  @Override
  public boolean add(int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    PrimitiveIterator.OfInt it = iterator();
    while (it.hasNext()) {
      it.remove();
    }
  }

  @Override
  public boolean remove(int value) {
    PrimitiveIterator.OfInt it = iterator();
    while (it.hasNext()) {
      if (value == it.nextInt()) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean contains(int value) {
    PrimitiveIterator.OfInt it = iterator();
    while (it.hasNext()) {
      if (value == it.nextInt()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int[] toArray() {
    int[] ints = new int[size()];

    int i = 0;
    PrimitiveIterator.OfInt it = iterator();
    while (it.hasNext()) {
      ints[i++] = it.nextInt();
    }

    return ints;
  }

  @Override
  public int hashCode() {
    return IntCollectionImpls.hashCode(this);
  }
}
