/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.containers;

import consulo.annotation.DeprecationInfo;
import consulo.application.util.function.Processor;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.util.collection.Stack;
import consulo.util.collection.*;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.longs.ConcurrentLongObjectMap;
import consulo.util.collection.primitive.longs.LongMaps;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.function.PairProcessor;
import consulo.util.lang.function.Predicates;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntProcedure;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;

@SuppressWarnings("ALL")
@Deprecated(forRemoval = true)
@DeprecationInfo("Use consulo.util.collection.ContainerUtil")
public class ContainerUtil extends ContainerUtilRt {
    private static final int INSERTION_SORT_THRESHOLD = 10;
    private static final int DEFAULT_CONCURRENCY_LEVEL = Math.min(16, Runtime.getRuntime().availableProcessors());

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> T[] ar(@Nonnull T... elements) {
        return elements;
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> HashMap<K, V> newHashMap(@Nonnull Map<? extends K, ? extends V> map) {
        return new HashMap<>(map);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <K, V> Map<K, V> newHashMap(@Nonnull Pair<K, ? extends V> first, @Nonnull Pair<K, ? extends V>... entries) {
        return ContainerUtilRt.newHashMap(first, entries);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> Map<K, V> newHashMap(@Nonnull List<K> keys, @Nonnull List<V> values) {
        return ContainerUtilRt.newHashMap(keys, values);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap(@Nonnull Map<K, V> map) {
        return new TreeMap<>(map);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int capacity) {
        return new LinkedHashMap<>(capacity);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(@Nonnull Map<K, V> map) {
        return new LinkedHashMap<>(map);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(@Nonnull Pair<K, V> first, @Nonnull Pair<K, V>... entries) {
        return ContainerUtilRt.newLinkedHashMap(first, entries);
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K, V> Map<K, V> newTroveMap() {
        return new HashMap<>();
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K, V> Map<K, V> newTroveMap(@Nonnull HashingStrategy<K> strategy) {
        return Maps.newHashMap(strategy);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(@Nonnull Class<K> keyType) {
        return new EnumMap<>(keyType);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <T> HashingStrategy<T> canonicalStrategy() {
        return HashingStrategy.canonical();
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <T> HashingStrategy<T> identityStrategy() {
        return HashingStrategy.identity();
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
        return new IdentityHashMap<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> LinkedList<T> newLinkedList() {
        return new LinkedList<>();
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> LinkedList<T> newLinkedList(@Nonnull T... elements) {
        return ContainerUtilRt.newLinkedList(elements);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> LinkedList<T> newLinkedList(@Nonnull Iterable<? extends T> elements) {
        return ContainerUtilRt.newLinkedList(elements);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> ArrayList<T> newArrayList() {
        return new ArrayList<>();
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <E> ArrayList<E> newArrayList(@Nonnull E... array) {
        return ContainerUtilRt.newArrayList(array);
    }

    @Nonnull
    @Contract(pure = true)
    public static <E> ArrayList<E> newArrayList(@Nonnull Iterable<? extends E> iterable) {
        return ContainerUtilRt.newArrayList(iterable);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
        return new ArrayList<>(size);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> newArrayList(@Nonnull T[] elements, int start, int end) {
        if (start < 0 || start > end || end > elements.length) {
            throw new IllegalArgumentException("start:" + start + " end:" + end + " length:" + elements.length);
        }

        return new AbstractList<>() {
            private final int size = end - start;

            @Override
            public T get(int index) {
                if (index < 0 || index >= size) {
                    throw new IndexOutOfBoundsException("index:" + index + " size:" + size);
                }
                return elements[start + index];
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> newUnmodifiableList(List<? extends T> originalList) {
        int size = originalList.size();
        if (size == 0) {
            return emptyList();
        }
        else if (size == 1) {
            return Collections.singletonList(originalList.get(0));
        }
        else {
            return Collections.unmodifiableList(newArrayList(originalList));
        }
    }


    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> newSmartList() {
        return new SmartList<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> newSmartList(T element) {
        return new SmartList<>(element);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> newSmartList(@Nonnull T... elements) {
        return new SmartList<>(elements);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> HashSet<T> newHashSet() {
        return new HashSet<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> HashSet<T> newHashSet(int initialCapacity) {
        return new HashSet<>(initialCapacity);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> HashSet<T> newHashSet(@Nonnull T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> HashSet<T> newHashSet(@Nonnull Iterable<? extends T> iterable) {
        return ContainerUtilRt.newHashSet(iterable);
    }

    @Nonnull
    public static <T> HashSet<T> newHashSet(@Nonnull Iterator<? extends T> iterator) {
        return ContainerUtilRt.newHashSet(iterator);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Set<T> newHashOrEmptySet(@Nullable Iterable<? extends T> iterable) {
        boolean empty = iterable == null || iterable instanceof Collection collection && collection.isEmpty();
        return empty ? Collections.<T>emptySet() : ContainerUtilRt.newHashSet(iterable);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> LinkedHashSet<T> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> LinkedHashSet<T> newLinkedHashSet(@Nonnull Iterable<? extends T> elements) {
        return ContainerUtilRt.newLinkedHashSet(elements);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> LinkedHashSet<T> newLinkedHashSet(@Nonnull T... elements) {
        return ContainerUtilRt.newLinkedHashSet(elements);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> LinkedHashSet<T> newLinkedHashSet(@Nonnull Iterable<? extends T> elements, @Nonnull T element) {
        LinkedHashSet<T> set = ContainerUtilRt.newLinkedHashSet(elements);
        set.add(element);
        return set;
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newTroveSet() {
        return new HashSet<>();
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newTroveSet(@Nonnull HashingStrategy<T> strategy) {
        return Sets.newHashSet(strategy);
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    @SafeVarargs
    public static <T> Set<T> newTroveSet(@Nonnull T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    @SafeVarargs
    public static <T> Set<T> newTroveSet(@Nonnull HashingStrategy<T> strategy, @Nonnull T... elements) {
        return Sets.newHashSet(Arrays.asList(elements), strategy);
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newTroveSet(@Nonnull HashingStrategy<T> strategy, @Nonnull Collection<T> elements) {
        return Sets.newHashSet(elements, strategy);
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newTroveSet(@Nonnull Collection<T> elements) {
        return new HashSet<>(elements);
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K> Set<K> newIdentityTroveSet() {
        return Sets.newHashSet(HashingStrategy.identity());
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K> Set<K> newIdentityTroveSet(int initialCapacity) {
        return Sets.newHashSet(initialCapacity, HashingStrategy.identity());
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K> Set<K> newIdentityTroveSet(@Nonnull Collection<K> collection) {
        return Sets.newHashSet(collection, HashingStrategy.identity());
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K, V> Map<K, V> newIdentityTroveMap() {
        return Maps.newHashMap(HashingStrategy.identity());
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> TreeSet<T> newTreeSet() {
        return new TreeSet<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> TreeSet<T> newTreeSet(@Nonnull Iterable<? extends T> elements) {
        return ContainerUtilRt.newTreeSet(elements);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> TreeSet<T> newTreeSet(@Nonnull T... elements) {
        return ContainerUtilRt.newTreeSet(elements);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> TreeSet<T> newTreeSet(@Nullable Comparator<? super T> comparator) {
        return new TreeSet<>(comparator);
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newConcurrentSet() {
        return consulo.util.collection.ContainerUtil.newConcurrentSet();
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    @Contract(pure = true)
    @Deprecated
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }

    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        return new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    }

    @Nonnull
    @Contract(pure = true)
    public static <E> List<E> reverse(@Nonnull List<E> elements) {
        return consulo.util.collection.ContainerUtil.reverse(elements);
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
    @Contract(pure = true)
    @SafeVarargs
    public static <E> Set<E> immutableSet(@Nonnull E... elements) {
        switch (elements.length) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(elements[0]);
            default:
                return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
        }
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <E> ImmutableList<E> immutableList(@Nonnull E... array) {
        return new ImmutableListBackedByArray<>(array);
    }

    @Nonnull
    @Contract(pure = true)
    public static <E> ImmutableList<E> immutableList(@Nonnull List<? extends E> list) {
        return new ImmutableListBackedByList<>(list);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ImmutableMapBuilder<K, V> immutableMapBuilder() {
        return ImmutableMapBuilder.newBuilder();
    }

    @Nonnull
    public static <K, V> MultiMap<K, V> groupBy(@Nonnull Iterable<V> collection, @Nonnull Function<V, K> grouper) {
        return consulo.util.collection.ContainerUtil.groupBy(collection, grouper);
    }

    @Contract(pure = true)
    public static <T> T getOrElse(@Nonnull List<T> elements, int i, T defaultValue) {
        return elements.size() > i ? elements.get(i) : defaultValue;
    }

    private static class ImmutableListBackedByList<E> extends ImmutableList<E> {
        private final List<? extends E> myStore;

        private ImmutableListBackedByList(@Nonnull List<? extends E> list) {
            myStore = list;
        }

        @Override
        public E get(int index) {
            return myStore.get(index);
        }

        @Override
        public int size() {
            return myStore.size();
        }
    }

    private static class ImmutableListBackedByArray<E> extends ImmutableList<E> {
        private final E[] myStore;

        private ImmutableListBackedByArray(@Nonnull E[] array) {
            myStore = array;
        }

        @Override
        public E get(int index) {
            return myStore[index];
        }

        @Override
        public int size() {
            return myStore.length;
        }
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> Map<K, V> intersection(@Nonnull Map<K, V> map1, @Nonnull Map<K, V> map2) {
        Map<K, V> res = newHashMap();
        Set<K> keys = newHashSet();
        keys.addAll(map1.keySet());
        keys.addAll(map2.keySet());
        for (K k : keys) {
            V v1 = map1.get(k);
            V v2 = map2.get(k);
            if (v1 == v2 || v1 != null && v1.equals(v2)) {
                res.put(k, v1);
            }
        }
        return res;
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> Map<K, Couple<V>> diff(@Nonnull Map<K, V> map1, @Nonnull Map<K, V> map2) {
        Map<K, Couple<V>> res = newHashMap();
        Set<K> keys = newHashSet();
        keys.addAll(map1.keySet());
        keys.addAll(map2.keySet());
        for (K k : keys) {
            V v1 = map1.get(k);
            V v2 = map2.get(k);
            if (!(v1 == v2 || v1 != null && v1.equals(v2))) {
                res.put(k, Couple.of(v1, v2));
            }
        }
        return res;
    }

    public static <T> void processSortedListsInOrder(
        @Nonnull List<? extends T> list1,
        @Nonnull List<? extends T> list2,
        @Nonnull Comparator<? super T> comparator,
        boolean mergeEqualItems,
        @Nonnull Consumer<? super T> processor
    ) {
        int index1 = 0;
        int index2 = 0;
        while (index1 < list1.size() || index2 < list2.size()) {
            T e;
            if (index1 >= list1.size()) {
                e = list2.get(index2++);
            }
            else if (index2 >= list2.size()) {
                e = list1.get(index1++);
            }
            else {
                T element1 = list1.get(index1);
                T element2 = list2.get(index2);
                int c = comparator.compare(element1, element2);
                if (c == 0) {
                    index1++;
                    index2++;
                    if (mergeEqualItems) {
                        e = element1;
                    }
                    else {
                        processor.accept(element1);
                        e = element2;
                    }
                }
                else if (c < 0) {
                    e = element1;
                    index1++;
                }
                else {
                    e = element2;
                    index2++;
                }
            }
            processor.accept(e);
        }
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> mergeSortedLists(
        @Nonnull List<? extends T> list1,
        @Nonnull List<? extends T> list2,
        @Nonnull Comparator<? super T> comparator,
        boolean mergeEqualItems
    ) {
        List<T> result = new ArrayList<>(list1.size() + list2.size());
        processSortedListsInOrder(list1, list2, comparator, mergeEqualItems, result::add);
        return result;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> mergeSortedArrays(
        @Nonnull T[] list1,
        @Nonnull T[] list2,
        @Nonnull Comparator<? super T> comparator,
        boolean mergeEqualItems,
        @Nullable Processor<? super T> filter
    ) {
        int index1 = 0;
        int index2 = 0;
        List<T> result = new ArrayList<>(list1.length + list2.length);

        while (index1 < list1.length || index2 < list2.length) {
            if (index1 >= list1.length) {
                T t = list2[index2++];
                if (filter != null && !filter.process(t)) {
                    continue;
                }
                result.add(t);
            }
            else if (index2 >= list2.length) {
                T t = list1[index1++];
                if (filter != null && !filter.process(t)) {
                    continue;
                }
                result.add(t);
            }
            else {
                T element1 = list1[index1];
                if (filter != null && !filter.process(element1)) {
                    index1++;
                    continue;
                }
                T element2 = list2[index2];
                if (filter != null && !filter.process(element2)) {
                    index2++;
                    continue;
                }
                int c = comparator.compare(element1, element2);
                if (c < 0) {
                    result.add(element1);
                    index1++;
                }
                else if (c > 0) {
                    result.add(element2);
                    index2++;
                }
                else {
                    result.add(element1);
                    if (!mergeEqualItems) {
                        result.add(element2);
                    }
                    index1++;
                    index2++;
                }
            }
        }

        return result;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> subList(@Nonnull List<T> list, int from) {
        return list.subList(from, list.size());
    }

    @Deprecated
    public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Iterable<? extends T> appendix) {
        consulo.util.collection.ContainerUtil.addAll(collection, appendix);
    }

    @Deprecated
    public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Iterator<? extends T> iterator) {
        consulo.util.collection.ContainerUtil.addAll(collection, iterator);
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    public static <T> void addAllNotNull(@Nonnull Collection<T> collection, @Nonnull Iterable<? extends T> elements) {
        consulo.util.collection.ContainerUtil.addAllNotNull(collection, elements);
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    public static <T> void addAllNotNull(@Nonnull Collection<T> collection, @Nonnull Iterator<? extends T> elements) {
        consulo.util.collection.ContainerUtil.addAllNotNull(collection, elements);
    }

    @Nonnull
    public static <T> List<T> collect(@Nonnull Iterator<T> iterator) {
        return consulo.util.collection.ContainerUtil.collect(iterator);
    }

    @Nonnull
    public static <T> Set<T> collectSet(@Nonnull Iterator<T> iterator) {
        if (!iterator.hasNext()) {
            return Collections.emptySet();
        }
        Set<T> hashSet = newHashSet();
        addAll(hashSet, iterator);
        return hashSet;
    }

    @Nonnull
    public static <K, V> Map<K, V> newMapFromKeys(@Nonnull Iterator<K> keys, @Nonnull Convertor<K, V> valueConvertor) {
        Map<K, V> map = newHashMap();
        while (keys.hasNext()) {
            K key = keys.next();
            map.put(key, valueConvertor.convert(key));
        }
        return map;
    }

    @Nonnull
    public static <K, V> Map<K, V> newMapFromValues(@Nonnull Iterator<V> values, @Nonnull Function<V, K> keyConvertor) {
        return consulo.util.collection.ContainerUtil.newMapFromValues(values, keyConvertor);
    }

    public static <K, V> void fillMapWithValues(
        @Nonnull Map<K, V> map,
        @Nonnull Iterator<V> values,
        @Nonnull Function<V, K> keyConvertor
    ) {
        consulo.util.collection.ContainerUtil.fillMapWithValues(map, values, keyConvertor);
    }

    @Nonnull
    public static <K, V> Map<K, Set<V>> classify(@Nonnull Iterator<V> iterator, @Nonnull Function<V, K> keyConvertor) {
        return consulo.util.collection.ContainerUtil.classify(iterator, keyConvertor);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Iterator<T> emptyIterator() {
        return EmptyIterator.getInstance();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Iterable<T> emptyIterable() {
        return EmptyIterable.getInstance();
    }

    @Nullable
    @Contract(pure = true)
    public static <T> T find(@Nonnull T[] array, @Nonnull Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.find(array, condition);
    }

    public static <T> boolean process(@Nonnull Iterable<? extends T> iterable, @Nonnull Predicate<T> processor) {
        return consulo.util.collection.ContainerUtil.process(iterable, processor);
    }

    public static <T> boolean process(@Nonnull List<? extends T> list, @Nonnull Predicate<T> processor) {
        return consulo.util.collection.ContainerUtil.process(list, processor);
    }

    public static <T> boolean process(@Nonnull T[] iterable, @Nonnull Predicate<? super T> processor) {
        return consulo.util.collection.ContainerUtil.process(iterable, processor);
    }

    public static <T> boolean process(@Nonnull Iterator<T> iterator, @Nonnull Predicate<? super T> processor) {
        return consulo.util.collection.ContainerUtil.process(iterator, processor);
    }

    @Nullable
    @Contract(pure = true)
    public static <T, V extends T> V find(@Nonnull Iterable<V> iterable, @Nonnull Predicate<T> condition) {
        return consulo.util.collection.ContainerUtil.find(iterable, condition);
    }

    @Nullable
    @Contract(pure = true)
    public static <T> T find(@Nonnull Iterable<? extends T> iterable, @Nonnull T equalTo) {
        return consulo.util.collection.ContainerUtil.find(iterable, equalTo);
    }

    @Nullable
    @Contract(pure = true)
    public static <T> T find(@Nonnull Iterator<? extends T> iterator, @Nonnull T equalTo) {
        return consulo.util.collection.ContainerUtil.find(iterator, equalTo);
    }

    @Nullable
    public static <T, V extends T> V find(@Nonnull Iterator<V> iterator, @Nonnull Predicate<T> condition) {
        return consulo.util.collection.ContainerUtil.find(iterator, condition);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2Map(@Nonnull T[] collection, @Nonnull Function<T, Pair<KEY, VALUE>> mapper) {
        return consulo.util.collection.ContainerUtil.map2Map(collection, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2Map(
        @Nonnull Collection<? extends T> collection,
        @Nonnull Function<T, Pair<KEY, VALUE>> mapper
    ) {
        return consulo.util.collection.ContainerUtil.map2Map(collection, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2MapNotNull(@Nonnull T[] collection, @Nonnull Function<T, Pair<KEY, VALUE>> mapper) {
        return map2MapNotNull(Arrays.asList(collection), mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2MapNotNull(
        @Nonnull Collection<? extends T> collection,
        @Nonnull Function<T, Pair<KEY, VALUE>> mapper
    ) {
        Map<KEY, VALUE> set = new HashMap<>(collection.size());
        for (T t : collection) {
            Pair<KEY, VALUE> pair = mapper.apply(t);
            if (pair != null) {
                set.put(pair.first, pair.second);
            }
        }
        return set;
    }

    @Nonnull
    @Contract(pure = true)
    public static <KEY, VALUE> Map<KEY, VALUE> map2Map(@Nonnull Collection<Pair<KEY, VALUE>> collection) {
        Map<KEY, VALUE> result = new HashMap<>(collection.size());
        for (Pair<KEY, VALUE> pair : collection) {
            result.put(pair.first, pair.second);
        }
        return result;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Object[] map2Array(@Nonnull T[] array, @Nonnull Function<T, Object> mapper) {
        return consulo.util.collection.ContainerUtil.map2Array(array, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Object[] map2Array(@Nonnull Collection<T> array, @Nonnull Function<T, Object> mapper) {
        return consulo.util.collection.ContainerUtil.map2Array(array, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> V[] map2Array(@Nonnull T[] array, @Nonnull Class<? super V> aClass, @Nonnull Function<T, V> mapper) {
        return consulo.util.collection.ContainerUtil.map2Array(array, aClass, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> V[] map2Array(
        @Nonnull Collection<? extends T> collection,
        @Nonnull Class<? super V> aClass,
        @Nonnull Function<T, V> mapper
    ) {
        return consulo.util.collection.ContainerUtil.map2Array(collection, aClass, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> V[] map2Array(@Nonnull Collection<? extends T> collection, @Nonnull V[] to, @Nonnull Function<T, V> mapper) {
        return map2List(collection, mapper).toArray(to);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> filter(@Nonnull T[] collection, @Nonnull Predicate<? super T> condition) {
        return findAll(collection, condition);
    }

    /**
     * @return iterator with elements from the original {@param iterator} which are valid according to {@param filter} predicate.
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> Iterator<T> filterIterator(
        @Nonnull Iterator<? extends T> iterator,
        @Nonnull Predicate<? super T> filter
    ) {
        return consulo.util.collection.ContainerUtil.filterIterator(iterator, filter);
    }

    @Nonnull
    @Contract(pure = true)
    public static int[] filter(@Nonnull int[] collection, @Nonnull TIntProcedure condition) {
        IntList result = IntLists.newArrayList();
        for (int t : collection) {
            if (condition.execute(t)) {
                result.add(t);
            }
        }
        return result.isEmpty() ? ArrayUtil.EMPTY_INT_ARRAY : result.toArray();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> findAll(@Nonnull T[] collection, @Nonnull Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.findAll(collection, condition);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> filter(@Nonnull Collection<? extends T> collection, @Nonnull Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.filter(collection, condition);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> Map<K, V> filter(@Nonnull Map<K, ? extends V> map, @Nonnull Predicate<? super K> keyFilter) {
        return consulo.util.collection.ContainerUtil.filter(map, keyFilter);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> findAll(@Nonnull Collection<? extends T> collection, @Nonnull Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.findAll(collection, condition);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> skipNulls(@Nonnull Collection<? extends T> collection) {
        return findAll(collection, Predicates.notNull());
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> List<V> findAll(@Nonnull T[] collection, @Nonnull Class<V> instanceOf) {
        return findAll(Arrays.asList(collection), instanceOf);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> V[] findAllAsArray(@Nonnull T[] collection, @Nonnull Class<V> instanceOf) {
        List<V> list = findAll(Arrays.asList(collection), instanceOf);
        @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(instanceOf, list.size());
        return list.toArray(array);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> V[] findAllAsArray(@Nonnull Collection<? extends T> collection, @Nonnull Class<V> instanceOf) {
        List<V> list = findAll(collection, instanceOf);
        @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(instanceOf, list.size());
        return list.toArray(array);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] findAllAsArray(@Nonnull T[] collection, @Nonnull Predicate<? super T> instanceOf) {
        List<T> list = findAll(collection, instanceOf);
        if (list.size() == collection.length) {
            return collection;
        }
        @SuppressWarnings("unchecked") T[] array = (T[])Array.newInstance(collection.getClass().getComponentType(), list.size());
        return list.toArray(array);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> List<V> findAll(@Nonnull Collection<? extends T> collection, @Nonnull Class<V> instanceOf) {
        List<V> result = new SmartList<>();
        for (T t : collection) {
            if (instanceOf.isInstance(t)) {
                @SuppressWarnings("unchecked") V v = (V)t;
                result.add(v);
            }
        }
        return result;
    }

    public static <T> void removeDuplicates(@Nonnull Collection<T> collection) {
        Set<T> collected = newHashSet();
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
    public static Map<String, String> stringMap(@Nonnull String... keyValues) {
        Map<String, String> result = newHashMap();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            result.put(keyValues[i], keyValues[i + 1]);
        }

        return result;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Iterator<T> iterate(@Nonnull T[] array) {
        return array.length == 0 ? EmptyIterator.<T>getInstance() : Arrays.asList(array).iterator();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Iterator<T> iterate(@Nonnull Enumeration<T> enumeration) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }

            @Override
            public T next() {
                return enumeration.nextElement();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Iterable<T> iterate(@Nonnull T[] arrays, @Nonnull Predicate<? super T> condition) {
        return iterate(Arrays.asList(arrays), condition);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Iterable<T> iterate(
        @Nonnull Collection<? extends T> collection,
        @Nonnull Predicate<? super T> condition
    ) {
        if (collection.isEmpty()) {
            return emptyIterable();
        }
        return new Iterable<>() {
            @Nonnull
            @Override
            public Iterator<T> iterator() {
                return new Iterator<>() {
                    private final Iterator<? extends T> impl = collection.iterator();
                    private T next = findNext();

                    @Override
                    public boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public T next() {
                        T result = next;
                        next = findNext();
                        return result;
                    }

                    @Nullable
                    private T findNext() {
                        while (impl.hasNext()) {
                            T each = impl.next();
                            if (condition.test(each)) {
                                return each;
                            }
                        }
                        return null;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <T> Iterable<T> iterateBackward(@Nonnull List<? extends T> list) {
        return Lists.iterateBackward(list);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, E> Iterable<Pair<T, E>> zip(@Nonnull Iterable<T> iterable1, @Nonnull Iterable<E> iterable2) {
        return consulo.util.collection.ContainerUtil.zip(iterable1, iterable2);
    }

    public static <E> void swapElements(@Nonnull List<E> list, int index1, int index2) {
        consulo.util.collection.ContainerUtil.swapElements(list, index1, index2);
    }

    @Nonnull
    public static <T> List<T> collect(@Nonnull Iterator<?> iterator, @Nonnull FilteringIterator.InstanceOf<T> instanceOf) {
        @SuppressWarnings("unchecked") List<T> list = collect(FilteringIterator.create((Iterator<T>)iterator, instanceOf));
        return list;
    }

    @Deprecated
    public static <T> void addAll(@Nonnull Collection<T> collection, @Nonnull Enumeration<? extends T> enumeration) {
        consulo.util.collection.ContainerUtil.addAll(collection, enumeration);
    }

    @Nonnull
    @SafeVarargs
    public static <T, A extends T, C extends Collection<T>> C addAll(@Nonnull C collection, @Nonnull A... elements) {
        return consulo.util.collection.ContainerUtil.addAll(collection, elements);
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    @Nonnull
    @SafeVarargs
    public static <T, A extends T, C extends Collection<T>> C addAllNotNull(@Nonnull C collection, @Nonnull A... elements) {
        return consulo.util.collection.ContainerUtil.addAllNotNull(collection, elements);
    }

    @SafeVarargs
    public static <T> boolean removeAll(@Nonnull Collection<T> collection, @Nonnull T... elements) {
        return consulo.util.collection.ContainerUtil.removeAll(collection, elements);
    }

    // returns true if the collection was modified
    public static <T> boolean retainAll(@Nonnull Collection<T> collection, @Nonnull Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.retainAll(collection, condition);
    }

    @Contract(pure = true)
    public static <T, U extends T> U findInstance(@Nonnull Iterable<? extends T> iterable, @Nonnull Class<? extends U> aClass) {
        return consulo.util.collection.ContainerUtil.findInstance(iterable, aClass);
    }

    public static <T, U extends T> U findInstance(@Nonnull Iterator<? extends T> iterator, @Nonnull Class<? extends U> aClass) {
        return consulo.util.collection.ContainerUtil.findInstance(iterator, aClass);
    }

    @Contract(pure = true)
    @Nonnull
    public static <T> List<T> filterIsInstance(@Nonnull Collection<?> collection, @Nonnull Class<? extends T> aClass) {
        return consulo.util.collection.ContainerUtil.filterIsInstance(collection, aClass);
    }

    @Contract(pure = true)
    @Nonnull
    public static <T> List<T> filterIsInstance(@Nonnull Object[] collection, @Nonnull Class<? extends T> aClass) {
        return consulo.util.collection.ContainerUtil.filterIsInstance(collection, aClass);
    }

    @Nullable
    @Contract(pure = true)
    public static <T, U extends T> U findInstance(@Nonnull T[] array, @Nonnull Class<U> aClass) {
        return consulo.util.collection.ContainerUtil.findInstance(array, aClass);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> List<T> concat(@Nonnull V[] array, @Nonnull Function<V, Collection<? extends T>> fun) {
        return consulo.util.collection.ContainerUtil.concat(array, fun);
    }

    /**
     * @return read-only list consisting of the elements from the collections stored in list added together
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> concat(@Nonnull Iterable<? extends Collection<T>> list) {
        return consulo.util.collection.ContainerUtil.concat(list);
    }

    /**
     * @param appendTail specify whether additional values should be appended in front or after the list
     * @return read-only list consisting of the elements from specified list with some additional values
     * @deprecated Use {@link #append(List, Object[])} or {@link #prepend(List, Object[])} instead
     */
    @Deprecated
    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> concat(boolean appendTail, @Nonnull List<? extends T> list, @Nonnull T... values) {
        return appendTail ? concat(list, list(values)) : concat(list(values), list);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> append(@Nonnull List<? extends T> list, @Nonnull T... values) {
        return concat(list, list(values));
    }

    /**
     * prepend values in front of the list
     *
     * @return read-only list consisting of values and the elements from specified list
     */
    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> prepend(@Nonnull List<? extends T> list, @Nonnull T... values) {
        return consulo.util.collection.ContainerUtil.prepend(list, values);
    }

    /**
     * @return read-only list consisting of the two lists added together
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> concat(@Nonnull List<? extends T> list1, @Nonnull List<? extends T> list2) {
        return consulo.util.collection.ContainerUtil.concat(list1, list2);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> Iterable<T> concat(@Nonnull Iterable<? extends T>... iterables) {
        return consulo.util.collection.ContainerUtil.concat(iterables);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
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
    @SafeVarargs
    public static <T> Iterable<T> concat(@Nonnull T[]... iterables) {
        return new Iterable<>() {
            @Nonnull
            @Override
            public Iterator<T> iterator() {
                Iterator[] iterators = new Iterator[iterables.length];
                for (int i = 0; i < iterables.length; i++) {
                    T[] iterable = iterables[i];
                    iterators[i] = iterate(iterable);
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
    @SafeVarargs
    public static <T> List<T> concat(@Nonnull List<? extends T>... lists) {
        int size = 0;
        for (List<? extends T> each : lists) {
            size += each.size();
        }
        if (size == 0) {
            return emptyList();
        }
        int finalSize = size;
        return new AbstractList<>() {
            @Override
            public T get(int index) {
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
     * @return read-only list consisting of the lists added together
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> concat(@Nonnull List<List<? extends T>> lists) {
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
        for (V v : list) {
            result.addAll(listGenerator.apply(v));
        }
        return result.isEmpty() ? ContainerUtil.<T>emptyList() : result;
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

    /**
     * @return read-only collection consisting of elements from both collections
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> Collection<T> intersection(
        @Nonnull Collection<? extends T> collection1,
        @Nonnull Collection<? extends T> collection2
    ) {
        List<T> result = new ArrayList<>();
        for (T t : collection1) {
            if (collection2.contains(t)) {
                result.add(t);
            }
        }
        return result.isEmpty() ? ContainerUtil.<T>emptyList() : result;
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
    public static <T> T getFirstItem(@Nullable Collection<T> items, @Nullable T defaultResult) {
        return items == null || items.isEmpty() ? defaultResult : items.iterator().next();
    }

    /**
     * The main difference from {@code subList} is that {@code getFirstItems} does not
     * throw any exceptions, even if maxItems is greater than size of the list
     *
     * @param items    list
     * @param maxItems size of the result will be equal or less than {@code maxItems}
     * @param <T>      type of list
     * @return new list with no more than {@code maxItems} first elements
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> getFirstItems(@Nonnull List<T> items, int maxItems) {
        return items.subList(0, Math.min(maxItems, items.size()));
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
    @Contract(pure = true)
    public static <T, U> Iterator<U> mapIterator(@Nonnull Iterator<T> iterator, @Nonnull Function<T, U> mapper) {
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
    public static <U> Iterator<U> mapIterator(@Nonnull PrimitiveIterator.OfInt iterator, @Nonnull IntFunction<? extends U> mapper) {
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

    /**
     * @return read-only collection consisting of elements from the 'from' collection which are absent from the 'what' collection
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> Collection<T> subtract(@Nonnull Collection<T> from, @Nonnull Collection<T> what) {
        Set<T> set = newHashSet(from);
        set.removeAll(what);
        return set.isEmpty() ? ContainerUtil.<T>emptyList() : set;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] toArray(@Nullable Collection<T> c, @Nonnull IntFunction<? extends T[]> factory) {
        return c != null ? c.toArray(factory.apply(c.size())) : factory.apply(0);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] toArray(
        @Nonnull Collection<? extends T> c1,
        @Nonnull Collection<? extends T> c2,
        @Nonnull ArrayFactory<T> factory
    ) {
        return ArrayUtil.mergeCollections(c1, c2, factory);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] mergeCollectionsToArray(
        @Nonnull Collection<? extends T> c1,
        @Nonnull Collection<? extends T> c2,
        @Nonnull ArrayFactory<T> factory
    ) {
        return ArrayUtil.mergeCollections(c1, c2, factory);
    }

    public static <T extends Comparable<T>> void sort(@Nonnull List<T> list) {
        int size = list.size();

        if (size < 2) {
            return;
        }
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

        if (size < 2) {
            return;
        }
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

    public static <T extends Comparable<T>> void sort(@Nonnull T[] a) {
        int size = a.length;

        if (size < 2) {
            return;
        }
        if (size == 2) {
            T t0 = a[0];
            T t1 = a[1];

            if (t0.compareTo(t1) > 0) {
                a[0] = t1;
                a[1] = t0;
            }
        }
        else if (size < INSERTION_SORT_THRESHOLD) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < i; j++) {
                    T ti = a[i];
                    T tj = a[j];

                    if (ti.compareTo(tj) < 0) {
                        a[i] = tj;
                        a[j] = ti;
                    }
                }
            }
        }
        else {
            Arrays.sort(a);
        }
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

    @Nonnull
    @Contract(pure = true)
    public static <T extends Comparable<? super T>> List<T> sorted(@Nonnull Collection<? extends T> list) {
        return sorted(list, Comparator.naturalOrder());
    }

    public static <T> void sort(@Nonnull T[] a, @Nonnull Comparator<T> comparator) {
        int size = a.length;

        if (size < 2) {
            return;
        }
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
        return result.isEmpty() ? ContainerUtil.<V>emptyList() : result;
    }

    /**
     * @return read-only list consisting of the elements from the iterable converted by mapping
     */
    @Nonnull
    @Contract(pure = true)
    public static <T, V> List<V> map(@Nonnull Collection<? extends T> iterable, @Nonnull Function<T, V> mapping) {
        if (iterable.isEmpty()) {
            return emptyList();
        }
        List<V> result = new ArrayList<>(iterable.size());
        for (T t : iterable) {
            result.add(mapping.apply(t));
        }
        return result;
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

    /**
     * @return read-only list consisting of the elements from the iterable converted by mapping with nulls filtered out
     */
    @Nonnull
    @Contract(pure = true)
    public static <T, V> List<V> mapNotNull(@Nonnull Iterable<? extends T> iterable, @Nonnull Function<T, V> mapping) {
        List<V> result = new ArrayList<>();
        for (T t : iterable) {
            V o = mapping.apply(t);
            if (o != null) {
                result.add(o);
            }
        }
        return result.isEmpty() ? ContainerUtil.<V>emptyList() : result;
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
     */
    @Nonnull
    @Contract(pure = true)
    public static <T, V> List<V> mapNotNull(@Nonnull Collection<? extends T> iterable, @Nonnull Function<T, V> mapping) {
        if (iterable.isEmpty()) {
            return emptyList();
        }

        List<V> result = new ArrayList<>(iterable.size());
        for (T t : iterable) {
            V o = mapping.apply(t);
            if (o != null) {
                result.add(o);
            }
        }
        return result.isEmpty() ? ContainerUtil.<V>emptyList() : result;
    }

    /**
     * @return read-only list consisting of the elements with nulls filtered out
     */
    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> packNullables(@Nonnull T... elements) {
        List<T> list = new ArrayList<>();
        for (T element : elements) {
            addIfNotNull(list, element);
        }
        return list.isEmpty() ? ContainerUtil.<T>emptyList() : list;
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
        return result.isEmpty() ? ContainerUtil.<V>emptyList() : result;
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

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> Set<T> set(@Nonnull T... items) {
        return newHashSet(items);
    }

    public static <K, V> void putIfNotNull(K key, @Nullable V value, @Nonnull Map<K, V> result) {
        if (value != null) {
            result.put(key, value);
        }
    }

    public static <K, V> void putIfNotNull(K key, @Nullable Collection<? extends V> value, @Nonnull MultiMap<K, V> result) {
        if (value != null) {
            result.putValues(key, value);
        }
    }

    public static <K, V> void putIfNotNull(K key, @Nullable V value, @Nonnull MultiMap<K, V> result) {
        if (value != null) {
            result.putValue(key, value);
        }
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> createMaybeSingletonList(@Nullable T element) {
        return element == null ? ContainerUtil.<T>emptyList() : Collections.singletonList(element);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Set<T> createMaybeSingletonSet(@Nullable T element) {
        return element == null ? Collections.<T>emptySet() : Collections.singleton(element);
    }

    @Nonnull
    public static <T, V> V getOrCreate(@Nonnull Map<T, V> result, T key, @Nonnull V defaultValue) {
        V value = result.get(key);
        if (value == null) {
            result.put(key, value = defaultValue);
        }
        return value;
    }

    public static <T, V> V getOrCreate(@Nonnull Map<T, V> result, T key, @Nonnull Supplier<V> factory) {
        V value = result.get(key);
        if (value == null) {
            result.put(key, value = factory.get());
        }
        return value;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> V getOrElse(@Nonnull Map<T, V> result, T key, @Nonnull V defValue) {
        V value = result.get(key);
        return value == null ? defValue : value;
    }

    @Contract(pure = true)
    public static <T> boolean and(@Nonnull T[] iterable, @Nonnull Predicate<? super T> condition) {
        return and(Arrays.asList(iterable), condition);
    }

    @Contract(pure = true)
    public static <T> boolean and(@Nonnull Iterable<? extends T> iterable, @Nonnull Predicate<? super T> condition) {
        for (T t : iterable) {
            if (!condition.test(t)) {
                return false;
            }
        }
        return true;
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
        for (T t : iterable) {
            if (condition.test(t)) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static <T> int count(@Nonnull Iterable<T> iterable, @Nonnull Predicate<? super T> condition) {
        int count = 0;
        for (T t : iterable) {
            if (condition.test(t)) {
                count++;
            }
        }
        return count;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> unfold(@Nullable T t, @Nonnull NullableFunction<T, T> next) {
        if (t == null) {
            return emptyList();
        }

        List<T> list = new ArrayList<>();
        while (t != null) {
            list.add(t);
            t = next.apply(t);
        }
        return list;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> dropTail(@Nonnull List<T> items) {
        return items.subList(0, items.size() - 1);
    }

    @Nonnull
    public static <T> List<T> dropWhile(@Nonnull Iterable<T> target, @Nonnull Predicate<T> predicate) {
        boolean yielding = false;
        List<T> list = new ArrayList<>();
        for (T item : target) {
            if (yielding) {
                list.add(item);
            }
            else if (!predicate.test(item)) {
                list.add(item);
                yielding = true;
            }
        }
        return list;
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> list(@Nonnull T... items) {
        return Arrays.asList(items);
    }

    // Generalized Quick Sort. Does neither array.clone() nor list.toArray()

    public static <T> void quickSort(@Nonnull List<T> list, @Nonnull Comparator<? super T> comparator) {
        quickSort(list, comparator, 0, list.size());
    }

    private static <T> void quickSort(@Nonnull List<T> x, @Nonnull Comparator<? super T> comparator, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && comparator.compare(x.get(j), x.get(j - 1)) < 0; j--) {
                    swapElements(x, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, comparator, l, l + s, l + 2 * s);
                m = med3(x, comparator, m - s, m, m + s);
                n = med3(x, comparator, n - 2 * s, n - s, n);
            }
            m = med3(x, comparator, l, m, n); // Mid-size, med of 3
        }
        T v = x.get(m);

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off;
        int b = a;
        int c = off + len - 1;
        int d = c;
        while (true) {
            while (b <= c && comparator.compare(x.get(b), v) <= 0) {
                if (comparator.compare(x.get(b), v) == 0) {
                    swapElements(x, a++, b);
                }
                b++;
            }
            while (c >= b && comparator.compare(v, x.get(c)) <= 0) {
                if (comparator.compare(x.get(c), v) == 0) {
                    swapElements(x, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swapElements(x, b++, c--);
        }

        // Swap partition elements back to middle
        int n = off + len;
        int s = Math.min(a - off, b - a);
        vecswap(x, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            quickSort(x, comparator, off, s);
        }
        if ((s = d - c) > 1) {
            quickSort(x, comparator, n - s, s);
        }
    }

    /*
     * Returns the index of the median of the three indexed longs.
     */
    private static <T> int med3(@Nonnull List<T> x, Comparator<? super T> comparator, int a, int b, int c) {
        return comparator.compare(x.get(a), x.get(b)) < 0
            ? comparator.compare(x.get(b), x.get(c)) < 0 ? b : comparator.compare(x.get(a), x.get(c)) < 0 ? c : a
            : comparator.compare(x.get(c), x.get(b)) < 0 ? b : comparator.compare(x.get(c), x.get(a)) < 0 ? c : a;
    }

    /*
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static <T> void vecswap(List<T> x, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swapElements(x, a, b);
        }
    }

    /**
     * Merge sorted points, which are sorted by x and with equal x by y.
     * Result is put to x1 y1.
     */
    public static void mergeSortedArrays(
        @Nonnull TIntArrayList x1,
        @Nonnull TIntArrayList y1,
        @Nonnull TIntArrayList x2,
        @Nonnull TIntArrayList y2
    ) {
        TIntArrayList newX = new TIntArrayList();
        TIntArrayList newY = new TIntArrayList();

        int i = 0;
        int j = 0;

        while (i < x1.size() && j < x2.size()) {
            if (x1.get(i) < x2.get(j) || x1.get(i) == x2.get(j) && y1.get(i) < y2.get(j)) {
                newX.add(x1.get(i));
                newY.add(y1.get(i));
                i++;
            }
            else if (x1.get(i) > x2.get(j) || x1.get(i) == x2.get(j) && y1.get(i) > y2.get(j)) {
                newX.add(x2.get(j));
                newY.add(y2.get(j));
                j++;
            }
            else { //equals
                newX.add(x1.get(i));
                newY.add(y1.get(i));
                i++;
                j++;
            }
        }

        while (i < x1.size()) {
            newX.add(x1.get(i));
            newY.add(y1.get(i));
            i++;
        }

        while (j < x2.size()) {
            newX.add(x2.get(j));
            newY.add(y2.get(j));
            j++;
        }

        x1.clear();
        y1.clear();
        x1.add(newX.toNativeArray());
        y1.add(newY.toNativeArray());
    }

    /**
     * @return read-only list consisting of the elements from all of the collections
     */
    @Nonnull
    @Contract(pure = true)
    public static <E> List<E> flatten(@Nonnull Collection<E>[] collections) {
        return flatten(Arrays.asList(collections));
    }

    /**
     * Processes the list, remove all duplicates and return the list with unique elements.
     *
     * @param list must be sorted (according to the comparator), all elements must be not-null
     */
    @Nonnull
    public static <T> List<T> removeDuplicatesFromSorted(@Nonnull List<T> list, @Nonnull Comparator<? super T> comparator) {
        T prev = null;
        List<T> result = null;
        for (int i = 0; i < list.size(); i++) {
            T t = list.get(i);
            if (t == null) {
                throw new IllegalArgumentException("get(" + i + ") = null");
            }
            int cmp = prev == null ? -1 : comparator.compare(prev, t);
            if (cmp < 0) {
                if (result != null) {
                    result.add(t);
                }
            }
            else if (cmp == 0) {
                if (result == null) {
                    result = new ArrayList<>(list.size());
                    result.addAll(list.subList(0, i));
                }
            }
            else {
                throw new IllegalArgumentException("List must be sorted but get(" + (i - 1) + ")=" + list.get(i - 1) + " > get(" + i + ")=" + t);
            }
            prev = t;
        }
        return result == null ? list : result;
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

        return result.isEmpty() ? emptyList() : result;
    }

    /**
     * @return read-only list consisting of the elements from all of the collections
     */
    @Nonnull
    @Contract(pure = true)
    public static <E> List<E> flattenIterables(@Nonnull Iterable<? extends Iterable<E>> collections) {
        List<E> result = new ArrayList<>();
        for (Iterable<E> list : collections) {
            for (E e : list) {
                result.add(e);
            }
        }
        return result.isEmpty() ? ContainerUtil.<E>emptyList() : result;
    }

    @Nonnull
    public static <K, V> V[] convert(@Nonnull K[] from, @Nonnull V[] to, @Nonnull Function<K, V> fun) {
        if (to.length < from.length) {
            @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(to.getClass().getComponentType(), from.length);
            to = array;
        }
        for (int i = 0; i < from.length; i++) {
            to[i] = fun.apply(from[i]);
        }
        return to;
    }

    @Contract(pure = true)
    public static <T> boolean containsIdentity(@Nonnull Iterable<T> list, T element) {
        for (T t : list) {
            if (t == element) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static <T> int indexOfIdentity(@Nonnull List<? extends T> list, T element) {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            if (list.get(i) == element) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> boolean equalsIdentity(@Nonnull List<T> list1, @Nonnull List<T> list2) {
        int listSize = list1.size();
        if (list2.size() != listSize) {
            return false;
        }

        for (int i = 0; i < listSize; i++) {
            if (list1.get(i) != list2.get(i)) {
                return false;
            }
        }
        return true;
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

    @Nullable
    @Contract(pure = true)
    public static <T, U extends T> U findLastInstance(@Nonnull List<T> list, @Nonnull Class<U> clazz) {
        int i = lastIndexOf(list, clazz::isInstance);
        //noinspection unchecked
        return i < 0 ? null : (U)list.get(i);
    }

    @Contract(pure = true)
    public static <T, U extends T> int lastIndexOfInstance(@Nonnull List<T> list, @Nonnull Class<U> clazz) {
        return lastIndexOf(list, clazz::isInstance);
    }

    @Contract(pure = true)
    public static <T> int indexOf(@Nonnull List<? extends T> list, @Nonnull T object) {
        return indexOf(list, t -> t.equals(object));
    }

    @Nonnull
    @Contract(pure = true)
    public static <A, B> Map<B, A> reverseMap(@Nonnull Map<A, B> map) {
        Map<B, A> result = newHashMap();
        for (Map.Entry<A, B> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    @Contract(pure = true)
    public static <T> boolean processRecursively(T root, @Nonnull PairProcessor<T, List<T>> processor) {
        LinkedList<T> list = new LinkedList<>();
        list.add(root);
        while (!list.isEmpty()) {
            T o = list.removeFirst();
            if (!processor.process(o, list)) {
                return false;
            }
        }
        return true;
    }

    @Contract("null -> null; !null -> !null")
    public static <T> List<T> trimToSize(@Nullable List<T> list) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return emptyList();
        }

        if (list instanceof ArrayList arrayList) {
            arrayList.trimToSize();
        }

        return list;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Stack<T> newStack() {
        return ContainerUtilRt.newStack();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Stack<T> newStack(@Nonnull Collection<T> initial) {
        return ContainerUtilRt.newStack(initial);
    }

    @Nonnull
    @Contract(pure = true)
    @SafeVarargs
    public static <T> Stack<T> newStack(@Nonnull T... initial) {
        return ContainerUtilRt.newStack(initial);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> emptyList() {
        return ContainerUtilRt.emptyList();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> CopyOnWriteArrayList<T> createEmptyCOWList() {
        return ContainerUtilRt.createEmptyCOWList();
    }

    /**
     * Creates List which is thread-safe to modify and iterate.
     * It differs from the java.util.concurrent.CopyOnWriteArrayList in the following:
     * - faster modification in the uncontended case
     * - less memory
     * - slower modification in highly contented case (which is the kind of situation you shouldn't use COWAL anyway)
     * <p/>
     * N.B. Avoid using {@code list.toArray(new T[list.size()])} on this list because it is inherently racey and
     * therefore can return array with null elements at the end.
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> createLockFreeCopyOnWriteList() {
        return createConcurrentList();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> createLockFreeCopyOnWriteList(@Nonnull Collection<? extends T> c) {
        return new LockFreeCopyOnWriteArrayList<>(c);
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectMap() {
        return IntMaps.newConcurrentIntObjectHashMap();
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        return IntMaps.newConcurrentIntObjectHashMap(initialCapacity, loadFactor, concurrencyLevel);
    }

    @Nonnull
    @Contract(pure = true)
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectSoftValueMap() {
        return IntMaps.newConcurrentIntObjectSoftValueHashMap();
    }

    @Nonnull
    @Contract(pure = true)
    public static <V> ConcurrentLongObjectMap<V> createConcurrentLongObjectMap() {
        return LongMaps.newConcurrentLongObjectHashMap();
    }

    @Nonnull
    @Contract(pure = true)
    public static <V> ConcurrentLongObjectMap<V> createConcurrentLongObjectMap(int initialCapacity) {
        return LongMaps.newConcurrentLongObjectHashMap(initialCapacity);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakValueMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentWeakValueMap();
    }

    @Nonnull
    @Contract(pure = true)
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectWeakValueMap() {
        return IntMaps.newConcurrentIntObjectWeakValueHashMap();
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeySoftValueMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        @Nonnull HashingStrategy<K> hashingStrategy
    ) {
        //noinspection deprecation
        return consulo.util.collection.ContainerUtil.createConcurrentWeakKeySoftValueMap(
            initialCapacity,
            loadFactor,
            concurrencyLevel,
            hashingStrategy
        );
    }

    @Nonnull
    @Contract(value = " -> new", pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftKeySoftValueMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentSoftKeySoftValueMap();
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftKeySoftValueMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        @Nonnull HashingStrategy<K> hashingStrategy
    ) {
        return consulo.util.collection.ContainerUtil.createConcurrentSoftKeySoftValueMap(
            initialCapacity,
            loadFactor,
            concurrencyLevel,
            hashingStrategy
        );
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeySoftValueMap() {
        return createConcurrentWeakKeySoftValueMap(
            100,
            0.75f,
            Runtime.getRuntime().availableProcessors(),
            ContainerUtil.<K>canonicalStrategy()
        );
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeyWeakValueMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentWeakKeyWeakValueMap();
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeyWeakValueMap(@Nonnull HashingStrategy<K> strategy) {
        return consulo.util.collection.ContainerUtil.createConcurrentWeakKeyWeakValueMap(strategy);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftValueMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentSoftValueMap();
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentSoftMap();
    }

    @Nonnull
    @Contract(value = " -> new", pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentWeakMap();
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        @Nonnull HashingStrategy<K> hashingStrategy
    ) {
        //noinspection deprecation
        return consulo.util.collection.ContainerUtil.createConcurrentSoftMap(
            initialCapacity,
            loadFactor,
            concurrencyLevel,
            hashingStrategy
        );
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        @Nonnull HashingStrategy<K> hashingStrategy
    ) {
        //noinspection deprecation
        return consulo.util.collection.ContainerUtil.createConcurrentWeakMap(
            initialCapacity,
            loadFactor,
            concurrencyLevel,
            hashingStrategy
        );
    }

    @Nonnull
    @Contract(pure = true)
    @Deprecated
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap(@Nonnull HashingStrategy<K> hashingStrategy) {
        //noinspection deprecation
        return Maps.newConcurrentWeakHashMap(hashingStrategy);
    }

    /**
     * @see #createLockFreeCopyOnWriteList()
     */
    @Nonnull
    @Contract(pure = true)
    public static <T> ConcurrentList<T> createConcurrentList() {
        return new LockFreeCopyOnWriteArrayList<>();
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> ConcurrentList<T> createConcurrentList(@Nonnull Collection<? extends T> collection) {
        return new LockFreeCopyOnWriteArrayList<>(collection);
    }

    /**
     * @see #addIfNotNull(Collection, Object)
     */
    @Deprecated
    public static <T> void addIfNotNull(@Nullable T element, @Nonnull Collection<T> result) {
        ContainerUtilRt.addIfNotNull(element, result);
    }

    public static <T> void addIfNotNull(@Nonnull Collection<T> result, @Nullable T element) {
        ContainerUtilRt.addIfNotNull(result, element);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> List<V> map2List(@Nonnull T[] array, @Nonnull Function<T, V> mapper) {
        return ContainerUtilRt.map2List(array, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> List<V> map2List(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, V> mapper) {
        return ContainerUtilRt.map2List(collection, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> Set<V> map2Set(@Nonnull T[] collection, @Nonnull Function<T, V> mapper) {
        return ContainerUtilRt.map2Set(collection, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> Set<V> map2Set(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, V> mapper) {
        return ContainerUtilRt.map2Set(collection, mapper);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> Set<V> map2LinkedSet(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, V> mapper) {
        if (collection.isEmpty()) {
            return Collections.emptySet();
        }
        Set<V> set = new LinkedHashSet<>(collection.size());
        for (T t : collection) {
            set.add(mapper.apply(t));
        }
        return set;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T, V> Set<V> map2SetNotNull(@Nonnull Collection<? extends T> collection, @Nonnull Function<T, V> mapper) {
        if (collection.isEmpty()) {
            return Collections.emptySet();
        }
        Set<V> set = new HashSet<>(collection.size());
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
    public static <T> T[] toArray(@Nonnull List<T> collection, @Nonnull T[] array) {
        return ContainerUtilRt.toArray(collection, array);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> T[] toArray(@Nonnull Collection<T> c, @Nonnull T[] sample) {
        return ContainerUtilRt.toArray(c, sample);
    }

    @Nonnull
    public static <T> T[] copyAndClear(@Nonnull Collection<T> collection, @Nonnull ArrayFactory<T> factory, boolean clear) {
        int size = collection.size();
        T[] a = factory.create(size);
        if (size > 0) {
            a = collection.toArray(a);
            if (clear) {
                collection.clear();
            }
        }
        return a;
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Collection<T> toCollection(@Nonnull Iterable<T> iterable) {
        return iterable instanceof Collection ? (Collection<T>)iterable : newArrayList(iterable);
    }

    @Nonnull
    public static <T> List<T> toList(@Nonnull Enumeration<T> enumeration) {
        if (!enumeration.hasMoreElements()) {
            return Collections.emptyList();
        }

        List<T> result = new SmartList<>();
        while (enumeration.hasMoreElements()) {
            result.add(enumeration.nextElement());
        }
        return result;
    }

    @Contract(value = "null -> true", pure = true)
    public static <T> boolean isEmpty(Collection<T> collection) {
        return consulo.util.collection.ContainerUtil.isEmpty(collection);
    }

    @Contract(value = "null -> true", pure = true)
    public static boolean isEmpty(Map map) {
        return consulo.util.collection.ContainerUtil.isEmpty(map);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> notNullize(@Nullable List<T> list) {
        return Lists.notNullize(list);
    }

    @Nonnull
    @Contract(pure = true)
    public static <K, V> Map<K, V> notNullize(@Nullable Map<K, V> map) {
        return Maps.notNullize(map);
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> List<T> toMutableSmartList(List<T> oldList) {
        if (oldList.size() == 1) {
            return new SmartList<>(getFirstItem(oldList));
        }
        else if (oldList.size() == 0) {
            return new SmartList<>();
        }
        else {
            return new ArrayList<>(oldList);
        }
    }

    @Nonnull
    @Contract(pure = true)
    public static <T> Set<T> notNullize(@Nullable Set<T> set) {
        return Sets.notNullize(set);
    }

    @Nullable
    @Contract(pure = true)
    public static <T, C extends Collection<T>> C nullize(@Nullable C collection) {
        return isEmpty(collection) ? null : collection;
    }

    @Contract(pure = true)
    public static <T extends Comparable<T>> int compareLexicographically(@Nonnull List<T> o1, @Nonnull List<T> o2) {
        for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
            int result = Comparing.compare(o1.get(i), o2.get(i));
            if (result != 0) {
                return result;
            }
        }
        return o1.size() < o2.size() ? -1 : o1.size() == o2.size() ? 0 : 1;
    }

    @Contract(pure = true)
    public static <T> int compareLexicographically(@Nonnull List<T> o1, @Nonnull List<T> o2, @Nonnull Comparator<T> comparator) {
        for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
            int result = comparator.compare(o1.get(i), o2.get(i));
            if (result != 0) {
                return result;
            }
        }
        return o1.size() < o2.size() ? -1 : o1.size() == o2.size() ? 0 : 1;
    }

    /**
     * Returns a String representation of the given map, by listing all key-value pairs contained in the map.
     */
    @Nonnull
    @Contract(pure = true)
    public static String toString(@Nonnull Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<?, ?> entry = iterator.next();
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public static class KeyOrderedMultiMap<K, V> extends MultiMap<K, V> {
        public KeyOrderedMultiMap() {
            super(new TreeMap<>());
        }

        public KeyOrderedMultiMap(@Nonnull MultiMap<? extends K, ? extends V> toCopy) {
            super(toCopy);
        }

        @Nonnull
        public NavigableSet<K> navigableKeySet() {
            //noinspection unchecked
            return ((TreeMap)myMap).navigableKeySet();
        }
    }

    @Nonnull
    public static <K, V> Map<K, V> createWeakKeySoftValueMap() {
        return consulo.util.collection.ContainerUtil.createWeakKeySoftValueMap();
    }

    public static <T> void weightSort(List<T> list, Function<T, Integer> weighterFunc) {
        Collections.sort(list, (o1, o2) -> weighterFunc.apply(o2) - weighterFunc.apply(o1));
    }

    /**
     * Hard keys weak values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    @Nonnull
    public static <K, V> Map<K, V> createWeakValueMap() {
        return consulo.util.collection.ContainerUtil.createWeakValueMap();
    }

    /**
     * Soft keys hard values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    @Nonnull
    @Deprecated
    public static <K, V> Map<K, V> createSoftMap() {
        return consulo.util.collection.ContainerUtil.createSoftMap();
    }

    @Contract(value = "_ -> new", pure = true)
    @Nonnull
    @Deprecated
    public static <K, V> Map<K, V> createSoftMap(@Nonnull HashingStrategy<? super K> strategy) {
        return consulo.util.collection.ContainerUtil.createSoftMap(strategy);
    }

    /**
     * Hard keys soft values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    @Nonnull
    public static <K, V> Map<K, V> createSoftValueMap() {
        //noinspection deprecation
        return consulo.util.collection.ContainerUtil.createSoftValueMap();
    }

    /**
     * Weak keys hard values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    @Nonnull
    public static <K, V> Map<K, V> createWeakMap() {
        return Maps.newWeakHashMap();
    }

    @Contract(value = "_ -> new", pure = true)
    @Nonnull
    public static <K, V> Map<K, V> createWeakMap(int initialCapacity) {
        return Maps.newWeakHashMap(initialCapacity);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    @Nonnull
    @Deprecated
    public static <K, V> Map<K, V> createWeakMap(int initialCapacity, float loadFactor, @Nonnull HashingStrategy<? super K> strategy) {
        return Maps.newWeakHashMap(initialCapacity, loadFactor, strategy);
    }

    @Contract(value = " -> new", pure = true)
    @Nonnull
    @Deprecated
    public static <K, V> Map<K, V> createWeakKeyWeakValueMap() {
        return consulo.util.collection.ContainerUtil.createWeakKeyWeakValueMap();
    }

    public static <T> boolean all(@Nonnull T[] collection, @Nonnull Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.all(collection, condition);
    }

    public static <T> boolean all(@Nonnull Collection<? extends T> collection, @Nonnull Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.all(collection, condition);
    }

    public static <K, V> boolean any(Map<K, V> map, Predicate<Map.Entry<K, V>> predicate) {
        return consulo.util.collection.ContainerUtil.any(map, predicate);
    }

    /**
     * Returns the only item from the collection or null if collection is empty or contains more than one item
     *
     * @param items collection to get the item from
     * @param <T>   type of collection element
     * @return the only collection element or null
     */
    @Contract(pure = true)
    public static <T> T getOnlyItem(@Nullable Collection<? extends T> items) {
        return getOnlyItem(items, null);
    }

    @Contract(pure = true)
    public static <T> T getOnlyItem(@Nullable Collection<? extends T> items, @Nullable T defaultResult) {
        return items == null || items.size() != 1 ? defaultResult : items.iterator().next();
    }

    public static <T> void groupAndRuns(List<? extends T> values, BiPredicate<T, T> func, Consumer<List<? extends T>> consumer) {
        if (values.isEmpty()) {
            return;
        }

        if (values.size() == 1) {
            consumer.accept(values);
            return;
        }

        T prev = values.get(0);
        int startIndex = -1;

        for (int i = 1; i < values.size(); i++) {
            T event = values.get(i);

            try {
                if (func.test(prev, event)) {
                    if (startIndex == -1) {
                        // start from prev if not group index
                        startIndex = i - 1;
                    }
                    else {
                        // nothing group already started
                    }
                }
                else if (i == 1) {
                    // second value not equal first - eat first as single group
                    consumer.accept(Collections.singletonList(prev));

                    startIndex = i;
                }
                else if (startIndex == -1) {
                    // if group not started - start fake group
                    // it will eat by groupper, or return as prev
                    startIndex = i;
                }
                else {
                    // finish group and start fake
                    List<? extends T> subList = values.subList(startIndex, i);
                    consumer.accept(subList);

                    startIndex = i;
                }
            }
            finally {
                prev = event;
            }
        }

        if (startIndex != -1) {
            List<? extends T> list = values.subList(startIndex, values.size());
            consumer.accept(list);
        }
    }
}
