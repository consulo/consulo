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

import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.Pair;
import consulo.util.nodep.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.LinkedHashSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stripped-down version of {@code com.intellij.util.containers.ContainerUtil}.
 * Intended to use by external (out-of-IDE-process) runners and helpers so it should not contain any library dependencies.
 *
 * @since 12.0
 */
@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public class ContainerUtilRt {
  private static final int ARRAY_COPY_THRESHOLD = 20;

  @Nonnull
  public static <K, V> Map<K, V> newHashMap() {
    return new HashMap<K, V>();
  }

  @Nonnull
  public static <K, V> Map<K, V> newHashMap(@Nonnull Map<? extends K, ? extends V> map) {
    return new HashMap<K, V>(map);
  }

  @Nonnull
  public static <K, V> Map<K, V> newHashMap(@Nonnull List<K> keys, @Nonnull List<V> values) {
    if (keys.size() != values.size()) {
      throw new IllegalArgumentException(keys + " should have same length as " + values);
    }

    Map<K, V> map = newHashMap(keys.size());
    for (int i = 0; i < keys.size(); ++i) {
      map.put(keys.get(i), values.get(i));
    }
    return map;
  }

  @Nonnull
  public static <K, V> Map<K, V> newHashMap(@Nonnull Pair<K, ? extends V> first, @Nonnull Pair<K, ? extends V>... entries) {
    Map<K, V> map = newHashMap(entries.length + 1);
    map.put(first.getFirst(), first.getSecond());
    for (Pair<K, ? extends V> entry : entries) {
      map.put(entry.getFirst(), entry.getSecond());
    }
    return map;
  }

  @Nonnull
  public static <K, V> Map<K, V> newHashMap(int initialCapacity) {
    return new HashMap<K, V>(initialCapacity);
  }

  @Nonnull
  public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
    return new TreeMap<K, V>();
  }

  @Nonnull
  public static <K extends Comparable, V> TreeMap<K, V> newTreeMap(@Nonnull Map<K, V> map) {
    return new TreeMap<K, V>(map);
  }

  @Nonnull
  public static <K, V> Map<K, V> newLinkedHashMap() {
    return new LinkedHashMap<K, V>();
  }

  @Nonnull
  public static <K, V> Map<K, V> newLinkedHashMap(int capacity) {
    return new LinkedHashMap<K, V>(capacity);
  }

  @Nonnull
  public static <K, V> Map<K, V> newLinkedHashMap(@Nonnull Map<K, V> map) {
    return new LinkedHashMap<K, V>(map);
  }

  @Nonnull
  public static <K, V> Map<K,V> newLinkedHashMap(@Nonnull Pair<K, V> first, @Nonnull Pair<K, V>[] entries) {
    Map<K, V> map = newLinkedHashMap();
    map.put(first.getFirst(), first.getSecond());
    for (Pair<K, V> entry : entries) {
      map.put(entry.getFirst(), entry.getSecond());
    }
    return map;
  }

  @Nonnull
  public static <T> LinkedList<T> newLinkedList() {
    return new LinkedList<T>();
  }

  @Nonnull
  public static <T> LinkedList<T> newLinkedList(@Nonnull T... elements) {
    final LinkedList<T> list = newLinkedList();
    Collections.addAll(list, elements);
    return list;
  }

  @Nonnull
  public static <T> LinkedList<T> newLinkedList(@Nonnull Iterable<? extends T> elements) {
    return copy(ContainerUtilRt.<T>newLinkedList(), elements);
  }

  @Nonnull
  public static <T> ArrayList<T> newArrayList() {
    return new ArrayList<T>();
  }

  @Nonnull
  public static <T> ArrayList<T> newArrayList(@Nonnull T... elements) {
    ArrayList<T> list = newArrayListWithCapacity(elements.length);
    Collections.addAll(list, elements);
    return list;
  }

  @Nonnull
  public static <T> ArrayList<T> newArrayList(@Nonnull Iterable<? extends T> elements) {
    if (elements instanceof Collection) {
      @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
      return new ArrayList<T>(collection);
    }
    return copy(ContainerUtilRt.<T>newArrayList(), elements);
  }

  @Nonnull
  public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
    return new ArrayList<T>(size);
  }

  @Nonnull
  private static <T, C extends Collection<T>> C copy(@Nonnull C collection, @Nonnull Iterable<? extends T> elements) {
    for (T element : elements) {
      collection.add(element);
    }
    return collection;
  }

  @Nonnull
  public static <T> HashSet<T> newHashSet() {
    return new HashSet<T>();
  }

  @Nonnull
  public static <T> HashSet<T> newHashSet(int initialCapacity) {
    return new HashSet<T>(initialCapacity);
  }

  @Nonnull
  public static <T> HashSet<T> newHashSet(@Nonnull T... elements) {
    return new HashSet<T>(Arrays.asList(elements));
  }

  @Nonnull
  public static <T> HashSet<T> newHashSet(@Nonnull Iterable<? extends T> elements) {
    if (elements instanceof Collection) {
      @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
      return new HashSet<T>(collection);
    }
    return newHashSet(elements.iterator());
  }

  @Nonnull
  public static <T> HashSet<T> newHashSet(@Nonnull Iterator<? extends T> iterator) {
    HashSet<T> set = newHashSet();
    while (iterator.hasNext()) set.add(iterator.next());
    return set;
  }

  @Nonnull
  public static <T> Set<T> newLinkedHashSet() {
    return new LinkedHashSet<T>();
  }

  @Nonnull
  public static <T> Set<T> newLinkedHashSet(@Nonnull T... elements) {
    return newLinkedHashSet(Arrays.asList(elements));
  }

  @Nonnull
  public static <T> Set<T> newLinkedHashSet(@Nonnull Iterable<? extends T> elements) {
    if (elements instanceof Collection) {
      @SuppressWarnings("unchecked") Collection<? extends T> collection = (Collection<? extends T>)elements;
      return new LinkedHashSet<T>(collection);
    }
    return copy(ContainerUtilRt.<T>newLinkedHashSet(), elements);
  }

  @Nonnull
  public static <T> TreeSet<T> newTreeSet() {
    return new TreeSet<T>();
  }

  @Nonnull
  public static <T> TreeSet<T> newTreeSet(@Nonnull T... elements) {
    TreeSet<T> set = newTreeSet();
    Collections.addAll(set, elements);
    return set;
  }

  @Nonnull
  public static <T> TreeSet<T> newTreeSet(@Nonnull Iterable<? extends T> elements) {
    return copy(ContainerUtilRt.<T>newTreeSet(), elements);
  }

  @Nonnull
  public static <T> TreeSet<T> newTreeSet(@Nullable Comparator<? super T> comparator) {
    return new TreeSet<T>(comparator);
  }

  /**
   * A variant of {@link java.util.Collections#emptyList()},
   * except that {@link #toArray()} here does not create garbage <code>new Object[0]</code> constantly.
   */
  private static class EmptyList<T> extends AbstractList<T> implements RandomAccess, Serializable {
    private static final long serialVersionUID = 1L;

    private static final EmptyList INSTANCE = new EmptyList();

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean contains(Object obj) {
      return false;
    }

    @Override
    public T get(int index) {
      throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Nonnull
    @Override
    public Object[] toArray() {
      return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    }

    @Nonnull
    @Override
    public <E> E[] toArray(@Nonnull E[] a) {
      if (a.length != 0) {
        a[0] = null;
      }
      return a;
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
      return EmptyIterator.getInstance();
    }
  }

  @Nonnull
  public static <T> List<T> emptyList() {
    //noinspection unchecked
    return (List<T>)EmptyList.INSTANCE;
  }

  @Nonnull
  public static <T> CopyOnWriteArrayList<T> createEmptyCOWList() {
    // does not create garbage new Object[0]
    return new CopyOnWriteArrayList<T>(ContainerUtilRt.<T>emptyList());
  }

  /**
   * @see #addIfNotNull(Collection, Object)
   */
  @Deprecated
  public static <T> void addIfNotNull(@Nullable T element, @Nonnull Collection<T> result) {
    if (element != null) {
      result.add(element);
    }
  }

  public static <T> void addIfNotNull(@Nonnull Collection<T> result, @Nullable T element) {
    if (element != null) {
      result.add(element);
    }
  }

  /**
   * @return read-only list consisting of the elements from array converted by mapper
   */
  @Nonnull
  public static <T, V> List<V> map2List(@Nonnull T[] array, @Nonnull Function<T, V> mapper) {
    return map2List(Arrays.asList(array), mapper);
  }

  /**
   * @return read-only list consisting of the elements from collection converted by mapper
   */
  @Nonnull
  public static <T, V> List<V> map2List(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, V> mapper) {
    if (collection.isEmpty()) return emptyList();
    List<V> list = new ArrayList<V>(collection.size());
    for (final T t : collection) {
      list.add(mapper.fun(t));
    }
    return list;
  }

  /**
   * @return read-only set consisting of the elements from collection converted by mapper
   */
  @Nonnull
  public static <T, V> Set<V> map2Set(@Nonnull T[] collection, @Nonnull Function<T, V> mapper) {
    return map2Set(Arrays.asList(collection), mapper);
  }

  /**
   * @return read-only set consisting of the elements from collection converted by mapper
   */
  @Nonnull
  public static <T, V> Set<V> map2Set(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, V> mapper) {
    if (collection.isEmpty()) return Collections.emptySet();
    Set <V> set = new HashSet<V>(collection.size());
    for (final T t : collection) {
      set.add(mapper.fun(t));
    }
    return set;
  }

  @Nonnull
  public static <T> T[] toArray(@Nonnull List<T> collection, @Nonnull T[] array) {
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
  @Nonnull
  public static <T> T[] toArray(@Nonnull Collection<T> c, @Nonnull T[] sample) {
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
