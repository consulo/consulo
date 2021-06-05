// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.lang;

import consulo.annotation.ReviewAfterMigrationToJRE;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.ref.Reference;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class ObjectUtil {
  private ObjectUtil() {
  }

  /**
   * @see NotNullizer
   */
  public static final Object NULL = sentinel("ObjectUtils.NULL");

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

  /**
   * They promise in http://mail.openjdk.java.net/pipermail/core-libs-dev/2018-February/051312.html that
   * the object reference won't be removed by JIT and GC-ed until this call.
   */
  @ReviewAfterMigrationToJRE(9)
  public static void reachabilityFence(Object o) {
    Reference.reachabilityFence(o);
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

  @Contract(value = "!null, _ -> !null; _, !null -> !null; null, null -> null", pure = true)
  public static <T> T chooseNotNull(@Nullable T t1, @Nullable T t2) {
    return t1 == null ? t2 : t1;
  }

  @Contract(value = "!null, _ -> !null; _, !null -> !null; null, null -> null", pure = true)
  public static <T> T coalesce(@Nullable T t1, @Nullable T t2) {
    return chooseNotNull(t1, t2);
  }

  @Contract(value = "!null, _, _ -> !null; _, !null, _ -> !null; _, _, !null -> !null; null,null,null -> null", pure = true)
  public static <T> T coalesce(@Nullable T t1, @Nullable T t2, @Nullable T t3) {
    return t1 != null ? t1 : t2 != null ? t2 : t3;
  }

  @Nullable
  public static <T> T coalesce(@Nullable Iterable<? extends T> o) {
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
  @Contract(pure = true)
  public static <T> T notNull(@Nullable T value, @Nonnull T defaultValue) {
    return value == null ? defaultValue : value;
  }

  @Nonnull
  public static <T> T notNull(@Nullable T value, @Nonnull Supplier<? extends T> defaultValue) {
    return value == null ? defaultValue.get() : value;
  }

  @Contract(value = "null, _ -> null", pure = true)
  @Nullable
  public static <T> T tryCast(@Nullable Object obj, @Nonnull Class<T> clazz) {
    if (clazz.isInstance(obj)) {
      return clazz.cast(obj);
    }
    return null;
  }

  @Nullable
  public static <T, S> S doIfCast(@Nullable Object obj, @Nonnull Class<T> clazz, final Function<? super T, ? extends S> convertor) {
    if (clazz.isInstance(obj)) {
      //noinspection unchecked
      return convertor.apply((T)obj);
    }
    return null;
  }

  @Contract("null, _ -> null")
  @Nullable
  public static <T, S> S doIfNotNull(@Nullable T obj, @Nonnull Function<? super T, ? extends S> function) {
    return obj == null ? null : function.apply(obj);
  }

  public static <T> void consumeIfNotNull(@Nullable T obj, @Nonnull Consumer<? super T> consumer) {
    if (obj != null) {
      consumer.accept(obj);
    }
  }

  public static <T> void consumeIfCast(@Nullable Object obj, @Nonnull Class<T> clazz, final Consumer<? super T> consumer) {
    if (clazz.isInstance(obj)) {
      //noinspection unchecked
      consumer.accept((T)obj);
    }
  }

  @Nullable
  @Contract("null, _ -> null")
  public static <T> T nullizeByCondition(@Nullable final T obj, @Nonnull final Predicate<? super T> condition) {
    if (condition.test(obj)) {
      return null;
    }
    return obj;
  }

  @Nullable
  @Contract("null, _ -> null")
  public static <T> T nullizeIfDefaultValue(@Nullable T obj, @Nonnull T defaultValue) {
    if (obj == defaultValue) {
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
  //public static int binarySearch(int fromIndex, int toIndex, @NotNull IntIntFunction indexComparator) {
  //  int low = fromIndex;
  //  int high = toIndex - 1;
  //  while (low <= high) {
  //    int mid = (low + high) >>> 1;
  //    int cmp = indexComparator.fun(mid);
  //    if (cmp < 0) low = mid + 1;
  //    else if (cmp > 0) high = mid - 1;
  //    else return mid;
  //  }
  //  return -(low + 1);
  //}
}
