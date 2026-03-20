/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.util.collection;

import org.jspecify.annotations.Nullable;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Immutable list in functional style
 *
 * @author nik
 */
public class FList<E> extends AbstractList<E> {
  @SuppressWarnings("unchecked")
  private static final FList<?> EMPTY_LIST = new FList(null, null, 0);

  private final @Nullable E myHead;
  private final @Nullable FList<E> myTail;
  private final int mySize;

  private FList(@Nullable E head, @Nullable FList<E> tail, int size) {
    myHead = head;
    myTail = tail;
    mySize = size;
  }

  @Override
  public @Nullable E get(int index) {
    if (index < 0 || index >= mySize) {
      throw new IndexOutOfBoundsException("index = " + index + ", size = " + mySize);
    }

    FList<E> current = this;
    while (index > 0) {
      current = Objects.requireNonNull(current).myTail;
      index--;
    }
    return Objects.requireNonNull(current).myHead;
  }

  public @Nullable E getHead() {
    return myHead;
  }

  public FList<E> prepend(E elem) {
    return new FList<E>(elem, this, mySize + 1);
  }

  public FList<E> without(@Nullable E elem) {
    FList<E> front = emptyList();

    FList<E> current = this;
    while (!Objects.requireNonNull(current).isEmpty()) {
      if (Objects.equals(elem, current.myHead)) {
        FList<E> result = Objects.requireNonNull(current.myTail);
        while (!Objects.requireNonNull(front).isEmpty()) {
          result = result.prepend(Objects.requireNonNull(front.myHead));
          front = front.myTail;
        }
        return result;
      }

      front = front.prepend(Objects.requireNonNull(current.myHead));
      current = current.myTail;
    }
    return this;
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {

      private FList<E> list = FList.this;

      @Override
      public boolean hasNext() {
        return list.size() > 0;
      }

      @Override
      public @Nullable E next() {
        if (list.size() == 0) throw new NoSuchElementException();

        E res = list.myHead;
        list = Objects.requireNonNull(list.getTail());

        return res;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public @Nullable FList<E> getTail() {
    return myTail;
  }

  @Override
  public int size() {
    return mySize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof FList that) {
      FList list1 = this;
      FList list2 = that;
      if (mySize != list2.mySize) return false;
      while (list1 != null) {
        if (!Objects.equals(list1.myHead, Objects.requireNonNull(list2).myHead)) return false;
        list1 = list1.getTail();
        list2 = list2.getTail();
        if (list1 == list2) return true;
      }
      return true;
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    int result = 1;
    FList each = this;
    while (each != null) {
      result = result * 31 + (each.myHead != null ? each.myHead.hashCode() : 0);
      each = each.getTail();
    }
    return result;
  }

  public static <E> FList<E> emptyList() {
    //noinspection unchecked
    return (FList<E>)EMPTY_LIST;
  }

  /**
   * Creates an FList object with the elements of the given sequence in the reversed order, i.e. the last element of {@code from} will be the result's {@link #getHead()}
   */
  public static <E> FList<E> createFromReversed(Iterable<E> from) {
    FList<E> result = emptyList();
    for (E e : from) {
      result = result.prepend(e);
    }
    return result;
  }
}
