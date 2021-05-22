/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.openapi.util.Comparing;
import com.intellij.util.text.CharArrayCharSequence;
import consulo.util.collection.HashingStrategy;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Author: msk
 */
@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class ArrayUtil extends ArrayUtilRt {
  public static final short[] EMPTY_SHORT_ARRAY = ArrayUtilRt.EMPTY_SHORT_ARRAY;
  public static final char[] EMPTY_CHAR_ARRAY = ArrayUtilRt.EMPTY_CHAR_ARRAY;
  public static final byte[] EMPTY_BYTE_ARRAY = ArrayUtilRt.EMPTY_BYTE_ARRAY;
  public static final int[] EMPTY_INT_ARRAY = ArrayUtilRt.EMPTY_INT_ARRAY;
  public static final boolean[] EMPTY_BOOLEAN_ARRAY = ArrayUtilRt.EMPTY_BOOLEAN_ARRAY;
  public static final Object[] EMPTY_OBJECT_ARRAY = ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  public static final String[] EMPTY_STRING_ARRAY = ArrayUtilRt.EMPTY_STRING_ARRAY;
  public static final Class[] EMPTY_CLASS_ARRAY = ArrayUtilRt.EMPTY_CLASS_ARRAY;
  public static final long[] EMPTY_LONG_ARRAY = ArrayUtilRt.EMPTY_LONG_ARRAY;
  public static final Collection[] EMPTY_COLLECTION_ARRAY = ArrayUtilRt.EMPTY_COLLECTION_ARRAY;
  public static final File[] EMPTY_FILE_ARRAY = ArrayUtilRt.EMPTY_FILE_ARRAY;
  public static final Runnable[] EMPTY_RUNNABLE_ARRAY = ArrayUtilRt.EMPTY_RUNNABLE_ARRAY;
  public static final CharSequence EMPTY_CHAR_SEQUENCE = new CharArrayCharSequence(EMPTY_CHAR_ARRAY);

  public static final ArrayFactory<String> STRING_ARRAY_FACTORY = new ArrayFactory<String>() {
    @Nonnull
    @Override
    public String[] create(int count) {
      return newStringArray(count);
    }
  };
  public static final ArrayFactory<Object> OBJECT_ARRAY_FACTORY = new ArrayFactory<Object>() {
    @Nonnull
    @Override
    public Object[] create(int count) {
      return newObjectArray(count);
    }
  };

  private ArrayUtil() {
  }

  @Nonnull
  @Contract(pure = true)
  public static byte[] realloc(@Nonnull byte[] array, final int newSize) {
    if (newSize == 0) {
      return EMPTY_BYTE_ARRAY;
    }

    final int oldSize = array.length;
    if (oldSize == newSize) {
      return array;
    }

    final byte[] result = new byte[newSize];
    System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static short[] realloc(@Nonnull short[] array, final int newSize) {
    if (newSize == 0) {
      return ArrayUtilRt.EMPTY_SHORT_ARRAY;
    }

    final int oldSize = array.length;
    return oldSize == newSize ? array : Arrays.copyOf(array, newSize);
  }

  @Nonnull
  @Contract(pure = true)
  public static boolean[] realloc(@Nonnull boolean[] array, final int newSize) {
    if (newSize == 0) {
      return EMPTY_BOOLEAN_ARRAY;
    }

    final int oldSize = array.length;
    if (oldSize == newSize) {
      return array;
    }

    boolean[] result = new boolean[newSize];
    System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static long[] realloc(@Nonnull long[] array, int newSize) {
    if (newSize == 0) {
      return EMPTY_LONG_ARRAY;
    }

    final int oldSize = array.length;
    if (oldSize == newSize) {
      return array;
    }

    long[] result = new long[newSize];
    System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] realloc(@Nonnull int[] array, final int newSize) {
    if (newSize == 0) {
      return EMPTY_INT_ARRAY;
    }

    final int oldSize = array.length;
    if (oldSize == newSize) {
      return array;
    }

    final int[] result = new int[newSize];
    System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] realloc(@Nonnull T[] array, final int newSize, @Nonnull ArrayFactory<T> factory) {
    final int oldSize = array.length;
    if (oldSize == newSize) {
      return array;
    }

    T[] result = factory.create(newSize);
    if (newSize == 0) {
      return result;
    }

    System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static long[] append(@Nonnull long[] array, long value) {
    array = realloc(array, array.length + 1);
    array[array.length - 1] = value;
    return array;
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] append(@Nonnull int[] array, int value) {
    array = realloc(array, array.length + 1);
    array[array.length - 1] = value;
    return array;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] insert(@Nonnull T[] array, int index, T value) {
    T[] result = newArray(getComponentType(array), array.length + 1);
    System.arraycopy(array, 0, result, 0, index);
    result[index] = value;
    System.arraycopy(array, index, result, index + 1, array.length - index);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] insert(@Nonnull int[] array, int index, int value) {
    int[] result = new int[array.length + 1];
    System.arraycopy(array, 0, result, 0, index);
    result[index] = value;
    System.arraycopy(array, index, result, index + 1, array.length - index);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static byte[] append(@Nonnull byte[] array, byte value) {
    array = realloc(array, array.length + 1);
    array[array.length - 1] = value;
    return array;
  }

  @Nonnull
  @Contract(pure = true)
  public static boolean[] append(@Nonnull boolean[] array, boolean value) {
    array = realloc(array, array.length + 1);
    array[array.length - 1] = value;
    return array;
  }

  @Nonnull
  @Contract(pure = true)
  public static char[] realloc(@Nonnull char[] array, final int newSize) {
    if (newSize == 0) {
      return EMPTY_CHAR_ARRAY;
    }

    final int oldSize = array.length;
    if (oldSize == newSize) {
      return array;
    }

    final char[] result = new char[newSize];
    System.arraycopy(array, 0, result, 0, Math.min(oldSize, newSize));
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] toObjectArray(@Nonnull Collection<? extends T> collection, @Nonnull Class<T> aClass) {
    T[] array = newArray(aClass, collection.size());
    return collection.toArray(array);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] toObjectArray(@Nonnull Class<T> aClass, @Nonnull Object... source) {
    T[] array = newArray(aClass, source.length);
    //noinspection SuspiciousSystemArraycopy
    System.arraycopy(source, 0, array, 0, array.length);
    return array;
  }

  @Nonnull
  @Contract(pure = true)
  public static Object[] toObjectArray(@Nonnull Collection<?> collection) {
    if (collection.isEmpty()) return EMPTY_OBJECT_ARRAY;
    //noinspection SSBasedInspection
    return collection.toArray(new Object[collection.size()]);
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] toIntArray(@Nonnull Collection<Integer> list) {
    int[] ret = newIntArray(list.size());
    int i = 0;
    for (Integer e : list) {
      ret[i++] = e.intValue();
    }
    return ret;
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] toIntArray(@Nonnull byte[] byteArray) {
    int[] ret = newIntArray(byteArray.length);
    for (int i = 0; i < byteArray.length; i++) {
      ret[i] = byteArray[i];
    }
    return ret;
  }


  @Nonnull
  @Contract(pure = true)
  public static <T> T[] mergeArrays(@Nonnull T[] a1, @Nonnull T[] a2) {
    if (a1.length == 0) {
      return a2;
    }
    if (a2.length == 0) {
      return a1;
    }

    final Class<T> class1 = getComponentType(a1);
    final Class<T> class2 = getComponentType(a2);
    final Class<T> aClass = class1.isAssignableFrom(class2) ? class1 : class2;

    T[] result = newArray(aClass, a1.length + a2.length);
    System.arraycopy(a1, 0, result, 0, a1.length);
    System.arraycopy(a2, 0, result, a1.length, a2.length);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] mergeCollections(@Nonnull Collection<? extends T> c1, @Nonnull Collection<? extends T> c2, @Nonnull ArrayFactory<T> factory) {
    T[] res = factory.create(c1.size() + c2.size());

    int i = 0;

    for (T t : c1) {
      res[i++] = t;
    }

    for (T t : c2) {
      res[i++] = t;
    }

    return res;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] mergeArrays(@Nonnull T[] a1, @Nonnull T[] a2, @Nonnull ArrayFactory<T> factory) {
    if (a1.length == 0) {
      return a2;
    }
    if (a2.length == 0) {
      return a1;
    }
    T[] result = factory.create(a1.length + a2.length);
    System.arraycopy(a1, 0, result, 0, a1.length);
    System.arraycopy(a2, 0, result, a1.length, a2.length);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static String[] mergeArrays(@Nonnull String[] a1, @Nonnull String... a2) {
    return mergeArrays(a1, a2, STRING_ARRAY_FACTORY);
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] mergeArrays(@Nonnull int[] a1, @Nonnull int[] a2) {
    if (a1.length == 0) {
      return a2;
    }
    if (a2.length == 0) {
      return a1;
    }
    int[] result = new int[a1.length + a2.length];
    System.arraycopy(a1, 0, result, 0, a1.length);
    System.arraycopy(a2, 0, result, a1.length, a2.length);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static byte[] mergeArrays(@Nonnull byte[] a1, @Nonnull byte[] a2) {
    if (a1.length == 0) {
      return a2;
    }
    if (a2.length == 0) {
      return a1;
    }
    byte[] result = new byte[a1.length + a2.length];
    System.arraycopy(a1, 0, result, 0, a1.length);
    System.arraycopy(a2, 0, result, a1.length, a2.length);
    return result;
  }

  /**
   * Allocates new array of size {@code array.length + collection.size()} and copies elements of {@code array} and
   * {@code collection} to it.
   *
   * @param array      source array
   * @param collection source collection
   * @param factory    array factory used to create destination array of type {@code T}
   * @return destination array
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> T[] mergeArrayAndCollection(@Nonnull T[] array, @Nonnull Collection<T> collection, @Nonnull final ArrayFactory<T> factory) {
    if (collection.isEmpty()) {
      return array;
    }

    final T[] array2;
    try {
      array2 = collection.toArray(factory.create(collection.size()));
    }
    catch (ArrayStoreException e) {
      throw new RuntimeException("Bad elements in collection: " + collection, e);
    }

    if (array.length == 0) {
      return array2;
    }

    final T[] result = factory.create(array.length + collection.size());
    System.arraycopy(array, 0, result, 0, array.length);
    System.arraycopy(array2, 0, result, array.length, array2.length);
    return result;
  }

  /**
   * Appends {@code element} to the {@code src} array. As you can
   * imagine the appended element will be the last one in the returned result.
   *
   * @param src     array to which the {@code element} should be appended.
   * @param element object to be appended to the end of {@code src} array.
   * @return new array
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> T[] append(@Nonnull final T[] src, @Nullable final T element) {
    //noinspection unchecked
    return append(src, element, (Class<T>)src.getClass().getComponentType());
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] prepend(final T element, @Nonnull final T[] array) {
    //noinspection unchecked
    return prepend(element, array, (Class<T>)array.getClass().getComponentType());
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] prepend(T element, @Nonnull T[] array, @Nonnull Class<T> type) {
    int length = array.length;
    T[] result = newArray(type, length + 1);
    System.arraycopy(array, 0, result, 1, length);
    result[0] = element;
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] prepend(final T element, @Nonnull final T[] src, @Nonnull ArrayFactory<T> factory) {
    int length = src.length;
    T[] result = factory.create(length + 1);
    System.arraycopy(src, 0, result, 1, length);
    result[0] = element;
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static byte[] prepend(byte element, @Nonnull byte[] array) {
    int length = array.length;
    final byte[] result = new byte[length + 1];
    result[0] = element;
    System.arraycopy(array, 0, result, 1, length);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] append(@Nonnull final T[] src, final T element, @Nonnull ArrayFactory<T> factory) {
    int length = src.length;
    T[] result = factory.create(length + 1);
    System.arraycopy(src, 0, result, 0, length);
    result[length] = element;
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] append(@Nonnull T[] src, @Nullable final T element, @Nonnull Class<T> componentType) {
    int length = src.length;
    T[] result = newArray(componentType, length + 1);
    System.arraycopy(src, 0, result, 0, length);
    result[length] = element;
    return result;
  }

  /**
   * Removes element with index {@code idx} from array {@code src}.
   *
   * @param src array.
   * @param idx index of element to be removed.
   * @return modified array.
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> T[] remove(@Nonnull final T[] src, int idx) {
    int length = src.length;
    if (idx < 0 || idx >= length) {
      throw new IllegalArgumentException("invalid index: " + idx);
    }
    T[] result = newArray(getComponentType(src), length - 1);
    System.arraycopy(src, 0, result, 0, idx);
    System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
    return result;
  }

  @Nonnull
  public static <T> T[] newArray(@Nonnull Class<T> type, int length) {
    //noinspection unchecked
    return (T[])Array.newInstance(type, length);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] remove(@Nonnull final T[] src, int idx, @Nonnull ArrayFactory<T> factory) {
    int length = src.length;
    if (idx < 0 || idx >= length) {
      throw new IllegalArgumentException("invalid index: " + idx);
    }
    T[] result = factory.create(length - 1);
    System.arraycopy(src, 0, result, 0, idx);
    System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] remove(@Nonnull final T[] src, T element) {
    final int idx = find(src, element);
    if (idx == -1) return src;

    return remove(src, idx);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] remove(@Nonnull final T[] src, T element, @Nonnull ArrayFactory<T> factory) {
    final int idx = find(src, element);
    if (idx == -1) return src;

    return remove(src, idx, factory);
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] remove(@Nonnull final int[] src, int idx) {
    int length = src.length;
    if (idx < 0 || idx >= length) {
      throw new IllegalArgumentException("invalid index: " + idx);
    }
    int[] result = newIntArray(src.length - 1);
    System.arraycopy(src, 0, result, 0, idx);
    System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static short[] remove(@Nonnull final short[] src, int idx) {
    int length = src.length;
    if (idx < 0 || idx >= length) {
      throw new IllegalArgumentException("invalid index: " + idx);
    }
    short[] result = src.length == 1 ? EMPTY_SHORT_ARRAY : new short[src.length - 1];
    System.arraycopy(src, 0, result, 0, idx);
    System.arraycopy(src, idx + 1, result, idx, length - idx - 1);
    return result;
  }

  @Contract(pure = true)
  public static int find(@Nonnull int[] src, int obj) {
    return indexOf(src, obj);
  }

  @Contract(pure = true)
  public static <T> int find(@Nonnull final T[] src, final T obj) {
    return ArrayUtilRt.find(src, obj);
  }

  @Contract(pure = true)
  public static boolean startsWith(@Nonnull byte[] array, @Nonnull byte[] prefix) {
    //noinspection ArrayEquality
    if (array == prefix) {
      return true;
    }
    int length = prefix.length;
    if (array.length < length) {
      return false;
    }

    for (int i = 0; i < length; i++) {
      if (array[i] != prefix[i]) {
        return false;
      }
    }

    return true;
  }

  @Contract(pure = true)
  public static <E> boolean startsWith(@Nonnull E[] array, @Nonnull E[] subArray) {
    //noinspection ArrayEquality
    if (array == subArray) {
      return true;
    }
    int length = subArray.length;
    if (array.length < length) {
      return false;
    }

    for (int i = 0; i < length; i++) {
      if (!Comparing.equal(array[i], subArray[i])) {
        return false;
      }
    }

    return true;
  }

  @Contract(pure = true)
  public static boolean startsWith(@Nonnull byte[] array, int start, @Nonnull byte[] subArray) {
    int length = subArray.length;
    if (array.length - start < length) {
      return false;
    }

    for (int i = 0; i < length; i++) {
      if (array[start + i] != subArray[i]) {
        return false;
      }
    }

    return true;
  }

  @Contract(pure = true)
  public static <T> boolean equals(@Nonnull T[] a1, @Nonnull T[] a2, @Nonnull HashingStrategy<? super T> comparator) {
    //noinspection ArrayEquality
    if (a1 == a2) {
      return true;
    }

    int length = a2.length;
    if (a1.length != length) {
      return false;
    }

    for (int i = 0; i < length; i++) {
      if (!comparator.equals(a1[i], a2[i])) {
        return false;
      }
    }
    return true;
  }

  @Contract(pure = true)
  public static <T> boolean equals(@Nonnull T[] a1, @Nonnull T[] a2, @Nonnull Comparator<? super T> comparator) {
    //noinspection ArrayEquality
    if (a1 == a2) {
      return true;
    }
    int length = a2.length;
    if (a1.length != length) {
      return false;
    }

    for (int i = 0; i < length; i++) {
      if (comparator.compare(a1[i], a2[i]) != 0) {
        return false;
      }
    }
    return true;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] reverseArray(@Nonnull T[] array) {
    T[] newArray = array.clone();
    for (int i = 0; i < array.length; i++) {
      newArray[array.length - i - 1] = array[i];
    }
    return newArray;
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] reverseArray(@Nonnull int[] array) {
    int[] newArray = array.clone();
    for (int i = 0; i < array.length; i++) {
      newArray[array.length - i - 1] = array[i];
    }
    return newArray;
  }

  @Contract(pure = true)
  public static int lexicographicCompare(@Nonnull int[] obj1, @Nonnull int[] obj2) {
    for (int i = 0; i < Math.min(obj1.length, obj2.length); i++) {
      int res = Integer.compare(obj1[i], obj2[i]);
      if (res != 0) return res;
    }
    return Integer.compare(obj1.length, obj2.length);
  }

  @Contract(pure = true)
  public static int lexicographicCompare(@Nonnull String[] obj1, @Nonnull String[] obj2) {
    for (int i = 0; i < Math.max(obj1.length, obj2.length); i++) {
      String o1 = i < obj1.length ? obj1[i] : null;
      String o2 = i < obj2.length ? obj2[i] : null;
      if (o1 == null) return -1;
      if (o2 == null) return 1;
      int res = o1.compareToIgnoreCase(o2);
      if (res != 0) return res;
    }
    return 0;
  }

  //must be Comparables
  @Contract(pure = true)
  public static <T> int lexicographicCompare(@Nonnull T[] obj1, @Nonnull T[] obj2) {
    for (int i = 0; i < Math.max(obj1.length, obj2.length); i++) {
      T o1 = i < obj1.length ? obj1[i] : null;
      T o2 = i < obj2.length ? obj2[i] : null;
      if (o1 == null) return -1;
      if (o2 == null) return 1;
      //noinspection unchecked
      int res = ((Comparable)o1).compareTo(o2);
      if (res != 0) return res;
    }
    return 0;
  }

  public static <T> void swap(@Nonnull T[] array, int i1, int i2) {
    final T t = array[i1];
    array[i1] = array[i2];
    array[i2] = t;
  }

  public static void swap(@Nonnull int[] array, int i1, int i2) {
    final int t = array[i1];
    array[i1] = array[i2];
    array[i2] = t;
  }

  public static void swap(@Nonnull boolean[] array, int i1, int i2) {
    final boolean t = array[i1];
    array[i1] = array[i2];
    array[i2] = t;
  }

  public static void swap(@Nonnull char[] array, int i1, int i2) {
    final char t = array[i1];
    array[i1] = array[i2];
    array[i2] = t;
  }

  public static <T> void rotateLeft(@Nonnull T[] array, int i1, int i2) {
    final T t = array[i1];
    System.arraycopy(array, i1 + 1, array, i1, i2 - i1);
    array[i2] = t;
  }

  public static <T> void rotateRight(@Nonnull T[] array, int i1, int i2) {
    final T t = array[i2];
    System.arraycopy(array, i1, array, i1 + 1, i2 - i1);
    array[i1] = t;
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull Object[] objects, @Nullable Object object) {
    return indexOf(objects, object, 0, objects.length);
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull Object[] objects, Object object, int start, int end) {
    if (object == null) {
      for (int i = start; i < end; i++) {
        if (objects[i] == null) return i;
      }
    }
    else {
      for (int i = start; i < end; i++) {
        if (object.equals(objects[i])) return i;
      }
    }
    return -1;
  }

  @Contract(pure = true)
  public static <T> int indexOf(@Nonnull List<T> objects, T object, @Nonnull HashingStrategy<T> comparator) {
    for (int i = 0; i < objects.size(); i++) {
      if (comparator.equals(objects.get(i), object)) return i;
    }
    return -1;
  }

  @Contract(pure = true)
  public static <T> int indexOf(@Nonnull List<T> objects, T object, @Nonnull Comparator<T> comparator) {
    for (int i = 0; i < objects.size(); i++) {
      if (comparator.compare(objects.get(i), object) == 0) return i;
    }
    return -1;
  }

  @Contract(pure = true)
  public static <T> int indexOf(@Nonnull T[] objects, T object, @Nonnull HashingStrategy<T> comparator) {
    for (int i = 0; i < objects.length; i++) {
      if (comparator.equals(objects[i], object)) return i;
    }
    return -1;
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull long[] ints, long value) {
    for (int i = 0; i < ints.length; i++) {
      if (ints[i] == value) return i;
    }

    return -1;
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull int[] ints, int value) {
    for (int i = 0; i < ints.length; i++) {
      if (ints[i] == value) return i;
    }

    return -1;
  }

  @Contract(pure = true)
  public static int indexOf(@Nonnull short[] ints, short value) {
    for (int i = 0; i < ints.length; i++) {
      if (ints[i] == value) return i;
    }

    return -1;
  }

  @Contract(pure = true)
  public static <T> int lastIndexOf(@Nonnull final T[] src, final T obj) {
    for (int i = src.length - 1; i >= 0; i--) {
      final T o = src[i];
      if (o == null) {
        if (obj == null) {
          return i;
        }
      }
      else {
        if (o.equals(obj)) {
          return i;
        }
      }
    }
    return -1;
  }

  @Contract(pure = true)
  public static int lastIndexOf(@Nonnull final int[] src, final int obj) {
    for (int i = src.length - 1; i >= 0; i--) {
      final int o = src[i];
      if (o == obj) {
        return i;
      }
    }
    return -1;
  }

  @Contract(pure = true)
  public static <T> int lastIndexOf(@Nonnull final T[] src, final T obj, @Nonnull HashingStrategy<? super T> comparator) {
    for (int i = src.length - 1; i >= 0; i--) {
      final T o = src[i];
      if (comparator.equals(obj, o)) {
        return i;
      }
    }
    return -1;
  }

  @Contract(pure = true)
  public static <T> int lastIndexOf(@Nonnull List<T> src, final T obj, @Nonnull HashingStrategy<? super T> comparator) {
    for (int i = src.size() - 1; i >= 0; i--) {
      final T o = src.get(i);
      if (comparator.equals(obj, o)) {
        return i;
      }
    }
    return -1;
  }

  @Contract(pure = true)
  public static int lastIndexOfNot(@Nonnull final int[] src, final int obj) {
    for (int i = src.length - 1; i >= 0; i--) {
      final int o = src[i];
      if (o != obj) {
        return i;
      }
    }
    return -1;
  }

  @Contract(pure = true)
  public static <T> boolean contains(@Nullable final T o, @Nonnull T... objects) {
    return indexOf(objects, o) >= 0;
  }

  @Contract(pure = true)
  public static boolean contains(@Nullable final String s, @Nonnull String... strings) {
    if (s == null) {
      for (String str : strings) {
        if (str == null) return true;
      }
    }
    else {
      for (String str : strings) {
        if (s.equals(str)) return true;
      }
    }

    return false;
  }

  @Nonnull
  @Contract(pure = true)
  public static int[] newIntArray(int count) {
    return count == 0 ? EMPTY_INT_ARRAY : new int[count];
  }

  @Nonnull
  @Contract(pure = true)
  public static long[] newLongArray(int count) {
    return count == 0 ? EMPTY_LONG_ARRAY : new long[count];
  }

  @Nonnull
  @Contract(pure = true)
  public static String[] newStringArray(int count) {
    return count == 0 ? EMPTY_STRING_ARRAY : new String[count];
  }

  @Nonnull
  @Contract(pure = true)
  public static Object[] newObjectArray(int count) {
    return count == 0 ? EMPTY_OBJECT_ARRAY : new Object[count];
  }

  @Nonnull
  @Contract(pure = true)
  public static <E> E[] ensureExactSize(int count, @Nonnull E[] sample) {
    if (count == sample.length) return sample;
    return newArray(getComponentType(sample), count);
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T getFirstElement(@Nullable T[] array) {
    return array != null && array.length > 0 ? array[0] : null;
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T getLastElement(@Nullable T[] array) {
    return array != null && array.length > 0 ? array[array.length - 1] : null;
  }

  @Contract(pure = true)
  public static int getLastElement(@Nullable int[] array, int defaultValue) {
    return array == null || array.length == 0 ? defaultValue : array[array.length - 1];
  }

  @Contract(value = "null -> true", pure = true)
  public static <T> boolean isEmpty(@Nullable T[] array) {
    return array == null || array.length == 0;
  }

  @Nonnull
  @Contract(pure = true)
  public static String[] toStringArray(@Nullable Collection<String> collection) {
    return ArrayUtilRt.toStringArray(collection);
  }

  public static <T> void copy(@Nonnull final Collection<? extends T> src, @Nonnull final T[] dst, final int dstOffset) {
    int i = dstOffset;
    for (T t : src) {
      dst[i++] = t;
    }
  }

  @Nullable
  @Contract("null -> null; !null -> !null")
  public static <T> T[] copyOf(@Nullable T[] original) {
    if (original == null) return null;
    return Arrays.copyOf(original, original.length);
  }

  @Nullable
  @Contract("null -> null; !null -> !null")
  public static boolean[] copyOf(@Nullable boolean[] original) {
    if (original == null) return null;
    return Arrays.copyOf(original, original.length);
  }

  @Nullable
  @Contract("null -> null; !null -> !null")
  public static int[] copyOf(@Nullable int[] original) {
    if (original == null) return null;
    return Arrays.copyOf(original, original.length);
  }

  @Nonnull
  public static <T> T[] stripTrailingNulls(@Nonnull T[] array) {
    return array.length != 0 && array[array.length - 1] == null ? Arrays.copyOf(array, trailingNullsIndex(array)) : array;
  }

  private static <T> int trailingNullsIndex(@Nonnull T[] array) {
    for (int i = array.length - 1; i >= 0; i--) {
      if (array[i] != null) {
        return i + 1;
      }
    }
    return 0;
  }

  // calculates average of the median values in the selected part of the array. E.g. for part=3 returns average in the middle third.
  public static long averageAmongMedians(@Nonnull long[] time, int part) {
    assert part >= 1;
    int n = time.length;
    Arrays.sort(time);
    long total = 0;
    for (int i = n / 2 - n / part / 2; i < n / 2 + n / part / 2; i++) {
      total += time[i];
    }
    int middlePartLength = n / part;
    return middlePartLength == 0 ? 0 : total / middlePartLength;
  }

  public static long averageAmongMedians(@Nonnull int[] time, int part) {
    assert part >= 1;
    int n = time.length;
    Arrays.sort(time);
    long total = 0;
    for (int i = n / 2 - n / part / 2; i < n / 2 + n / part / 2; i++) {
      total += time[i];
    }
    int middlePartLength = n / part;
    return middlePartLength == 0 ? 0 : total / middlePartLength;
  }

  public static int min(int[] values) {
    int min = Integer.MAX_VALUE;
    for (int value : values) {
      if (value < min) min = value;
    }
    return min;
  }

  @Nonnull
  public static <T> Class<T> getComponentType(@Nonnull T[] collection) {
    //noinspection unchecked
    return (Class<T>)collection.getClass().getComponentType();
  }
}
