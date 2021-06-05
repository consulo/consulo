/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.util.collection.primitive.ints;

import javax.annotation.Nonnull;
import java.util.EmptyStackException;
import java.util.PrimitiveIterator;

public class IntStack implements IntCollection {
  private int[] data;
  private int size;
  public IntStack(int initialCapacity) {
    data = new int[initialCapacity];
    size = 0;
  }

  public IntStack() {
    this(5);
  }

  public void push(int t) {
    if (size >= data.length) {
      int[] newdata = new int[data.length * 3 / 2];
      System.arraycopy(data, 0, newdata, 0, size);
      data = newdata;
    }
    data[size++] = t;
  }

  public int peek() {
    if (size == 0) throw new EmptyStackException();
    return data[size - 1];
  }

  public int pop() {
    if (size == 0) throw new EmptyStackException();
    return data[--size];
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof IntStack) {
      IntStack otherStack = (IntStack)o;
      if (size != otherStack.size) return false;
      for (int i = 0; i < otherStack.size; i++) {
        if (data[i] != otherStack.data[i]) return false;
      }
      return true;
    }

    return false;
  }

  @Override
  public void clear() {
    size = 0;
  }

  @Override
  public boolean add(int value) {
    push(value);
    return true;
  }

  @Override
  public boolean remove(int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int[] toArray() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public PrimitiveIterator.OfInt iterator() {
    throw new UnsupportedOperationException();
  }
}
