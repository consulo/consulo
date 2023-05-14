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
import consulo.util.lang.Pair;
import consulo.util.lang.function.Condition;
import consulo.util.lang.function.Conditions;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * Based on IDEA code
 */
public class ContainerUtil {
  private static final int INSERTION_SORT_THRESHOLD = 10;

  @Nonnull
  @Contract(pure = true)
  public static <T, V> Set<V> map2SetNotNull(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, V> mapper) {
    if (collection.isEmpty()) return Collections.emptySet();
    Set<V> set = new HashSet<V>(collection.size());
    for (T t : collection) {
      V value = mapper.apply(t);
      if (value != null) {
        set.add(value);
      }
    }
    return set.isEmpty() ? Collections.<V>emptySet() : set;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, KEY, VALUE> Map<KEY, VALUE> map2Map(@Nonnull T[] collection, @Nonnull Function<T, Pair<KEY, VALUE>> mapper) {
    return map2Map(Arrays.asList(collection), mapper);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, KEY, VALUE> Map<KEY, VALUE> map2Map(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, Pair<KEY, VALUE>> mapper) {
    final Map<KEY, VALUE> set = new HashMap<KEY, VALUE>(collection.size());
    for (T t : collection) {
      Pair<KEY, VALUE> pair = mapper.apply(t);
      set.put(pair.first, pair.second);
    }
    return set;
  }

  @Contract(pure = true)
  public static <T> boolean intersects(@Nonnull Collection<? extends T> collection1, @Nonnull Collection<? extends T> collection2) {
    if (collection1.size() <= collection2.size()) {
      for (T t : collection1) {
        if (collection2.contains(t)) {
          return true;
        }
      }
    }
    else {
      for (T t : collection2) {
        if (collection1.contains(t)) {
          return true;
        }
      }
    }
    return false;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> subList(@Nonnull List<T> list, int from) {
    return list.subList(from, list.size());
  }

  /**
   * @return read-only set consisting of the elements from collection converted by mapper
   */

  public static <T, V> Set<V> map2Set(T[] collection, Function<T, V> mapper) {
    return map2Set(Arrays.asList(collection), mapper);
  }

  /**
   * @return read-only set consisting of the elements from collection converted by mapper
   */

  public static <T, V> Set<V> map2Set(Collection<? extends T> collection, Function<T, V> mapper) {
    if (collection.isEmpty()) return Collections.emptySet();
    Set<V> set = new HashSet<V>(collection.size());
    for (final T t : collection) {
      set.add(mapper.apply(t));
    }
    return set;
  }

  @Contract(pure = true)
  public static <T> boolean and(@Nonnull T[] iterable, @Nonnull Predicate<? super T> condition) {
    return and(Arrays.asList(iterable), condition);
  }

  @Contract(pure = true)
  public static <T> boolean and(@Nonnull Iterable<? extends T> iterable, @Nonnull Predicate<? super T> condition) {
    for (final T t : iterable) {
      if (!condition.test(t)) return false;
    }
    return true;
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> union(@Nonnull Map<? extends K, ? extends V> map, @Nonnull Map<? extends K, ? extends V> map2) {
    Map<K, V> result = new HashMap<>(map.size() + map2.size());
    result.putAll(map);
    result.putAll(map2);
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> union(@Nonnull Set<T> set, @Nonnull Set<T> set2) {
    Set<T> result = new HashSet<>(set.size() + set2.size());
    result.addAll(set);
    result.addAll(set2);
    return result;
  }

  @Nonnull
  public static <T> List<T> collect(@Nonnull Iterator<T> iterator) {
    if (!iterator.hasNext()) return List.of();
    List<T> list = new ArrayList<>();
    addAll(list, iterator);
    return list;
  }

  @Contract(pure = true)
  public static <T, U extends T> U findInstance(@Nonnull Iterable<? extends T> iterable, @Nonnull Class<? extends U> aClass) {
    return findInstance(iterable.iterator(), aClass);
  }

  @Nullable
  @Contract(pure = true)
  public static <T, U extends T> U findInstance(@Nonnull T[] array, @Nonnull Class<U> aClass) {
    return findInstance(Arrays.asList(array), aClass);
  }

  public static <T, U extends T> U findInstance(@Nonnull Iterator<? extends T> iterator, @Nonnull Class<? extends U> aClass) {
    //noinspection unchecked
    return (U)find(iterator, FilteringIterator.instanceOf(aClass));
  }

  @Nullable
  @Contract(pure = true)
  public static <T, L extends List<T>> T getLastItem(@Nullable L list, @Nullable T def) {
    return isEmpty(list) ? def : list.get(list.size() - 1);
  }

  @Nullable
  @Contract(pure = true)
  public static <T, L extends List<T>> T getLastItem(@Nullable L list) {
    return getLastItem(list, null);
  }

  public static <E> void swapElements(@Nonnull List<E> list, int index1, int index2) {
    E e1 = list.get(index1);
    E e2 = list.get(index2);
    list.set(index1, e2);
    list.set(index2, e1);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T extends Comparable<? super T>> List<T> sorted(@Nonnull Collection<? extends T> list) {
    return sorted(list, Comparator.naturalOrder());
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> sorted(@Nonnull Collection<? extends T> list, @Nonnull Comparator<? super T> comparator) {
    return sorted((Iterable<? extends T>)list, comparator);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> sorted(@Nonnull Iterable<? extends T> list, @Nonnull Comparator<? super T> comparator) {
    List<T> sorted = newArrayList(list);
    sort(sorted, comparator);
    return sorted;
  }
  
  public static <T> void sort(@Nonnull T[] a, @Nonnull Comparator<T> comparator) {
    int size = a.length;

    if (size < 2) return;
    if (size == 2) {
      T t0 = a[0];
      T t1 = a[1];

      if (comparator.compare(t0, t1) > 0) {
        a[0] = t1;
        a[1] = t0;
      }
    }
    else if (size < INSERTION_SORT_THRESHOLD) {
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < i; j++) {
          T ti = a[i];
          T tj = a[j];

          if (comparator.compare(ti, tj) < 0) {
            a[i] = tj;
            a[j] = ti;
          }
        }
      }
    }
    else {
      Arrays.sort(a, comparator);
    }
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> createMaybeSingletonList(@Nullable T element) {
    return element == null ? List.of() : Collections.singletonList(element);
  }

  @Nonnull
  @Contract(pure = true)
  public static <E> List<E> reverse(@Nonnull final List<E> elements) {
    if (elements.isEmpty()) {
      return List.of();
    }

    return new AbstractList<>() {
      @Override
      public E get(int index) {
        return elements.get(elements.size() - 1 - index);
      }

      @Override
      public int size() {
        return elements.size();
      }
    };
  }

  @Contract(pure = true)
  public static <T> boolean exists(@Nonnull T[] iterable, @Nonnull Predicate<? super T> condition) {
    return or(Arrays.asList(iterable), condition);
  }

  @Contract(pure = true)
  public static <T> boolean exists(@Nonnull Iterable<? extends T> iterable, @Nonnull Predicate<? super T> condition) {
    return or(iterable, condition);
  }

  @Contract(pure = true)
  public static <T> boolean or(@Nonnull T[] iterable, @Nonnull Predicate<? super T> condition) {
    return or(Arrays.asList(iterable), condition);
  }

  @Contract(pure = true)
  public static <T> boolean or(@Nonnull Iterable<? extends T> iterable, @Nonnull Predicate<? super T> condition) {
    for (final T t : iterable) {
      if (condition.test(t)) return true;
    }
    return false;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Object[] map2Array(@Nonnull T[] array, @Nonnull Function<T, Object> mapper) {
    return map2Array(array, Object.class, mapper);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Object[] map2Array(@Nonnull Collection<T> array, @Nonnull Function<T, Object> mapper) {
    return map2Array(array, Object.class, mapper);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> V[] map2Array(@Nonnull T[] array, @Nonnull Class<? super V> aClass, @Nonnull Function<T, V> mapper) {
    return map2Array(Arrays.asList(array), aClass, mapper);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> V[] map2Array(@Nonnull Collection<? extends T> collection, @Nonnull Class<? super V> aClass, @Nonnull Function<T, V> mapper) {
    final List<V> list = map2List(collection, mapper);
    @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(aClass, list.size());
    return list.toArray(array);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> V[] map2Array(@Nonnull Collection<? extends T> collection, @Nonnull V[] to, @Nonnull Function<T, V> mapper) {
    return map2List(collection, mapper).toArray(to);
  }

  /**
   * @return read-only list consisting of the elements from array converted by mapper
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> map2List(@Nonnull T[] array, @Nonnull Function<T, V> mapper) {
    return map2List(Arrays.asList(array), mapper);
  }

  /**
   * @return read-only list consisting of the elements from collection converted by mapper
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> map2List(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, V> mapper) {
    if (collection.isEmpty()) return List.of();
    List<V> list = new ArrayList<>(collection.size());
    for (final T t : collection) {
      list.add(mapper.apply(t));
    }
    return list;
  }

  public static <T extends Comparable<T>> void sort(@Nonnull List<T> list) {
    int size = list.size();

    if (size < 2) return;
    if (size == 2) {
      T t0 = list.get(0);
      T t1 = list.get(1);

      if (t0.compareTo(t1) > 0) {
        list.set(0, t1);
        list.set(1, t0);
      }
    }
    else if (size < INSERTION_SORT_THRESHOLD) {
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < i; j++) {
          T ti = list.get(i);
          T tj = list.get(j);

          if (ti.compareTo(tj) < 0) {
            list.set(i, tj);
            list.set(j, ti);
          }
        }
      }
    }
    else {
      Collections.sort(list);
    }
  }

  public static <T> void sort(@Nonnull List<T> list, @Nonnull Comparator<? super T> comparator) {
    int size = list.size();

    if (size < 2) return;
    if (size == 2) {
      T t0 = list.get(0);
      T t1 = list.get(1);

      if (comparator.compare(t0, t1) > 0) {
        list.set(0, t1);
        list.set(1, t0);
      }
    }
    else if (size < INSERTION_SORT_THRESHOLD) {
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < i; j++) {
          T ti = list.get(i);
          T tj = list.get(j);

          if (comparator.compare(ti, tj) < 0) {
            list.set(i, tj);
            list.set(j, ti);
          }
        }
      }
    }
    else {
      Collections.sort(list, comparator);
    }
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> filter(@Nonnull Collection<? extends T> collection, @Nonnull Predicate<? super T> condition) {
    return findAll(collection, condition);
  }

  @Nonnull
  @Contract(pure = true)
  public static <K, V> Map<K, V> filter(@Nonnull Map<K, ? extends V> map, @Nonnull Predicate<? super K> keyFilter) {
    Map<K, V> result = new HashMap<>();
    for (Map.Entry<K, ? extends V> entry : map.entrySet()) {
      if (keyFilter.test(entry.getKey())) {
        result.put(entry.getKey(), entry.getValue());
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
    List<V> result = new ArrayList<>();
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

    List<V> result = new ArrayList<>(arr.length);
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
    List<V> result = new ArrayList<>(iterable.size());
    for (T t : iterable) {
      result.add(mapping.apply(t));
    }
    return result;
  }

  /**
   * @return read-only list consisting of the elements from the array converted by mapping
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> map(@Nonnull T[] array, @Nonnull Function<T, V> mapping) {
    List<V> result = new ArrayList<>(array.length);
    for (T t : array) {
      result.add(mapping.apply(t));
    }
    return result.isEmpty() ? List.of() : result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> Set<T> newConcurrentSet() {
    return ConcurrentHashMap.newKeySet();
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> skipNulls(@Nonnull Collection<? extends T> collection) {
    return findAll(collection, Objects::nonNull);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> filter(@Nonnull T[] collection, @Nonnull Predicate<? super T> condition) {
    return findAll(collection, condition);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> findAll(@Nonnull T[] collection, @Nonnull Predicate<? super T> condition) {
    final List<T> result = new SmartList<>();
    for (T t : collection) {
      if (condition.test(t)) {
        result.add(t);
      }
    }
    return result;
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> findAll(@Nonnull T[] collection, @Nonnull Class<V> instanceOf) {
    return findAll(Arrays.asList(collection), instanceOf);
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> findAll(@Nonnull Collection<? extends T> collection, @Nonnull Class<V> instanceOf) {
    final List<V> result = new SmartList<V>();
    for (final T t : collection) {
      if (instanceOf.isInstance(t)) {
        @SuppressWarnings("unchecked") V v = (V)t;
        result.add(v);
      }
    }
    return result;
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
  @Deprecated
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

  @Nonnull
  @Contract(pure = true)
  @Deprecated
  public static <K> Set<K> newIdentityTroveSet(@Nonnull Collection<K> collection) {
    return Sets.newHashSet(collection, HashingStrategy.<K>identity());
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

  @Nonnull
  public static <K, V> Map<K, V> newMapFromValues(@Nonnull Iterator<V> values, @Nonnull Function<V, K> keyConvertor) {
    Map<K, V> map = new HashMap<K, V>();
    fillMapWithValues(map, values, keyConvertor);
    return map;
  }

  public static <K, V> void fillMapWithValues(@Nonnull Map<K, V> map, @Nonnull Iterator<V> values, @Nonnull Function<V, K> keyConvertor) {
    while (values.hasNext()) {
      V value = values.next();
      map.put(keyConvertor.apply(value), value);
    }
  }

  @Nonnull
  public static <K, V> Map<K, Set<V>> classify(@Nonnull Iterator<V> iterator, @Nonnull Function<V, K> keyConvertor) {
    Map<K, Set<V>> hashMap = new LinkedHashMap<K, Set<V>>();
    while (iterator.hasNext()) {
      V value = iterator.next();
      final K key = keyConvertor.apply(value);
      Set<V> set = hashMap.get(key);
      if (set == null) {
        hashMap.put(key, set = new LinkedHashSet<V>()); // ordered set!!
      }
      set.add(value);
    }
    return hashMap;
  }

  @Contract(pure = true)
  public static <T> int indexOf(@Nonnull List<? extends T> list, @Nonnull Predicate<? super T> condition) {
    for (int i = 0, listSize = list.size(); i < listSize; i++) {
      T t = list.get(i);
      if (condition.test(t)) {
        return i;
      }
    }
    return -1;
  }

  @Contract(pure = true)
  public static <T> int lastIndexOf(@Nonnull List<T> list, @Nonnull Predicate<? super T> condition) {
    for (int i = list.size() - 1; i >= 0; i--) {
      T t = list.get(i);
      if (condition.test(t)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * @return read-only list consisting of the elements from all of the collections
   */
  @Nonnull
  @Contract(pure = true)
  public static <E> List<E> flatten(@Nonnull Iterable<? extends Collection<? extends E>> collections) {
    int totalSize = 0;
    for (Collection<? extends E> list : collections) {
      totalSize += list.size();
    }
    List<E> result = new ArrayList<>(totalSize);
    for (Collection<? extends E> list : collections) {
      result.addAll(list);
    }

    return result.isEmpty() ? List.of() : result;
  }

  @Nonnull
  public static <K, V> MultiMap<K, V> groupBy(@Nonnull Iterable<V> collection, @Nonnull Function<V, K> grouper) {
    MultiMap<K, V> result = MultiMap.createLinked();
    for (V data : collection) {
      K key = grouper.apply(data);
      if (key == null) {
        continue;
      }
      result.putValue(key, data);
    }

    if (!result.isEmpty() && result.keySet().iterator().next() instanceof Comparable) {
      return new KeyOrderedMultiMap<K, V>(result);
    }
    return result;
  }

  // returns true if the collection was modified
  public static <T> boolean retainAll(@Nonnull Collection<T> collection, @Nonnull Condition<? super T> condition) {
    boolean modified = false;

    for (Iterator<T> iterator = collection.iterator(); iterator.hasNext(); ) {
      T next = iterator.next();
      if (!condition.value(next)) {
        iterator.remove();
        modified = true;
      }
    }

    return modified;
  }

  private static class KeyOrderedMultiMap<K, V> extends MultiMap<K, V> {

    public KeyOrderedMultiMap() {
    }

    public KeyOrderedMultiMap(@Nonnull MultiMap<? extends K, ? extends V> toCopy) {
      super(toCopy);
    }

    @Nonnull
    @Override
    protected Map<K, Collection<V>> createMap() {
      return new TreeMap<K, Collection<V>>();
    }

    @Nonnull
    @Override
    protected Map<K, Collection<V>> createMap(int initialCapacity, float loadFactor) {
      return new TreeMap<K, Collection<V>>();
    }

    @Nonnull
    public NavigableSet<K> navigableKeySet() {
      //noinspection unchecked
      return ((TreeMap)myMap).navigableKeySet();
    }
  }

  @Nullable
  @Contract(pure = true)
  public static <T> T iterateAndGetLastItem(@Nonnull Iterable<T> items) {
    Iterator<T> itr = items.iterator();
    T res = null;
    while (itr.hasNext()) {
      res = itr.next();
    }

    return res;
  }

  @Nonnull
  public static <T> T[] copyAndClear(@Nonnull Collection<T> collection, @Nonnull ArrayFactory<T> factory, boolean clear) {
    int size = collection.size();
    T[] a = factory.create(size);
    if (size > 0) {
      a = collection.toArray(a);
      if (clear) collection.clear();
    }
    return a;
  }


  /**
   * Returns the only item from the collection or null if collection is empty or contains more than one item
   *
   * @param items collection to get the item from
   * @param <T>   type of collection element
   * @return the only collection element or null
   */
  @Contract(pure = true)
  public static <T> T getOnlyItem(@Nullable final Collection<? extends T> items) {
    return getOnlyItem(items, null);
  }

  @Contract(pure = true)
  public static <T> T getOnlyItem(@Nullable final Collection<? extends T> items, @Nullable final T defaultResult) {
    return items == null || items.size() != 1 ? defaultResult : items.iterator().next();
  }

  /**
   * @return read-only collection consisting of elements from both collections
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> Collection<T> intersection(@Nonnull Collection<? extends T> collection1, @Nonnull Collection<? extends T> collection2) {
    List<T> result = new ArrayList<T>();
    for (T t : collection1) {
      if (collection2.contains(t)) {
        result.add(t);
      }
    }
    return result.isEmpty() ? List.of() : result;
  }

  public static <T> boolean all(@Nonnull T[] collection, @Nonnull java.util.function.Predicate<? super T> condition) {
    for (T t : collection) {
      if (!condition.test(t)) {
        return false;
      }
    }
    return true;
  }

  public static <T> boolean all(@Nonnull Collection<? extends T> collection, @Nonnull java.util.function.Predicate<? super T> condition) {
    for (T t : collection) {
      if (!condition.test(t)) {
        return false;
      }
    }
    return true;
  }

  public static <K, V> boolean any(Map<K, V> map, java.util.function.Predicate<Map.Entry<K, V>> predicate) {
    if (map.isEmpty()) return false;

    for (Map.Entry<K, V> element : map.entrySet()) {
      if (predicate.test(element)) {
        return true;
      }
    }
    return false;
  }

  /**
   * prepend values in front of the list
   *
   * @return read-only list consisting of values and the elements from specified list
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> List<T> prepend(@Nonnull List<? extends T> list, @Nonnull T... values) {
    return concat(list(values), list);
  }

  @Contract(pure = true)
  @Nonnull
  public static <T> List<T> filterIsInstance(@Nonnull Collection<?> collection, final @Nonnull Class<? extends T> aClass) {
    //noinspection unchecked
    return filter((Collection<T>)collection, Conditions.instanceOf(aClass));
  }

  @Contract(pure = true)
  @Nonnull
  public static <T> List<T> filterIsInstance(@Nonnull final Object[] collection, final @Nonnull Class<? extends T> aClass) {
    //noinspection unchecked
    return (List<T>)filter(collection, Conditions.instanceOf(aClass));
  }

  public static <T> void removeDuplicates(@Nonnull Collection<T> collection) {
    Set<T> collected = new HashSet<T>();
    for (Iterator<T> iterator = collection.iterator(); iterator.hasNext(); ) {
      T t = iterator.next();
      if (!collected.contains(t)) {
        collected.add(t);
      }
      else {
        iterator.remove();
      }
    }
  }

  @Nonnull
  @Contract(pure = true)
  public static <T, E> Iterable<Pair<T, E>> zip(@Nonnull final Iterable<T> iterable1, @Nonnull final Iterable<E> iterable2) {
    return new Iterable<Pair<T, E>>() {
      @Override
      public Iterator<Pair<T, E>> iterator() {
        return new Iterator<Pair<T, E>>() {
          private final Iterator<T> i1 = iterable1.iterator();
          private final Iterator<E> i2 = iterable2.iterator();

          @Override
          public boolean hasNext() {
            return i1.hasNext() && i2.hasNext();
          }

          @Override
          public Pair<T, E> next() {
            return Pair.create(i1.next(), i2.next());
          }

          @Override
          public void remove() {
            i1.remove();
            i2.remove();
          }
        };
      }
    };
  }

  /**
   * @return read-only list consisting of the elements from the iterable converted by mapping with nulls filtered out
   */
  @Nonnull
  @Contract(pure = true)
  public static <T, V> List<V> mapNotNull(@Nonnull Iterable<? extends T> iterable, @Nonnull Function<T, V> mapping) {
    List<V> result = new ArrayList<V>();
    for (T t : iterable) {
      final V o = mapping.apply(t);
      if (o != null) {
        result.add(o);
      }
    }
    return result.isEmpty() ? List.of() : result;
  }
}
