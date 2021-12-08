/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.util.collection.primitive.ints.impl.list;

import consulo.annotation.DeprecationInfo;
import consulo.util.collection.primitive.PrimitiveListIterator;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.impl.IntCollectionImpls;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.IntConsumer;

@Deprecated
@DeprecationInfo("Use IntLists")
public class IntArrayList implements Cloneable, IntList {
  /**
   * An optimized version of AbstractList.Itr
   */
  private class Itr implements PrimitiveIterator.OfInt {
    int cursor;       // index of next element to return
    int lastRet = -1; // index of last element returned; -1 if no such
    int expectedModCount = modCount;

    // prevent creating a synthetic constructor
    Itr() {
    }

    @Override
    public boolean hasNext() {
      return cursor != size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int nextInt() {
      checkForComodification();
      int i = cursor;
      if (i >= size) throw new NoSuchElementException();
      int[] elementData = IntArrayList.this.elementData;
      if (i >= elementData.length) throw new ConcurrentModificationException();
      cursor = i + 1;
      return elementData[lastRet = i];
    }

    @Override
    public void remove() {
      if (lastRet < 0) throw new IllegalStateException();
      checkForComodification();

      try {
        IntArrayList.this.remove(lastRet);
        cursor = lastRet;
        lastRet = -1;
        expectedModCount = modCount;
      }
      catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
      }
    }

    @Override
    public void forEachRemaining(IntConsumer action) {
      Objects.requireNonNull(action);
      final int size = IntArrayList.this.size;
      int i = cursor;
      if (i < size) {
        final int[] es = elementData;
        if (i >= es.length) throw new ConcurrentModificationException();
        for (; i < size && modCount == expectedModCount; i++) {
          action.accept(es[i]);
        }
        // update once at end to reduce heap write traffic
        cursor = i;
        lastRet = i - 1;
        checkForComodification();
      }
    }

    final void checkForComodification() {
      if (modCount != expectedModCount) throw new ConcurrentModificationException();
    }
  }

  /**
   * An optimized version of AbstractList.ListItr
   */
  private class ListItr extends Itr implements PrimitiveListIterator.OfInt {
    ListItr(int index) {
      super();
      cursor = index;
    }

    @Override
    public boolean hasPrevious() {
      return cursor != 0;
    }

    @Override
    public int nextIndex() {
      return cursor;
    }

    @Override
    public int previousIndex() {
      return cursor - 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int previousInt() {
      checkForComodification();
      int i = cursor - 1;
      if (i < 0) throw new NoSuchElementException();
      int[] elementData = IntArrayList.this.elementData;
      if (i >= elementData.length) throw new ConcurrentModificationException();
      cursor = i;
      return elementData[lastRet = i];
    }

    @Override
    public void setInt(int e) {
      if (lastRet < 0) throw new IllegalStateException();
      checkForComodification();

      try {
        IntArrayList.this.set(lastRet, e);
      }
      catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
      }
    }

