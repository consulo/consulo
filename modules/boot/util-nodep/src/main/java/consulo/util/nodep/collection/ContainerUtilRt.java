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
package consulo.util.nodep.collection;

import consulo.util.nodep.Pair;
import consulo.util.nodep.function.Function;

import java.util.*;

/**
 * Stripped-down version of {@code com.intellij.util.containers.ContainerUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers so it should not contain any library dependencies.
 *
 * @since 12.0
 */
@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public class ContainerUtilRt {
  private static final int ARRAY_COPY_THRESHOLD = 20;


  public static <K, V> Map<K, V> newHashMap() {
    return new HashMap<K, V>();
  }


  public static <K, V> Map<K, V> newHashMap(Map<? extends K, ? extends V> map) {
    return new HashMap<K, V>(map);
  }


  public static <K, V> Map<K, V> newHashMap(List<K> keys, List<V> values) {
    if (keys.size() != values.size()) {
      throw new IllegalArgumentException(keys + " should have same length as " + values);
    }

    Map<K, V> map = newHashMap(keys.size());
    for (int i = 0; i < keys.size(); ++i) {
      map.put(keys.get(i), values.get(i));
    }
    return map;
  }


  public static <K, V> Map<K, V> newHashMap(Pair<K, ? extends V> first, Pair<K, ? extends V>... entries) {
    Map<K, V> map = newHashMap(entries.length + 1);
    map.put(first.getFirst(), first.getSecond());
    for (Pair<K, ? extends V> entry : entries) {
      map.put(entry.getFirst(), entry.getSecond());
    }
    return map;
  }


  public static <K, V> Map<K, V> newHashMap(int initialCapacity) {
    return new HashMap<K, V>(initialCapacity);
  }


  public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
    return new TreeMap<K, V>();
  }


  public static <K extends Comparable, V> TreeMap<K, V> newTreeMap(Map<K, V> map) {
    return new TreeMap<K, V>(map);
  }


  public static <K, V> Map<K, V> newLinkedHashMap() {
    return new LinkedHashMap<K, V>();
  }


  public static <K, V> Map<K, V> newLinkedHashMap(int capacity) {
    return new LinkedHashMap<K, V>(capacity);
  }


  public static <K, V> Map<K, V> newLinkedHashMap(Map<K, V> map) {
    return new LinkedHashMap<K, V>(map);
  }


  public static <K, V> Map<K, V> newLinkedHashMap(Pair<K, V> first, Pair<K, V>[] entries) {
    Map<K, V> map = newLinkedHashMap();
    map.put(first.getFirst(), first.getSecond());
    for (Pair<K, V> entry : entries) {
      map.put(entry.getFirst(), entry.getSecond());
    }
    return map;
  }


  public static <T> LinkedList<T> newLinkedList() {
    return new LinkedList<T>();
  }


  public static <T> LinkedList<T> newLinkedList(T... elements) {
    final LinkedList<T> list = newLinkedList();
    Collections.addAll(list, elements);
    return list;
  }


  public static <T> LinkedList<T> newLinkedList(Iterable<? extends T> elements) {
    return copy(ContainerUtilRt.<T>newLinkedList(), elements);
  }


  public static <T> ArrayList<T> newArrayList() {
    return new ArrayList<T>();
  }


  public static <T> ArrayList<T> newArrayList(T... elements) {
    ArrayList<T> list = newArrayListWithCapacity(elements.length);
    Collections.addAll(list, elements);
    return list;
  }


  public static <T> ArrayList<T> newArrayList(Iterable<? extends T> elements) {
    if (elements instanceof Collection) {
      @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
      return new ArrayList<T>(collection);
    }
    return copy(ContainerUtilRt.<T>newArrayList(), elements);
  }


  public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
    return new ArrayList<T>(size);
  }


  private static <T, C extends Collection<T>> C copy(C collection, Iterable<? extends T> elements) {
    for (T element : elements) {
      collection.add(element);
    }
    return collection;
  }


  public static <T> HashSet<T> newHashSet() {
    return new HashSet<T>();
  }


  public static <T> HashSet<T> newHashSet(int initialCapacity) {
    return new HashSet<T>(initialCapacity);
  }


  public static <T> HashSet<T> newHashSet(T... elements) {
    return new HashSet<T>(Arrays.asList(elements));
  }


  public static <T> HashSet<T> newHashSet(Iterable<? extends T> elements) {
    if (elements instanceof Collection) {
      @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
      return new HashSet<T>(collection);
    }
    return newHashSet(elements.iterator());
  }


  public static <T> HashSet<T> newHashSet(Iterator<? extends T> iterator) {
    HashSet<T> set = newHashSet();
    while (iterator.hasNext()) set.add(iterator.next());
    return set;
  }


  public static <T> Set<T> newLinkedHashSet() {
    return new LinkedHashSet<T>();
  }


  public static <T> Set<T> newLinkedHashSet(T... elements) {
    return newLinkedHashSet(Arrays.asList(elements));
  }


  public static <T> Set<T> newLinkedHashSet(Iterable<? extends T> elements) {
    if (elements instanceof Collection) {
      @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
      return new LinkedHashSet<T>(collection);
    }
    return copy(ContainerUtilRt.<T>newLinkedHashSet(), elements);
  }


  public static <T> TreeSet<T> newTreeSet() {
    return new TreeSet<T>();
  }


  public static <T> TreeSet<T> newTreeSet(T... elements) {
    TreeSet<T> set = newTreeSet();
    Collections.addAll(set, elements);
    return set;
  }


  public static <T> TreeSet<T> newTreeSet(Iterable<? extends T> elements) {
    return copy(ContainerUtilRt.<T>newTreeSet(), elements);
  }


  public static <T> TreeSet<T> newTreeSet(Comparator<? super T> comparator) {
    return new TreeSet<T>(comparator);
  }

  /**
   * @return read-only list consisting of the elements from array converted by mapper
   */

  public static <T, V> List<V> map2List(T[] array, Function<T, V> mapper) {
    return map2List(Arrays.asList(array), mapper);
  }

  /**
   * @return read-only list consisting of the elements from collection converted by mapper
   */

  public static <T, V> List<V> map2List(Collection<? extends T> collection, Function<T, V> mapper) {
    if (collection.isEmpty()) return Collections.emptyList();
    List<V> list = new ArrayList<V>(collection.size());
    for (final T t : collection) {
      list.add(mapper.fun(t));
    }
    return list;
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
      set.add(mapper.fun(t));
    }
    return set;
  }


  public static <T> T[] toArray(List<T> collection, T[] array) {
    final int length = array.length;
    if (length < ARRAY_COPY_THRESHOLD && array.length >= collection.size()) {
      for (int i = 0; i < collection.size(); i++) {
        array[i] = collection.get(i);
      }
      return array;
    }
    return collection.toArray(array);
  }

  /**
   * This is a replacement for {@link Collection#toArray(Object[])}. For small collections it is faster to stay at java level and refrain
   * from calling JNI {@link System#arraycopy(Object, int, Object, int, int)}
   */

  public static <T> T[] toArray(Collection<T> c, T[] sample) {
    final int size = c.size();
    if (size == sample.length && size < ARRAY_COPY_THRESHOLD) {
      int i = 0;
      for (T t : c) {
        sample[i++] = t;
      }
      return sample;
    }

    return c.toArray(sample);
  }
}
