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

import consulo.util.collection.primitive.ints.IntSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import javax.annotation.Nonnull;
import java.util.PrimitiveIterator;

/**
 * @author VISTALL
 * @since 08/05/2021
 */
public class MyIntHashSet implements IntSet {
  private final TIntHashSet myDelegate;

  public MyIntHashSet() {
    myDelegate = new TIntHashSet();
  }

  public MyIntHashSet(int capacity) {
    myDelegate = new TIntHashSet(capacity);
  }

  public MyIntHashSet(int[] array) {
    myDelegate = new TIntHashSet(array);
  }

  @Override
  public boolean add(int value) {
    return myDelegate.add(value);
  }

  @Override
  public boolean remove(int value) {
    return myDelegate.remove(value);
  }

  @Override
  public boolean contains(int value) {
    return myDelegate.contains(value);
  }

  @Override
  public int[] toArray() {
    return myDelegate.toArray();
  }

  @Override
  public int size() {
    return myDelegate.size();
  }

  @Override
  public boolean isEmpty() {
    return myDelegate.isEmpty();
  }

  @Override
  public void clear() {
    myDelegate.clear();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof MyIntHashSet my) {
      return my.myDelegate.equals(myDelegate);
    }
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return myDelegate.toString();
  }

  @Override
  public int hashCode() {
    return myDelegate.hashCode();
  }

  @Nonnull
  @Override
  public PrimitiveIterator.OfInt iterator() {
    TIntIterator iterator = myDelegate.iterator();
    return new PrimitiveIterator.OfInt() {
      @Override
      public int nextInt() {
        return iterator.next();
      }

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }
}
