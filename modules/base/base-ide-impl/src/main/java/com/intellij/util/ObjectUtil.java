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

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.Convertor;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Proxy;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class ObjectUtil {
  /**
   * @see NotNullizer
   */
  public static final Object NULL = sentinel("ObjectUtils.NULL");

  private ObjectUtil() {
  }

  @Nonnull
  public static <T> T assertNotNull(@Nullable T t) {
    return notNull(t);
  }

  public static <T> void assertAllElementsNotNull(@Nonnull T[] array) {
    for (int i = 0; i < array.length; i++) {
      T t = array[i];
      if (t == null) {
        throw new NullPointerException("Element [" + i + "] is null");
      }
    }
  }

  @Contract("null, null -> null")
  public static <T> T chooseNotNull(@Nullable T t1, @Nullable T t2) {
    return t1 == null? t2 : t1;
  }

  @Contract("null,null->null")
  public static <T> T coalesce(@Nullable T t1, @Nullable T t2) {
    return t1 != null ? t1 : t2;
  }

  @Contract("null,null,null->null")
  public static <T> T coalesce(@Nullable T t1, @Nullable T t2, @Nullable T t3) {
    return t1 != null ? t1 : t2 != null ? t2 : t3;
  }

  @Nullable
  public static <T> T coalesce(@Nullable Iterable<T> o) {
    if (o == null) return null;
    for (T t : o) {
      if (t != null) return t;
    }
    return null;
  }

  @Nonnull
  public static <T> T notNull(@Nullable T value) {
    //noinspection ConstantConditions
    return notNull(value, value);
  }

  @Nonnull
  public static <T> T notNull(@Nullable T value, @Nonnull T defaultValue) {
    return value == null ? defaultValue : value;
  }

  @Nonnull
  public static <T> T notNull(@Nullable T value, @Nonnull Supplier<? extends T> defaultValue) {
    return value == null ? defaultValue.get() : value;
  }

  @Nullable
  public static <T> T tryCast(@Nullable Object obj, @Nonnull Class<T> clazz) {
    if (clazz.isInstance(obj)) {
      return clazz.cast(obj);
    }
    return null;
  }

  @Nullable
  public static <T, S> S doIfCast(@Nullable Object obj, @Nonnull Class<T> clazz, final Convertor<T, S> convertor) {
    if (clazz.isInstance(obj)) {
      //noinspection unchecked
      return convertor.convert((T)obj);
    }
    return null;
  }

  @Nullable
  public static <T> T nullizeByCondition(@Nullable final T obj, @Nonnull final Condition<T> condition) {
    if (condition.value(obj)) {
      return null;
    }
    return obj;
  }

  /**
   * Performs binary search on the range [fromIndex, toIndex)
   *
   * @param indexComparator a comparator which receives a middle index and returns the result of comparision of the value at this index and the goal value
   *                        (e.g 0 if found, -1 if the value[middleIndex] < goal, or 1 if value[middleIndex] > goal)
   * @return index for which {@code indexComparator} returned 0 or {@code -insertionIndex-1} if wasn't found
   * @see java.util.Arrays#binarySearch(Object[], Object, Comparator)
   * @see java.util.Collections#binarySearch(List, Object, Comparator)
   */
  public static int binarySearch(int fromIndex, int toIndex, @Nonnull IntUnaryOperator indexComparator) {
    int low = fromIndex;
    int high = toIndex - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      int cmp = indexComparator.applyAsInt(mid);
      if (cmp < 0) low = mid + 1;
      else if (cmp > 0) high = mid - 1;
      else return mid;
    }
    return -(low + 1);
  }

  /**
   * Creates a new object which could be used as sentinel value (special value to distinguish from any other object). It does not equal
   * to any other object. Usually should be assigned to the static final field.
   *
   * @param name an object name, returned from {@link #toString()} to simplify the debugging or heap dump analysis
   *             (guaranteed to be stored as sentinel object field). If sentinel is assigned to the static final field,
   *             it's recommended to supply that field name (possibly qualified with the class name).
   * @return a new sentinel object
   */
  @Nonnull
  public static Object sentinel(@Nonnull String name) {
    return new Sentinel(name);
  }

  public static void reachabilityFence(Object o) {
    consulo.util.lang.ObjectUtil.reachabilityFence(o);
  }

  private static class Sentinel {
    private final String myName;

    Sentinel(@Nonnull String name) {
      myName = name;
    }

    @Override
    public String toString() {
      return myName;
    }
  }

  /**
   * Creates an instance of class {@code ofInterface} with its {@link Object#toString()} method returning {@code name}.
   * No other guarantees about return value behaviour.
   * {@code ofInterface} must represent an interface class.
   * Useful for stubs in generic code, e.g. for storing in {@code List<T>} to represent empty special value.
   */
  @Nonnull
  public static <T> T sentinel(@Nonnull final String name, @Nonnull Class<T> ofInterface) {
    if (!ofInterface.isInterface()) {
      throw new IllegalArgumentException("Expected interface but got: " + ofInterface);
    }
    // java.lang.reflect.Proxy.ProxyClassFactory fails if the class is not available via the classloader.
    // We must use interface own classloader because classes from plugins are not available via ObjectUtils' classloader.
    //noinspection unchecked
    return (T)Proxy.newProxyInstance(ofInterface.getClassLoader(), new Class[]{ofInterface}, (__, method, args) -> {
      if ("toString".equals(method.getName()) && args.length == 0) {
        return name;
      }
      throw new AbstractMethodError();
    });
  }
}