    @Override
    public void addInt(int e) {
      checkForComodification();

      try {
        int i = cursor;
        IntArrayList.this.add(i, e);
        cursor = i + 1;
        lastRet = -1;
        expectedModCount = modCount;
      }
      catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
      }
    }
  }

  private static final int[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = new int[0];
  private static final int DEFAULT_CAPACITY = 10;

  private int[] elementData;
  private int size;
  private int modCount;

  public IntArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
      elementData = new int[initialCapacity];
    }
    else {
      elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }
  }

  public IntArrayList(int[] values) {
    elementData = values.clone();
    size = values.length;
  }

  public IntArrayList() {
    elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
  }

  public void trimToSize() {
    int oldCapacity = elementData.length;
    if (size == 0) {
      elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }
    else if (size < oldCapacity) {
      int[] oldData = elementData;
      elementData = new int[size];
      System.arraycopy(oldData, 0, elementData, 0, size);
    }
  }

  public void ensureCapacity(int minCapacity) {
    int oldCapacity = elementData.length;
    if (minCapacity > oldCapacity) {
      int[] oldData = elementData;
      int newCapacity = oldCapacity * 3 / 2 + 1;
      if (newCapacity < minCapacity) {
        newCapacity = minCapacity;
      }
      elementData = new int[newCapacity];
      System.arraycopy(oldData, 0, elementData, 0, size);
    }
  }

  public void fill(int fromIndex, int toIndex, int value) {
    if (toIndex > size) {
      ensureCapacity(toIndex);
      size = toIndex;
    }
    Arrays.fill(elementData, fromIndex, toIndex, value);
  }

  public void add(@Nonnull int[] values) {
    int length = values.length;
    ensureCapacity(size + length);
    System.arraycopy(values, 0, elementData, size, length);
    size += length;

    modCount++;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public boolean contains(int elem) {
    return indexOf(elem) >= 0;
  }

  @Override
  public int indexOf(int elem) {
    for (int i = 0; i < size; i++) {
      if (elem == elementData[i]) return i;
    }
    return -1;
  }

  @Override
  public boolean remove(int value) {
    int index = indexOf(value);
    if (index == -1) {
      return false;
    }
    removeByIndex(index);
    return true;
  }

  public int indexOf(int elem, int startIndex, int endIndex) {
    if (startIndex < 0 || endIndex < startIndex || endIndex >= size) {
      throw new IndexOutOfBoundsException("startIndex: " + startIndex + "; endIndex: " + endIndex + "; mySize: " + size);
    }
    for (int i = startIndex; i < endIndex; i++) {
      if (elem == elementData[i]) return i;
    }
    return -1;
  }

  public int lastIndexOf(int elem) {
    for (int i = size - 1; i >= 0; i--) {
      if (elem == elementData[i]) return i;
    }
    return -1;
  }

  @Override
  public Object clone() {
    try {
      IntArrayList v = (IntArrayList)super.clone();
      v.elementData = new int[size];
      System.arraycopy(elementData, 0, v.elementData, 0, size);
      return v;
    }
    catch (CloneNotSupportedException e) {
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  @Override
  @Nonnull
  public int[] toArray() {
    int[] result = new int[size];
    System.arraycopy(elementData, 0, result, 0, size);
    return result;
  }

  @Nonnull
  public int[] toArray(@Nonnull int[] a) {
    if (a.length < size) {
      a = new int[size];
    }

    System.arraycopy(elementData, 0, a, 0, size);

    return a;
  }

  @Nonnull
  public int[] toArray(int startIndex, int length) {
    int[] result = new int[length];
    System.arraycopy(elementData, startIndex, result, 0, length);
    return result;
  }

  @Override
  public int get(int index) {
    checkRange(index);
    return elementData[index];
  }

  @Override
  public int set(int index, int element) {
    checkRange(index);

    int oldValue = elementData[index];
    elementData[index] = element;
    return oldValue;
  }

  public void setQuick(int index, int element) {
    elementData[index] = element;
  }

  @Override
  public boolean add(int o) {
    ensureCapacity(size + 1);
    elementData[size++] = o;
    return true;
  }

  @Override
  public void add(int index, int element) {
    if (index > size || index < 0) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    ensureCapacity(size + 1);
    System.arraycopy(elementData, index, elementData, index + 1, size - index);
    elementData[index] = element;
    size++;
  }

  @Override
  public boolean addAll(int index, int[] a) {
    rangeCheckForAdd(index);

    modCount++;
    int numNew = a.length;
    if (numNew == 0) return false;
    int[] elementData;
    final int s;
    if (numNew > (elementData = this.elementData).length - (s = size)) elementData = grow(s + numNew);

    int numMoved = s - index;
    if (numMoved > 0) System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
    System.arraycopy(a, 0, elementData, index, numNew);
    size = s + numNew;
    return true;
  }

  /**
   * Increases the capacity to ensure that it can hold at least the
   * number of elements specified by the minimum capacity argument.
   *
   * @param minCapacity the desired minimum capacity
   * @throws OutOfMemoryError if minCapacity is less than zero
   */
  private int[] grow(int minCapacity) {
    int oldCapacity = elementData.length;
    if (oldCapacity > 0 || elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
      int newCapacity = newLength(oldCapacity, minCapacity - oldCapacity, /* minimum growth */
                                  oldCapacity >> 1           /* preferred growth */);
      return elementData = Arrays.copyOf(elementData, newCapacity);
    }
    else {
      return elementData = new int[Math.max(DEFAULT_CAPACITY, minCapacity)];
    }
  }

  /**
   * The maximum length of array to allocate (unless necessary).
   * Some VMs reserve some header words in an array.
   * Attempts to allocate larger arrays may result in
   * {@code OutOfMemoryError: Requested array size exceeds VM limit}
   */
  public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

  public static int newLength(int oldLength, int minGrowth, int prefGrowth) {
    // assert oldLength >= 0
    // assert minGrowth > 0

    int newLength = Math.max(minGrowth, prefGrowth) + oldLength;
    if (newLength - MAX_ARRAY_LENGTH <= 0) {
      return newLength;
    }
    return hugeLength(oldLength, minGrowth);
  }

  private static int hugeLength(int oldLength, int minGrowth) {
    int minLength = oldLength + minGrowth;
    if (minLength < 0) { // overflow
      throw new OutOfMemoryError("Required array length too large");
    }
    if (minLength <= MAX_ARRAY_LENGTH) {
      return MAX_ARRAY_LENGTH;
    }
    return Integer.MAX_VALUE;
  }

  private int[] grow() {
    return grow(size + 1);
  }


  @Override
  public int removeByIndex(int index) {
    checkRange(index);

    int oldValue = elementData[index];

    int numMoved = size - index - 1;
    if (numMoved > 0) {
      System.arraycopy(elementData, index + 1, elementData, index, numMoved);
    }
    size--;

    modCount++;
    return oldValue;
  }

  @Override
  public void clear() {
    size = 0;
  }

  @Override
  public void removeRange(int fromIndex, int toIndex) {
    int numMoved = size - toIndex;
    System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);
    size -= toIndex - fromIndex;
  }

  public void copyRange(int fromIndex, int length, int toIndex) {
    if (length < 0 || fromIndex < 0 || fromIndex + length > size || toIndex < 0 || toIndex + length > size) {
      throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + "; length: " + length + "; toIndex: " + toIndex + "; mySize: " + size);
    }
    System.arraycopy(elementData, fromIndex, elementData, toIndex, length);
  }

  /**
   * A version of rangeCheck used by add and addAll.
   */
  private void rangeCheckForAdd(int index) {
    if (index > size || index < 0) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
  }

  /**
   * Constructs an IndexOutOfBoundsException detail message.
   * Of the many possible refactorings of the error handling code,
   * this "outlining" performs best with both server and client VMs.
   */
  private String outOfBoundsMsg(int index) {
    return "Index: " + index + ", Size: " + size;
  }

  private void checkRange(int index) {
    if (index >= size || index < 0) {
      //noinspection HardCodedStringLiteral
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  @Override
  public void sort() {
    int oldModCount = this.modCount;
    Arrays.sort(elementData, 0, size);
    if(oldModCount != this.modCount) {
      throw new ConcurrentModificationException();
    }
    this.modCount ++;
  }

  @Override
  public String toString() {
    return Arrays.toString(toArray());
  }

  @Nonnull
  @Override
  public PrimitiveIterator.OfInt iterator() {
    return new Itr();
  }

  @Override
  public PrimitiveListIterator.OfInt listIterator() {
    return new ListItr(0);
  }

  @Override
  public PrimitiveListIterator.OfInt listIterator(int index) {
    return new ListItr(index);
  }

  @Override
  public int hashCode() {
    return IntCollectionImpls.hashCode(this);
  }
}
