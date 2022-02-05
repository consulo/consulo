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

import consulo.util.collection.impl.map.*;
import consulo.util.lang.function.Condition;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * Based on IDEA code
 */
public class ContainerUtil {
  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> findAll(@Nonnull T[] collection, @Nonnull Condition<? super T> condition) {
    final List<T> result = new SmartList<T>();
    for (T t : collection) {
      if (condition.value(t)) {
        result.add(t);
      }
    }
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> filter(@Nonnull Collection<? extends T> collection, @Nonnull Condition<? super T> condition) {
    return findAll(collection, condition);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> filter(@Nonnull Map<K, ? extends V> map, @Nonnull Condition<? super K> keyFilter) {
    Map<K, V> result = new HashMap<K, V>();
    for (Map.Entry<K, ? extends V> entry : map.entrySet()) {
      if (keyFilter.value(entry.getKey())) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> findAll(@Nonnull Collection<? extends T> collection, @Nonnull Condition<? super T> condition) {
    if (collection.isEmpty()) return List.of();
    final List<T> result = new SmartList<T>();
    for (final T t : collection) {
      if (condition.value(t)) {
        result.add(t);
      }
    }
    return result;
  }


  /**
   * @return read-only list consisting of the elements from the iterable converted by mapping
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> map(@Nonnull Iterable<? extends T> iterable, @Nonnull Function<T, V> mapping) {
    List<V> result = new ArrayList<V>();
    for (T t : iterable) {
      result.add(mapping.apply(t));
    }
    return result.isEmpty() ? List.of() : result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> V[] map(@Nonnull T[] arr, @Nonnull Function<T, V> mapping, @Nonnull V[] emptyArray) {
    if (arr.length == 0) {
      assert emptyArray.length == 0 : "You must pass an empty array";
      return emptyArray;
    }

    List<V> result = new ArrayList<V>(arr.length);
    for (T t : arr) {
      result.add(mapping.apply(t));
    }
    return result.toArray(emptyArray);
  }


  /**
   * @return read-only list consisting of the elements from the iterable converted by mapping
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> map(@Nonnull Collection<? extends T> iterable, @Nonnull Function<T, V> mapping) {
    if (iterable.isEmpty()) return List.of();
    List<V> result = new ArrayList<V>(iterable.size());
    for (T t : iterable) {
      result.add(mapping.apply(t));
    }
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> newConcurrentSet() {
    return Collections.newSetFromMap(newConcurrentMap());
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> skipNulls(@Nonnull Collection<? extends T> collection) {
    return findAll(collection, Objects::nonNull);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> findAll(@Nonnull Collection<? extends T> collection, @Nonnull Predicate<? super T> condition) {
    if (collection.isEmpty()) return List.of();
    final List<T> result = new ArrayList<>();
    for (final T t : collection) {
      if (condition.test(t)) {
        result.add(t);
      }
    }
    return result;
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
    return Maps.newSoftHashMap();
  }

  @Contract(value = "_ -> new", pure = true)
  @Nonnull
  public static <K, V> Map<K, V> createSoftMap(@Nonnull HashingStrategy<? super K> strategy) {
    return Maps.newSoftHashMap(strategy);
  }

  @Nonnull
  @Contract(value = " -> new", pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentSoftKeySoftValueMap() {
    return createConcurrentSoftKeySoftValueMap(100, 0.75f, Runtime.getRuntime().availableProcessors(), canonicalStrategy());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentSoftKeySoftValueMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final HashingStrategy<K> hashingStrategy) {
    return Maps.newConcurrentSoftKeySoftValueHashMap(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <T> HashingStrategy<T> canonicalStrategy() {
    return HashingStrategy.canonical();
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <T> HashingStrategy<T> identityStrategy() {
    return HashingStrategy.identity();
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeySoftValueMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull final HashingStrategy<K> hashingStrategy) {
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
  public static <K, V> Map<K, V> createWeakMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<? super K> strategy) {
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
  public static <T> T find(@Nonnull T[] array, @Nonnull Predicate<? super T> condition) {
    for (T element : array) {
      if (condition.test(element)) return element;
    }
    return null;
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
  public static <K, V> Map<K, V> createWeakKeySoftValueMap() {
    return new WeakKeySoftValueHashMap<>();
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeyWeakValueMap() {
    return createConcurrentWeakKeyWeakValueMap(ContainerUtil.<K>canonicalStrategy());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeyWeakValueMap(@Nonnull HashingStrategy<K> strategy) {
    return Maps.newConcurrentWeakKeyWeakValueHashMap(strategy);
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
    return new ArrayList<>();
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
    return Sets.newHashSet(HashingStrategy.identity());
  }

  @Nonnull
  @Contract(pure = true)
  public static <K> Set<K> newIdentityTroveSet(int initialCapacity) {
    return Sets.newHashSet(initialCapacity, HashingStrategy.<K>identity());
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
      return new ArrayList<>(collection);
    }
    return copy(ContainerUtil.<T>newArrayList(), elements);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
    return new ArrayList<>(size);
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
  public static <K, V> ConcurrentMap<K, V> createConcurrentSoftMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull HashingStrategy<K> hashingStrategy) {
    return Maps.newConcurrentSoftHashMap(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap(int initialCapacity, float loadFactor, int concurrencyLevel, @Nonnull HashingStrategy<K> hashingStrategy) {
    return Maps.newConcurrentWeakHashMap(initialCapacity, loadFactor, concurrencyLevel, hashingStrategy);
  }

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap(@Nonnull HashingStrategy<K> hashingStrategy) {
    return Maps.newConcurrentWeakHashMap(hashingStrategy);
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

    return new AbstractList<>() {
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
    return new Iterator<>() {
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
    return new Iterator<>() {
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

  @Nonnull
  @Contract(pure = true)
  public static <T> Iterable<T> concat(@Nonnull final Iterable<? extends T>... iterables) {
    return new Iterable<>() {
      @Nonnull
      @Override
      public Iterator<T> iterator() {
        Iterator[] iterators = new Iterator[iterables.length];
        for (int i = 0; i < iterables.length; i++) {
          Iterable<? extends T> iterable = iterables[i];
          iterators[i] = iterable.iterator();
        }
        @SuppressWarnings("unchecked") Iterator<T> i = concatIterators(iterators);
        return i;
      }
    };
  }


  /**
   * @return read-only list consisting of the lists added together
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> concat(@Nonnull final List<? extends T>... lists) {
    int size = 0;
    for (List<? extends T> each : lists) {
      size += each.size();
    }
    if (size == 0) return List.of();
    final int finalSize = size;
    return new AbstractList<>() {
      @Override
      public T get(final int index) {
        if (index >= 0 && index < finalSize) {
          int from = 0;
          for (List<? extends T> each : lists) {
            if (from <= index && index < from + each.size()) {
              return each.get(index - from);
            }
            from += each.size();
          }
          if (from != finalSize) {
            throw new ConcurrentModificationException("The list has changed. Its size was " + finalSize + "; now it's " + from);
          }
        }
        throw new IndexOutOfBoundsException("index: " + index + "size: " + size());
      }

      @Override
      public int size() {
        return finalSize;
      }
    };
  }

  /**
   * @return read-only list consisting of the two lists added together
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> concat(@Nonnull final List<? extends T> list1, @Nonnull final List<? extends T> list2) {
    if (list1.isEmpty() && list2.isEmpty()) {
      return Collections.emptyList();
    }
    if (list1.isEmpty()) {
      //noinspection unchecked
      return (List<T>)list2;
    }
    if (list2.isEmpty()) {
      //noinspection unchecked
      return (List<T>)list1;
    }

    final int size1 = list1.size();
    final int size = size1 + list2.size();

    return new AbstractList<>() {
      @Override
      public T get(int index) {
        if (index < size1) {
          return list1.get(index);
        }

        return list2.get(index - size1);
      }

      @Override
      public int size() {
        return size;
      }
    };
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Iterator<T> concatIterators(@Nonnull Iterator<T>... iterators) {
    return new SequenceIterator<>(iterators);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Iterator<T> concatIterators(@Nonnull Collection<Iterator<T>> iterators) {
    return new SequenceIterator<>(iterators);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<T> concat(@Nonnull V[] array, @Nonnull Function<V, Collection<? extends T>> fun) {
    return concat(Arrays.asList(array), fun);
  }

  /**
   * @return read-only list consisting of the elements from the collections stored in list added together
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> concat(@Nonnull Iterable<? extends Collection<T>> list) {
    List<T> result = new ArrayList<>();
    for (final Collection<T> ts : list) {
      result.addAll(ts);
    }
    return result.isEmpty() ? Collections.<T>emptyList() : result;
  }

  /**
   * @param appendTail specify whether additional values should be appended in front or after the list
   * @return read-only list consisting of the elements from specified list with some additional values
   * @deprecated Use {@link #append(List, Object[])} or {@link #prepend(List, Object[])} instead
   */
  @Deprecated
  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> concat(boolean appendTail, @Nonnull List<? extends T> list, @Nonnull T... values) {
    return appendTail ? concat(list, list(values)) : concat(list(values), list);
  }

  /**
   * @return read-only list consisting of the lists added together
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> concat(@Nonnull final List<List<? extends T>> lists) {
    @SuppressWarnings("unchecked") List<? extends T>[] array = lists.toArray(new List[lists.size()]);
    return concat(array);
  }

  /**
   * @return read-only list consisting of the lists (made by listGenerator) added together
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<T> concat(@Nonnull Iterable<? extends V> list, @Nonnull Function<V, Collection<? extends T>> listGenerator) {
    List<T> result = new ArrayList<>();
    for (final V v : list) {
      result.addAll(listGenerator.apply(v));
    }
    return result.isEmpty() ? List.of() : result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> list(@Nonnull T... items) {
    return Arrays.asList(items);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> T[] toArray(@Nullable Collection<T> c, @Nonnull IntFunction<? extends T[]> factory) {
    return c != null ? c.toArray(factory.apply(c.size())) : factory.apply(0);
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T getFirstItem(@Nullable Collection<T> items) {
    return getFirstItem(items, null);
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T getFirstItem(@Nullable List<T> items) {
    return items == null || items.isEmpty() ? null : items.get(0);
  }

  @Contract(pure = true)
  public static <T> T getFirstItem(@Nullable final Collection<T> items, @Nullable final T defaultResult) {
    return items == null || items.isEmpty() ? defaultResult : items.iterator().next();
  }


  /**
   * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> mapNotNull(@Nonnull T[] array, @Nonnull Function<T, V> mapping) {
    return mapNotNull(Arrays.asList(array), mapping);
  }


  /**
   * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> mapNotNull(@Nonnull Collection<? extends T> iterable, @Nonnull Function<T, V> mapping) {
    if (iterable.isEmpty()) {
      return List.of();
    }

    List<V> result = new ArrayList<>(iterable.size());
    for (T t : iterable) {
      final V o = mapping.apply(t);
      if (o != null) {
        result.add(o);
      }
    }
    return result.isEmpty() ? List.of() : result;
  }

  /**
   * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> V[] mapNotNull(@Nonnull T[] array, @Nonnull Function<T, V> mapping, @Nonnull V[] emptyArray) {
    List<V> result = new ArrayList<>(array.length);
    for (T t : array) {
      V v = mapping.apply(t);
      if (v != null) {
        result.add(v);
      }
    }
    if (result.isEmpty()) {
      assert emptyArray.length == 0 : "You must pass an empty array";
      return emptyArray;
    }
    return result.toArray(emptyArray);
  }

  public static <T> boolean process(@Nonnull Iterable<? extends T> iterable, @Nonnull Predicate<T> processor) {
    for (final T t : iterable) {
      if (!processor.test(t)) {
        return false;
      }
    }
    return true;
  }

  public static <T> boolean process(@Nonnull List<? extends T> list, @Nonnull Predicate<T> processor) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, size = list.size(); i < size; i++) {
      T t = list.get(i);
      if (!processor.test(t)) {
        return false;
      }
    }
    return true;
  }

  public static <T> boolean process(@Nonnull T[] iterable, @Nonnull Predicate<? super T> processor) {
    for (final T t : iterable) {
      if (!processor.test(t)) {
        return false;
      }
    }
    return true;
  }

  public static <T> boolean process(@Nonnull Iterator<T> iterator, @Nonnull Predicate<? super T> processor) {
    while (iterator.hasNext()) {
      if (!processor.test(iterator.next())) {
        return false;
      }
    }
    return true;
  }
}
