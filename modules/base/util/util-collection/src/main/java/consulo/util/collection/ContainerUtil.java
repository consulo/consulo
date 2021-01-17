/*
 * Copyright 2013-2019 consulo.io
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

import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Based on IDEA code
 */
public class ContainerUtil {
  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> newConcurrentSet() {
    return Collections.newSetFromMap(newConcurrentMap());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
    return new ConcurrentHashMap<>();
  }

  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity) {
    return new ConcurrentHashMap<>(initialCapacity);
  }

  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
    return new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakValueMap() {
    return new ConcurrentWeakValueHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentSoftValueMap() {
    return new ConcurrentSoftValueHashMap<>();
  }

  /**
   * Adds all not-null elements from the {@code elements}, ignoring nulls
   */
  public static <T> void addAllNotNull(@Nonnull Collection<T> collection, @Nonnull Iterable<? extends T> elements) {
    addAllNotNull(collection, elements.iterator());
  }

  /**
   * Adds all not-null elements from the {@code elements}, ignoring nulls
   */
  @SafeVarargs
  @Nonnull
  public static <T, A extends T, C extends Collection<T>> C addAllNotNull(@Nonnull C collection, @Nonnull A... elements) {
    for (T element : elements) {
      if (element != null) {
        collection.add(element);
      }
    }
    return collection;
  }

  /**
   * Adds all not-null elements from the {@code elements}, ignoring nulls
   */
  public static <T> void addAllNotNull(@Nonnull Collection<T> collection, @Nonnull Iterator<? extends T> elements) {
    while (elements.hasNext()) {
      T o = elements.next();
      if (o != null) {
        collection.add(o);
      }
    }
  }

  /**
   * Soft keys hard values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> createSoftMap() {
    return new SoftHashMap<>(4);
  }

  @Contract(value = "_ -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> createSoftMap(@Nonnull TObjectHashingStrategy<? super K> strategy) {
    return new SoftHashMap<K, V>(strategy);
  }

  @Nonnull
  @Contract(value = " -> new", pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentSoftKeySoftValueMap() {
    return createConcurrentSoftKeySoftValueMap(100, 0.75f, Runtime.getRuntime().availableProcessors(), canonicalStrategy());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentSoftKeySoftValueMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final TObjectHashingStrategy<K> hashingStrategy) {
    return new ConcurrentSoftKeySoftValueHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <T> TObjectHashingStrategy<T> canonicalStrategy() {
    return ObjectHashingStrategies.canonicalStrategy();
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <T> TObjectHashingStrategy<T> identityStrategy() {
    return ObjectHashingStrategies.identityStrategy();
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeySoftValueMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final TObjectHashingStrategy<K> hashingStrategy) {
    return Maps.newConcurrentWeakKeySoftValueHashMap(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  /**
   * Hard keys soft values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> createSoftValueMap() {
    return new SoftValueHashMap<>(canonicalStrategy());
  }

  /**
   * Weak keys hard values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  @Deprecated
  public static <K, V> Map<K, V> createWeakMap() {
    return Maps.newWeakHashMap(4);
  }

  @Contract(value = "_ -> new", pure = true)
  @Nonnull
  @Deprecated
  public static <K, V> Map<K, V> createWeakMap(int initialCapacity) {
    return Maps.newWeakHashMap(initialCapacity, 0.8f, canonicalStrategy());
  }

  @Contract(value = "_, _, _ -> new", pure = true)
  @Nonnull
  @Deprecated
  public static <K, V> Map<K, V> createWeakMap(int initialCapacity, float loadFactor, @Nonnull TObjectHashingStrategy<? super K> strategy) {
    return Maps.newWeakHashMap(initialCapacity, loadFactor, strategy);
  }

  @Contract(value = " -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> createWeakKeyWeakValueMap() {
    return new WeakKeyWeakValueHashMap<>(true);
  }

  public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Iterable<? extends T> appendix) {
    addAll(collection, appendix.iterator());
  }

  public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Iterator<? extends T> iterator) {
    while (iterator.hasNext()) {
      T o = iterator.next();
      collection.add(o);
    }
  }

  public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Enumeration<? extends T> enumeration) {
    while (enumeration.hasMoreElements()) {
      T element = enumeration.nextElement();
      collection.add(element);
    }
  }

  @Nonnull
  public static <T, A extends T, C extends Collection<T>> C addAll(@Nonnull C collection, @Nonnull A... elements) {
    //noinspection ManualArrayToCollectionCopy
    for (T element : elements) {
      collection.add(element);
    }
    return collection;
  }

  @Nullable
  @Contract(pure = true)
  public static <T, V extends T> V find(@Nonnull Iterable<V> iterable, @Nonnull Predicate<T> condition) {
    return find(iterable.iterator(), condition);
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T find(@Nonnull Iterable<? extends T> iterable, @Nonnull final T equalTo) {
    return find(iterable, object -> equalTo == object || equalTo.equals(object));
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T find(@Nonnull Iterator<? extends T> iterator, @Nonnull final T equalTo) {
    return find(iterator, object -> equalTo == object || equalTo.equals(object));
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentLongObjectMap<V> createConcurrentLongObjectMap() {
    //noinspection deprecation
    return new ConcurrentLongObjectHashMap<V>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentLongObjectMap<V> createConcurrentLongObjectMap(int initialCapacity) {
    //noinspection deprecation
    return new ConcurrentLongObjectHashMap<V>(initialCapacity);
  }

  @Nonnull
  public static <K, V> Map<K, V> createWeakKeySoftValueMap() {
    return new WeakKeySoftValueHashMap<K, V>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeyWeakValueMap() {
    return createConcurrentWeakKeyWeakValueMap(ContainerUtil.<K>canonicalStrategy());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeyWeakValueMap(@Nonnull TObjectHashingStrategy<K> strategy) {
    //noinspection deprecation
    return new ConcurrentWeakKeyWeakValueHashMap<K, V>(100, 0.75f, Runtime.getRuntime().availableProcessors(), strategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectWeakValueMap() {
    return Maps.newConcurrentIntObjectWeakValueHashMap();
  }

  @Nullable
  public static <T, V extends T> V find(@Nonnull Iterator<V> iterator, @Nonnull Predicate<T> condition) {
    while (iterator.hasNext()) {
      V value = iterator.next();
      if (condition.test(value)) return value;
    }
    return null;
  }


  @Nonnull
  @Contract(pure = true)
  public static <T> ArrayList<T> newArrayList() {
    return new ArrayList<T>();
  }

  @Nonnull
  @Contract(pure = true)
  @SafeVarargs
  public static <T> ArrayList<T> newArrayList(@Nonnull T... elements) {
    ArrayList<T> list = newArrayListWithCapacity(elements.length);
    Collections.addAll(list, elements);
    return list;
  }

  @Nonnull
  @Contract(pure = true)
  public static <K> Set<K> newIdentityTroveSet() {
    return new THashSet<>(ObjectHashingStrategies.<K>identityStrategy());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K> Set<K> newIdentityTroveSet(int initialCapacity) {
    return new THashSet<>(initialCapacity, ObjectHashingStrategies.<K>identityStrategy());
  }

  public static <T> void weightSort(List<T> list, final Function<T, Integer> weighterFunc) {
    Collections.sort(list, (o1, o2) -> weighterFunc.apply(o2) - weighterFunc.apply(o1));
  }

  @Nonnull
  private static <T, C extends Collection<T>> C copy(@Nonnull C collection, @Nonnull Iterable<? extends T> elements) {
    for (T element : elements) {
      collection.add(element);
    }
    return collection;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> ArrayList<T> newArrayList(@Nonnull Iterable<? extends T> elements) {
    if (elements instanceof Collection) {
      @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
      return new ArrayList<T>(collection);
    }
    return copy(ContainerUtil.<T>newArrayList(), elements);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
    return new ArrayList<T>(size);
  }

  /**
   * Hard keys weak values hash map.
   * Null keys are NOT allowed
   * Null values are allowed
   */
  @Contract(value = " -> new", pure = true)
  @Nonnull
  @Deprecated
  public static <K, V> Map<K, V> createWeakValueMap() {
    return Maps.newWeakValueHashMap();
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <K, V> ConcurrentMap<K, V> createConcurrentSoftMap() {
    return Maps.newConcurrentSoftHashMap();
  }

  @Nonnull
  @Contract(value = " -> new", pure = true)
  @Deprecated
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap() {
    return Maps.newConcurrentWeakHashMap();
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <K, V> ConcurrentMap<K, V> createConcurrentSoftMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull TObjectHashingStrategy<K> hashingStrategy) {
    return Maps.newConcurrentSoftHashMap(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull TObjectHashingStrategy<K> hashingStrategy) {
    return Maps.newConcurrentWeakHashMap(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap(@Nonnull TObjectHashingStrategy<K> hashingStrategy) {
    return Maps.newConcurrentWeakHashMap(hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectMap() {
    return Maps.newConcurrentIntObjectHashMap();
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
    return Maps.newConcurrentIntObjectHashMap(initialCapacity, loadFactor, concurrencyLevel);
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectSoftValueMap() {
    return Maps.newConcurrentIntObjectSoftValueHashMap();
  }

  public static <T> void addIfNotNull(@Nonnull Collection<T> result, @Nullable T element) {
    if (element != null) {
      result.add(element);
    }
  }

  @Contract(value = "null -> true", pure = true)
  public static <T> boolean isEmpty(Collection<T> collection) {
    return collection == null || collection.isEmpty();
  }

  @Contract(value = "null -> true", pure = true)
  public static boolean isEmpty(Map map) {
    return map == null || map.isEmpty();
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> newArrayList(@Nonnull final T[] elements, final int start, final int end) {
    if (start < 0 || start > end || end > elements.length) {
      throw new IllegalArgumentException("start:" + start + " end:" + end + " length:" + elements.length);
    }

    return new AbstractList<T>() {
      private final int size = end - start;

      @Override
      public T get(final int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index:" + index + " size:" + size);
        return elements[start + index];
      }

      @Override
      public int size() {
        return size;
      }
    };
  }

  /**
   * @return iterator with elements from the original {@param iterator} which are valid according to {@param filter} predicate.
   */
  @Contract(pure = true)
  @Nonnull
  public static <T> Iterator<T> filterIterator(final @Nonnull Iterator<? extends T> iterator, final @Nonnull Predicate<? super T> filter) {
    return new Iterator<T>() {
      T next;
      boolean hasNext;

      {
        findNext();
      }

      @Override
      public boolean hasNext() {
        return hasNext;
      }

      private void findNext() {
        hasNext = false;
        while (iterator.hasNext()) {
          T t = iterator.next();
          if (filter.test(t)) {
            next = t;
            hasNext = true;
            break;
          }
        }
      }

      @Override
      public T next() {
        T result;
        if (hasNext) {
          result = next;
          findNext();
        }
        else {
          throw new NoSuchElementException();
        }
        return result;
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }

  @Contract(pure = true)
  @Nonnull
  public static <T, U> Iterator<U> mapIterator(final @Nonnull Iterator<? extends T> iterator, final @Nonnull Function<? super T, ? extends U> mapper) {
    return new Iterator<U>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public U next() {
        return mapper.apply(iterator.next());
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }
}
