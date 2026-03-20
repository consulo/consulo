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
import consulo.util.collection.Stack;
import consulo.util.collection.*;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.longs.ConcurrentLongObjectMap;
import consulo.util.collection.primitive.longs.LongMaps;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntProcedure;
import org.jspecify.annotations.Nullable;
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

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> T[] ar(T... elements) {
        return elements;
    }

    
    @Contract(pure = true)
    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    
    @Contract(pure = true)
    public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
        return new HashMap<>(map);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <K, V> Map<K, V> newHashMap(Pair<K, ? extends V> first, Pair<K, ? extends V>... entries) {
        return ContainerUtilRt.newHashMap(first, entries);
    }

    
    @Contract(pure = true)
    public static <K, V> Map<K, V> newHashMap(List<K> keys, List<V> values) {
        return ContainerUtilRt.newHashMap(keys, values);
    }

    
    @Contract(pure = true)
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<>();
    }

    
    @Contract(pure = true)
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap(Map<K, V> map) {
        return new TreeMap<>(map);
    }

    
    @Contract(pure = true)
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    
    @Contract(pure = true)
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int capacity) {
        return new LinkedHashMap<>(capacity);
    }

    
    @Contract(pure = true)
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(Map<K, V> map) {
        return new LinkedHashMap<>(map);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(Pair<K, V> first, Pair<K, V>... entries) {
        return ContainerUtilRt.newLinkedHashMap(first, entries);
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <K, V> Map<K, V> newTroveMap() {
        return new HashMap<>();
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <K, V> Map<K, V> newTroveMap(HashingStrategy<K> strategy) {
        return Maps.newHashMap(strategy);
    }

    
    @Contract(pure = true)
    public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Class<K> keyType) {
        return new EnumMap<>(keyType);
    }

    @SuppressWarnings("unchecked")
    
    @Contract(pure = true)
    @Deprecated
    public static <T> HashingStrategy<T> canonicalStrategy() {
        return HashingStrategy.canonical();
    }

    @SuppressWarnings("unchecked")
    
    @Contract(pure = true)
    @Deprecated
    public static <T> HashingStrategy<T> identityStrategy() {
        return HashingStrategy.identity();
    }

    
    @Contract(pure = true)
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
        return new IdentityHashMap<>();
    }

    
    @Contract(pure = true)
    public static <T> LinkedList<T> newLinkedList() {
        return new LinkedList<>();
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> LinkedList<T> newLinkedList(T... elements) {
        return ContainerUtilRt.newLinkedList(elements);
    }

    
    @Contract(pure = true)
    public static <T> LinkedList<T> newLinkedList(Iterable<? extends T> elements) {
        return ContainerUtilRt.newLinkedList(elements);
    }

    
    @Contract(pure = true)
    public static <T> ArrayList<T> newArrayList() {
        return new ArrayList<>();
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <E> ArrayList<E> newArrayList(E... array) {
        return ContainerUtilRt.newArrayList(array);
    }

    
    @Contract(pure = true)
    public static <E> ArrayList<E> newArrayList(Iterable<? extends E> iterable) {
        return ContainerUtilRt.newArrayList(iterable);
    }

    
    @Contract(pure = true)
    public static <T> ArrayList<T> newArrayListWithCapacity(int size) {
        return new ArrayList<>(size);
    }

    
    @Contract(pure = true)
    public static <T> List<T> newArrayList(T[] elements, int start, int end) {
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

    
    @Contract(pure = true)
    public static <T> List<T> newSmartList() {
        return new SmartList<>();
    }

    
    @Contract(pure = true)
    public static <T> List<T> newSmartList(T element) {
        return new SmartList<>(element);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> newSmartList(T... elements) {
        return new SmartList<>(elements);
    }

    
    @Contract(pure = true)
    public static <T> HashSet<T> newHashSet() {
        return new HashSet<>();
    }

    
    @Contract(pure = true)
    public static <T> HashSet<T> newHashSet(int initialCapacity) {
        return new HashSet<>(initialCapacity);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> HashSet<T> newHashSet(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    
    @Contract(pure = true)
    public static <T> HashSet<T> newHashSet(Iterable<? extends T> iterable) {
        return ContainerUtilRt.newHashSet(iterable);
    }

    
    public static <T> HashSet<T> newHashSet(Iterator<? extends T> iterator) {
        return ContainerUtilRt.newHashSet(iterator);
    }

    
    @Contract(pure = true)
    public static <T> Set<T> newHashOrEmptySet(@Nullable Iterable<? extends T> iterable) {
        boolean empty = iterable == null || iterable instanceof Collection collection && collection.isEmpty();
        return empty ? Collections.<T>emptySet() : ContainerUtilRt.newHashSet(iterable);
    }

    
    @Contract(pure = true)
    public static <T> LinkedHashSet<T> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }

    
    @Contract(pure = true)
    public static <T> LinkedHashSet<T> newLinkedHashSet(Iterable<? extends T> elements) {
        return ContainerUtilRt.newLinkedHashSet(elements);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> LinkedHashSet<T> newLinkedHashSet(T... elements) {
        return ContainerUtilRt.newLinkedHashSet(elements);
    }

    
    @Contract(pure = true)
    public static <T> LinkedHashSet<T> newLinkedHashSet(Iterable<? extends T> elements, T element) {
        LinkedHashSet<T> set = ContainerUtilRt.newLinkedHashSet(elements);
        set.add(element);
        return set;
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newTroveSet() {
        return new HashSet<>();
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newTroveSet(HashingStrategy<T> strategy) {
        return Sets.newHashSet(strategy);
    }

    
    @Contract(pure = true)
    @Deprecated
    @SafeVarargs
    public static <T> Set<T> newTroveSet(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    
    @Contract(pure = true)
    @Deprecated
    @SafeVarargs
    public static <T> Set<T> newTroveSet(HashingStrategy<T> strategy, T... elements) {
        return Sets.newHashSet(Arrays.asList(elements), strategy);
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newTroveSet(HashingStrategy<T> strategy, Collection<T> elements) {
        return Sets.newHashSet(elements, strategy);
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newTroveSet(Collection<T> elements) {
        return new HashSet<>(elements);
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <K> Set<K> newIdentityTroveSet() {
        return Sets.newHashSet(HashingStrategy.identity());
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <K> Set<K> newIdentityTroveSet(int initialCapacity) {
        return Sets.newHashSet(initialCapacity, HashingStrategy.identity());
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <K> Set<K> newIdentityTroveSet(Collection<K> collection) {
        return Sets.newHashSet(collection, HashingStrategy.identity());
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <K, V> Map<K, V> newIdentityTroveMap() {
        return Maps.newHashMap(HashingStrategy.identity());
    }

    
    @Contract(pure = true)
    public static <T> TreeSet<T> newTreeSet() {
        return new TreeSet<>();
    }

    
    @Contract(pure = true)
    public static <T> TreeSet<T> newTreeSet(Iterable<? extends T> elements) {
        return ContainerUtilRt.newTreeSet(elements);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> TreeSet<T> newTreeSet(T... elements) {
        return ContainerUtilRt.newTreeSet(elements);
    }

    
    @Contract(pure = true)
    public static <T> TreeSet<T> newTreeSet(@Nullable Comparator<? super T> comparator) {
        return new TreeSet<>(comparator);
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <T> Set<T> newConcurrentSet() {
        return consulo.util.collection.ContainerUtil.newConcurrentSet();
    }

    
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

    
    @Contract(pure = true)
    public static <E> List<E> reverse(List<E> elements) {
        return consulo.util.collection.ContainerUtil.reverse(elements);
    }

    
    @Contract(pure = true)
    public static <K, V> Map<K, V> union(Map<? extends K, ? extends V> map, Map<? extends K, ? extends V> map2) {
        return consulo.util.collection.ContainerUtil.union(map, map2);
    }

    
    @Contract(pure = true)
    public static <T> Set<T> union(Set<T> set, Set<T> set2) {
        return consulo.util.collection.ContainerUtil.union(set, set2);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <E> Set<E> immutableSet(E... elements) {
        switch (elements.length) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(elements[0]);
            default:
                return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(elements)));
        }
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <E> ImmutableList<E> immutableList(E... array) {
        return new ImmutableListBackedByArray<>(array);
    }

    
    @Contract(pure = true)
    public static <E> ImmutableList<E> immutableList(List<? extends E> list) {
        return new ImmutableListBackedByList<>(list);
    }

    
    @Contract(pure = true)
    public static <K, V> ImmutableMapBuilder<K, V> immutableMapBuilder() {
        return ImmutableMapBuilder.newBuilder();
    }

    
    public static <K, V> MultiMap<K, V> groupBy(Iterable<V> collection, Function<V, K> grouper) {
        return consulo.util.collection.ContainerUtil.groupBy(collection, grouper);
    }

    @Contract(pure = true)
    public static <T> T getOrElse(List<T> elements, int i, T defaultValue) {
        return elements.size() > i ? elements.get(i) : defaultValue;
    }

    private static class ImmutableListBackedByList<E> extends ImmutableList<E> {
        private final List<? extends E> myStore;

        private ImmutableListBackedByList(List<? extends E> list) {
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

        private ImmutableListBackedByArray(E[] array) {
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

    
    @Contract(pure = true)
    public static <K, V> Map<K, V> intersection(Map<K, V> map1, Map<K, V> map2) {
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

    
    @Contract(pure = true)
    public static <K, V> Map<K, Couple<V>> diff(Map<K, V> map1, Map<K, V> map2) {
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
        List<? extends T> list1,
        List<? extends T> list2,
        Comparator<? super T> comparator,
        boolean mergeEqualItems,
        Consumer<? super T> processor
    ) {
        consulo.util.collection.ContainerUtil.processSortedListsInOrder(list1, list2, comparator, mergeEqualItems, processor);
    }

    
    @Contract(pure = true)
    public static <T> List<T> mergeSortedLists(
        List<? extends T> list1,
        List<? extends T> list2,
        Comparator<? super T> comparator,
        boolean mergeEqualItems
    ) {
        List<T> result = new ArrayList<>(list1.size() + list2.size());
        processSortedListsInOrder(list1, list2, comparator, mergeEqualItems, result::add);
        return result;
    }

    
    @Contract(pure = true)
    public static <T> List<T> mergeSortedArrays(
        T[] list1,
        T[] list2,
        Comparator<? super T> comparator,
        boolean mergeEqualItems,
        @Nullable Predicate<? super T> filter
    ) {
        int index1 = 0;
        int index2 = 0;
        List<T> result = new ArrayList<>(list1.length + list2.length);

        while (index1 < list1.length || index2 < list2.length) {
            if (index1 >= list1.length) {
                T t = list2[index2++];
                if (filter != null && !filter.test(t)) {
                    continue;
                }
                result.add(t);
            }
            else if (index2 >= list2.length) {
                T t = list1[index1++];
                if (filter != null && !filter.test(t)) {
                    continue;
                }
                result.add(t);
            }
            else {
                T element1 = list1[index1];
                if (filter != null && !filter.test(element1)) {
                    index1++;
                    continue;
                }
                T element2 = list2[index2];
                if (filter != null && !filter.test(element2)) {
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

    
    @Contract(pure = true)
    public static <T> List<T> subList(List<T> list, int from) {
        return list.subList(from, list.size());
    }

    @Deprecated
    public static <T> void addAll(Collection<T> collection, Iterable<? extends T> appendix) {
        consulo.util.collection.ContainerUtil.addAll(collection, appendix);
    }

    @Deprecated
    public static <T> void addAll(Collection<T> collection, Iterator<? extends T> iterator) {
        consulo.util.collection.ContainerUtil.addAll(collection, iterator);
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    public static <T> void addAllNotNull(Collection<T> collection, Iterable<? extends T> elements) {
        consulo.util.collection.ContainerUtil.addAllNotNull(collection, elements);
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    public static <T> void addAllNotNull(Collection<T> collection, Iterator<? extends T> elements) {
        consulo.util.collection.ContainerUtil.addAllNotNull(collection, elements);
    }

    
    public static <T> List<T> collect(Iterator<T> iterator) {
        return consulo.util.collection.ContainerUtil.collect(iterator);
    }

    
    public static <T> Set<T> collectSet(Iterator<T> iterator) {
        if (!iterator.hasNext()) {
            return Collections.emptySet();
        }
        Set<T> hashSet = newHashSet();
        addAll(hashSet, iterator);
        return hashSet;
    }

    
    public static <K, V> Map<K, V> newMapFromKeys(Iterator<K> keys, Function<K, V> valueConvertor) {
        Map<K, V> map = newHashMap();
        while (keys.hasNext()) {
            K key = keys.next();
            map.put(key, valueConvertor.apply(key));
        }
        return map;
    }

    
    public static <K, V> Map<K, V> newMapFromValues(Iterator<V> values, Function<V, K> keyConvertor) {
        return consulo.util.collection.ContainerUtil.newMapFromValues(values, keyConvertor);
    }

    public static <K, V> void fillMapWithValues(
        Map<K, V> map,
        Iterator<V> values,
        Function<V, K> keyConvertor
    ) {
        consulo.util.collection.ContainerUtil.fillMapWithValues(map, values, keyConvertor);
    }

    
    public static <K, V> Map<K, Set<V>> classify(Iterator<V> iterator, Function<V, K> keyConvertor) {
        return consulo.util.collection.ContainerUtil.classify(iterator, keyConvertor);
    }

    
    @Contract(pure = true)
    public static <T> Iterator<T> emptyIterator() {
        return EmptyIterator.getInstance();
    }

    
    @Contract(pure = true)
    public static <T> Iterable<T> emptyIterable() {
        return EmptyIterable.getInstance();
    }

    @Contract(pure = true)
    public static <T> @Nullable T find(T[] array, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.find(array, condition);
    }

    public static <T> boolean process(Iterable<? extends T> iterable, Predicate<T> processor) {
        return consulo.util.collection.ContainerUtil.process(iterable, processor);
    }

    public static <T> boolean process(List<? extends T> list, Predicate<T> processor) {
        return consulo.util.collection.ContainerUtil.process(list, processor);
    }

    public static <T> boolean process(T[] iterable, Predicate<? super T> processor) {
        return consulo.util.collection.ContainerUtil.process(iterable, processor);
    }

    public static <T> boolean process(Iterator<T> iterator, Predicate<? super T> processor) {
        return consulo.util.collection.ContainerUtil.process(iterator, processor);
    }

    @Contract(pure = true)
    public static <T, V extends T> @Nullable V find(Iterable<V> iterable, Predicate<T> condition) {
        return consulo.util.collection.ContainerUtil.find(iterable, condition);
    }

    @Contract(pure = true)
    public static <T> @Nullable T find(Iterable<? extends T> iterable, T equalTo) {
        return consulo.util.collection.ContainerUtil.find(iterable, equalTo);
    }

    @Contract(pure = true)
    public static <T> @Nullable T find(Iterator<? extends T> iterator, T equalTo) {
        return consulo.util.collection.ContainerUtil.find(iterator, equalTo);
    }

    public static @Nullable <T, V extends T> V find(Iterator<V> iterator, Predicate<T> condition) {
        return consulo.util.collection.ContainerUtil.find(iterator, condition);
    }

    
    @Contract(pure = true)
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2Map(T[] collection, Function<T, Pair<KEY, VALUE>> mapper) {
        return consulo.util.collection.ContainerUtil.map2Map(collection, mapper);
    }

    
    @Contract(pure = true)
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2Map(
        Collection<? extends T> collection,
        Function<T, Pair<KEY, VALUE>> mapper
    ) {
        return consulo.util.collection.ContainerUtil.map2Map(collection, mapper);
    }

    
    @Contract(pure = true)
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2MapNotNull(T[] collection, Function<T, Pair<KEY, VALUE>> mapper) {
        return map2MapNotNull(Arrays.asList(collection), mapper);
    }

    
    @Contract(pure = true)
    public static <T, KEY, VALUE> Map<KEY, VALUE> map2MapNotNull(
        Collection<? extends T> collection,
        Function<T, Pair<KEY, VALUE>> mapper
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

    
    @Contract(pure = true)
    public static <KEY, VALUE> Map<KEY, VALUE> map2Map(Collection<Pair<KEY, VALUE>> collection) {
        Map<KEY, VALUE> result = new HashMap<>(collection.size());
        for (Pair<KEY, VALUE> pair : collection) {
            result.put(pair.first, pair.second);
        }
        return result;
    }

    
    @Contract(pure = true)
    public static <T> Object[] map2Array(T[] array, Function<T, Object> mapper) {
        return consulo.util.collection.ContainerUtil.map2Array(array, mapper);
    }

    
    @Contract(pure = true)
    public static <T> Object[] map2Array(Collection<T> array, Function<T, Object> mapper) {
        return consulo.util.collection.ContainerUtil.map2Array(array, mapper);
    }

    
    @Contract(pure = true)
    public static <T, V> V[] map2Array(T[] array, Class<? super V> aClass, Function<T, V> mapper) {
        return consulo.util.collection.ContainerUtil.map2Array(array, aClass, mapper);
    }

    
    @Contract(pure = true)
    public static <T, V> V[] map2Array(
        Collection<? extends T> collection,
        Class<? super V> aClass,
        Function<T, V> mapper
    ) {
        return consulo.util.collection.ContainerUtil.map2Array(collection, aClass, mapper);
    }

    
    @Contract(pure = true)
    public static <T, V> V[] map2Array(Collection<? extends T> collection, V[] to, Function<T, V> mapper) {
        return consulo.util.collection.ContainerUtil.map2Array(collection, to, mapper);
    }

    
    @Contract(pure = true)
    public static <T> List<T> filter(T[] collection, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.filter(collection, condition);
    }

    /**
     * @return iterator with elements from the original {@param iterator} which are valid according to {@param filter} predicate.
     */
    
    @Contract(pure = true)
    public static <T> Iterator<T> filterIterator(
        Iterator<? extends T> iterator,
        Predicate<? super T> filter
    ) {
        return consulo.util.collection.ContainerUtil.filterIterator(iterator, filter);
    }

    
    @Contract(pure = true)
    public static int[] filter(int[] collection, TIntProcedure condition) {
        IntList result = IntLists.newArrayList();
        for (int t : collection) {
            if (condition.execute(t)) {
                result.add(t);
            }
        }
        return result.isEmpty() ? ArrayUtil.EMPTY_INT_ARRAY : result.toArray();
    }

    
    @Contract(pure = true)
    public static <T> List<T> findAll(T[] collection, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.findAll(collection, condition);
    }

    
    @Contract(pure = true)
    public static <T> List<T> filter(Collection<? extends T> collection, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.filter(collection, condition);
    }

    
    @Contract(pure = true)
    public static <K, V> Map<K, V> filter(Map<K, ? extends V> map, Predicate<? super K> keyFilter) {
        return consulo.util.collection.ContainerUtil.filter(map, keyFilter);
    }

    
    @Contract(pure = true)
    public static <T> List<T> findAll(Collection<? extends T> collection, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.findAll(collection, condition);
    }

    
    @Contract(pure = true)
    public static <T> List<T> skipNulls(Collection<? extends T> collection) {
        return consulo.util.collection.ContainerUtil.skipNulls(collection);
    }

    
    @Contract(pure = true)
    public static <T, V> List<V> findAll(T[] collection, Class<V> instanceOf) {
        return consulo.util.collection.ContainerUtil.findAll(collection, instanceOf);
    }

    
    @Contract(pure = true)
    public static <T, V> V[] findAllAsArray(T[] collection, Class<V> instanceOf) {
        List<V> list = findAll(Arrays.asList(collection), instanceOf);
        @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(instanceOf, list.size());
        return list.toArray(array);
    }

    
    @Contract(pure = true)
    public static <T, V> V[] findAllAsArray(Collection<? extends T> collection, Class<V> instanceOf) {
        List<V> list = findAll(collection, instanceOf);
        @SuppressWarnings("unchecked") V[] array = (V[])Array.newInstance(instanceOf, list.size());
        return list.toArray(array);
    }

    
    @Contract(pure = true)
    public static <T> T[] findAllAsArray(T[] collection, Predicate<? super T> instanceOf) {
        List<T> list = findAll(collection, instanceOf);
        if (list.size() == collection.length) {
            return collection;
        }
        @SuppressWarnings("unchecked") T[] array = (T[])Array.newInstance(collection.getClass().getComponentType(), list.size());
        return list.toArray(array);
    }

    
    @Contract(pure = true)
    public static <T, V> List<V> findAll(Collection<? extends T> collection, Class<V> instanceOf) {
        return consulo.util.collection.ContainerUtil.findAll(collection, instanceOf);
    }

    public static <T> void removeDuplicates(Collection<T> collection) {
        consulo.util.collection.ContainerUtil.removeDuplicates(collection);
    }

    
    @Contract(pure = true)
    public static Map<String, String> stringMap(String... keyValues) {
        Map<String, String> result = newHashMap();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            result.put(keyValues[i], keyValues[i + 1]);
        }

        return result;
    }

    
    @Contract(pure = true)
    public static <T> Iterator<T> iterate(T[] array) {
        return array.length == 0 ? EmptyIterator.<T>getInstance() : Arrays.asList(array).iterator();
    }

    
    @Contract(pure = true)
    public static <T> Iterator<T> iterate(Enumeration<T> enumeration) {
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

    
    @Contract(pure = true)
    public static <T> Iterable<T> iterate(T[] arrays, Predicate<? super T> condition) {
        return iterate(Arrays.asList(arrays), condition);
    }

    
    @Contract(pure = true)
    public static <T> Iterable<T> iterate(
        Collection<? extends T> collection,
        Predicate<? super T> condition
    ) {
        if (collection.isEmpty()) {
            return emptyIterable();
        }
        return new Iterable<>() {
            
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

                    private @Nullable T findNext() {
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

    
    @Contract(pure = true)
    @Deprecated
    public static <T> Iterable<T> iterateBackward(List<? extends T> list) {
        return Lists.iterateBackward(list);
    }

    
    @Contract(pure = true)
    public static <T, E> Iterable<Pair<T, E>> zip(Iterable<T> iterable1, Iterable<E> iterable2) {
        return consulo.util.collection.ContainerUtil.zip(iterable1, iterable2);
    }

    public static <E> void swapElements(List<E> list, int index1, int index2) {
        consulo.util.collection.ContainerUtil.swapElements(list, index1, index2);
    }

    
    public static <T> List<T> collect(Iterator<?> iterator, FilteringIterator.InstanceOf<T> instanceOf) {
        @SuppressWarnings("unchecked") List<T> list = collect(FilteringIterator.create((Iterator<T>)iterator, instanceOf));
        return list;
    }

    @Deprecated
    public static <T> void addAll(Collection<T> collection, Enumeration<? extends T> enumeration) {
        consulo.util.collection.ContainerUtil.addAll(collection, enumeration);
    }

    
    @SafeVarargs
    public static <T, A extends T, C extends Collection<T>> C addAll(C collection, A... elements) {
        return consulo.util.collection.ContainerUtil.addAll(collection, elements);
    }

    /**
     * Adds all not-null elements from the {@code elements}, ignoring nulls
     */
    
    @SafeVarargs
    public static <T, A extends T, C extends Collection<T>> C addAllNotNull(C collection, A... elements) {
        return consulo.util.collection.ContainerUtil.addAllNotNull(collection, elements);
    }

    @SafeVarargs
    public static <T> boolean removeAll(Collection<T> collection, T... elements) {
        return consulo.util.collection.ContainerUtil.removeAll(collection, elements);
    }

    // returns true if the collection was modified
    public static <T> boolean retainAll(Collection<T> collection, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.retainAll(collection, condition);
    }

    @Contract(pure = true)
    public static <T, U extends T> U findInstance(Iterable<? extends T> iterable, Class<? extends U> aClass) {
        return consulo.util.collection.ContainerUtil.findInstance(iterable, aClass);
    }

    public static <T, U extends T> U findInstance(Iterator<? extends T> iterator, Class<? extends U> aClass) {
        return consulo.util.collection.ContainerUtil.findInstance(iterator, aClass);
    }

    @Contract(pure = true)
    
    public static <T> List<T> filterIsInstance(Collection<?> collection, Class<? extends T> aClass) {
        return consulo.util.collection.ContainerUtil.filterIsInstance(collection, aClass);
    }

    @Contract(pure = true)
    
    public static <T> List<T> filterIsInstance(Object[] collection, Class<? extends T> aClass) {
        return consulo.util.collection.ContainerUtil.filterIsInstance(collection, aClass);
    }

    @Contract(pure = true)
    public static <T, U extends T> @Nullable U findInstance(T[] array, Class<U> aClass) {
        return consulo.util.collection.ContainerUtil.findInstance(array, aClass);
    }

    
    @Contract(pure = true)
    public static <T, V> List<T> concat(V[] array, Function<V, Collection<? extends T>> fun) {
        return consulo.util.collection.ContainerUtil.concat(array, fun);
    }

    /**
     * @return read-only list consisting of the elements from the collections stored in list added together
     */
    
    @Contract(pure = true)
    public static <T> List<T> concat(Iterable<? extends Collection<T>> list) {
        return consulo.util.collection.ContainerUtil.concat(list);
    }

    /**
     * @param appendTail specify whether additional values should be appended in front or after the list
     * @return read-only list consisting of the elements from specified list with some additional values
     * @deprecated Use {@link #append(List, Object[])} or {@link #prepend(List, Object[])} instead
     */
    @Deprecated
    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> concat(boolean appendTail, List<? extends T> list, T... values) {
        return consulo.util.collection.ContainerUtil.concat(appendTail, list, values);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> append(List<? extends T> list, T... values) {
        return concat(list, list(values));
    }

    /**
     * prepend values in front of the list
     *
     * @return read-only list consisting of values and the elements from specified list
     */
    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> prepend(List<? extends T> list, T... values) {
        return consulo.util.collection.ContainerUtil.prepend(list, values);
    }

    /**
     * @return read-only list consisting of the two lists added together
     */
    
    @Contract(pure = true)
    public static <T> List<T> concat(List<? extends T> list1, List<? extends T> list2) {
        return consulo.util.collection.ContainerUtil.concat(list1, list2);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> Iterable<T> concat(Iterable<? extends T>... iterables) {
        return consulo.util.collection.ContainerUtil.concat(iterables);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> Iterator<T> concatIterators(Iterator<T>... iterators) {
        return new SequenceIterator<>(iterators);
    }

    
    @Contract(pure = true)
    public static <T> Iterator<T> concatIterators(Collection<Iterator<T>> iterators) {
        return new SequenceIterator<>(iterators);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> Iterable<T> concat(T[]... iterables) {
        return new Iterable<>() {
            
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
    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> concat(List<? extends T>... lists) {
        return consulo.util.collection.ContainerUtil.concat(lists);
    }

    /**
     * @return read-only list consisting of the lists added together
     */
    
    @Contract(pure = true)
    public static <T> List<T> concat(List<List<? extends T>> lists) {
        return consulo.util.collection.ContainerUtil.concat(lists);
    }

    /**
     * @return read-only list consisting of the lists (made by listGenerator) added together
     */
    
    @Contract(pure = true)
    public static <T, V> List<T> concat(Iterable<? extends V> list, Function<V, Collection<? extends T>> listGenerator) {
        return consulo.util.collection.ContainerUtil.concat(list, listGenerator);
    }

    @Contract(pure = true)
    public static <T> boolean intersects(Collection<? extends T> collection1, Collection<? extends T> collection2) {
        return consulo.util.collection.ContainerUtil.intersects(collection1, collection2);
    }

    /**
     * @return read-only collection consisting of elements from both collections
     */
    
    @Contract(pure = true)
    public static <T> Collection<T> intersection(
        Collection<? extends T> collection1,
        Collection<? extends T> collection2
    ) {
        return consulo.util.collection.ContainerUtil.intersection(collection1, collection2);
    }

    @Contract(pure = true)
    public static <T> @Nullable T getFirstItem(@Nullable Collection<T> items) {
        return consulo.util.collection.ContainerUtil.getFirstItem(items);
    }

    @Contract(pure = true)
    public static <T> @Nullable T getFirstItem(@Nullable List<T> items) {
        return consulo.util.collection.ContainerUtil.getFirstItem(items);
    }

    @Contract(pure = true)
    public static <T> T getFirstItem(@Nullable Collection<T> items, @Nullable T defaultResult) {
        return consulo.util.collection.ContainerUtil.getFirstItem(items, defaultResult);
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
    
    @Contract(pure = true)
    public static <T> List<T> getFirstItems(List<T> items, int maxItems) {
        return items.subList(0, Math.min(maxItems, items.size()));
    }

    @Contract(pure = true)
    public static <T> @Nullable T iterateAndGetLastItem(Iterable<T> items) {
        return consulo.util.collection.ContainerUtil.iterateAndGetLastItem(items);
    }

    
    @Contract(pure = true)
    public static <T, U> Iterator<U> mapIterator(Iterator<T> iterator, Function<T, U> mapper) {
        return Iterators.mapIterator(iterator, mapper);
    }

    
    @Contract(pure = true)
    public static <U> Iterator<U> mapIterator(PrimitiveIterator.OfInt iterator, IntFunction<? extends U> mapper) {
        return Iterators.mapIterator(iterator, mapper);
    }

    @Contract(pure = true)
    public static <T, L extends List<T>> @Nullable T getLastItem(@Nullable L list, @Nullable T def) {
        return consulo.util.collection.ContainerUtil.getLastItem(list, def);
    }

    @Contract(pure = true)
    public static <T, L extends List<T>> @Nullable T getLastItem(@Nullable L list) {
        return consulo.util.collection.ContainerUtil.getLastItem(list);
    }

    /**
     * @return read-only collection consisting of elements from the 'from' collection which are absent from the 'what' collection
     */
    
    @Contract(pure = true)
    public static <T> Collection<T> subtract(Collection<T> from, Collection<T> what) {
        return consulo.util.collection.ContainerUtil.subtract(from, what);
    }

    
    @Contract(pure = true)
    public static <T> T[] toArray(@Nullable Collection<T> c, IntFunction<? extends T[]> factory) {
        return consulo.util.collection.ContainerUtil.toArray(c, factory);
    }

    
    @Contract(pure = true)
    public static <T> T[] toArray(
        Collection<? extends T> c1,
        Collection<? extends T> c2,
        ArrayFactory<T> factory
    ) {
        return consulo.util.collection.ArrayUtil.mergeCollections(c1, c2, factory);
    }

    
    @Contract(pure = true)
    public static <T> T[] mergeCollectionsToArray(
        Collection<? extends T> c1,
        Collection<? extends T> c2,
        ArrayFactory<T> factory
    ) {
        return consulo.util.collection.ArrayUtil.mergeCollections(c1, c2, factory);
    }

    public static <T extends Comparable<T>> void sort(List<T> list) {
        consulo.util.collection.ContainerUtil.sort(list);
    }

    public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        consulo.util.collection.ContainerUtil.sort(list, comparator);
    }

    public static <T extends Comparable<T>> void sort(T[] a) {
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

    
    @Contract(pure = true)
    public static <T> List<T> sorted(Collection<? extends T> list, Comparator<? super T> comparator) {
        return consulo.util.collection.ContainerUtil.sorted(list, comparator);
    }

    
    @Contract(pure = true)
    public static <T> List<T> sorted(Iterable<? extends T> list, Comparator<? super T> comparator) {
        return consulo.util.collection.ContainerUtil.sorted(list, comparator);
    }

    
    @Contract(pure = true)
    public static <T extends Comparable<? super T>> List<T> sorted(Collection<? extends T> list) {
        return sorted(list, Comparator.naturalOrder());
    }

    public static <T> void sort(T[] a, Comparator<T> comparator) {
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
    
    @Contract(pure = true)
    public static <T, V> List<V> map(Iterable<? extends T> iterable, Function<T, V> mapping) {
        return consulo.util.collection.ContainerUtil.map(iterable, mapping);
    }

    /**
     * @return read-only list consisting of the elements from the iterable converted by mapping
     */
    
    @Contract(pure = true)
    public static <T, V> List<V> map(Collection<? extends T> iterable, Function<T, V> mapping) {
        return consulo.util.collection.ContainerUtil.map(iterable, mapping);
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
     */
    
    @Contract(pure = true)
    public static <T, V> List<V> mapNotNull(T[] array, Function<T, V> mapping) {
        return consulo.util.collection.ContainerUtil.mapNotNull(array, mapping);
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
     */
    
    @Contract(pure = true)
    public static <T, V> V[] mapNotNull(T[] array, Function<T, V> mapping, V[] emptyArray) {
        return consulo.util.collection.ContainerUtil.mapNotNull(array, mapping, emptyArray);
    }

    /**
     * @return read-only list consisting of the elements from the iterable converted by mapping with nulls filtered out
     */
    
    @Contract(pure = true)
    public static <T, V> List<V> mapNotNull(Iterable<? extends T> iterable, Function<T, V> mapping) {
        return consulo.util.collection.ContainerUtil.mapNotNull(iterable, mapping);
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping with nulls filtered out
     */
    
    @Contract(pure = true)
    public static <T, V> List<V> mapNotNull(Collection<? extends T> iterable, Function<T, V> mapping) {
        return consulo.util.collection.ContainerUtil.mapNotNull(iterable, mapping);
    }

    /**
     * @return read-only list consisting of the elements with nulls filtered out
     */
    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> packNullables(T... elements) {
        List<T> list = new ArrayList<>();
        for (T element : elements) {
            addIfNotNull(list, element);
        }
        return list.isEmpty() ? List.of() : list;
    }

    /**
     * @return read-only list consisting of the elements from the array converted by mapping
     */
    
    @Contract(pure = true)
    public static <T, V> List<V> map(T[] array, Function<T, V> mapping) {
        return consulo.util.collection.ContainerUtil.map(array, mapping);
    }

    
    @Contract(pure = true)
    public static <T, V> V[] map(T[] arr, Function<T, V> mapping, V[] emptyArray) {
        return consulo.util.collection.ContainerUtil.map(arr, mapping, emptyArray);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> Set<T> set(T... items) {
        return newHashSet(items);
    }

    public static <K, V> void putIfNotNull(K key, @Nullable V value, Map<K, V> result) {
        Maps.putIfNotNull(key, value, result);
    }

    public static <K, V> void putIfNotNull(K key, @Nullable Collection<? extends V> value, MultiMap<K, V> result) {
        if (value != null) {
            result.putValues(key, value);
        }
    }

    public static <K, V> void putIfNotNull(K key, @Nullable V value, MultiMap<K, V> result) {
        if (value != null) {
            result.putValue(key, value);
        }
    }

    
    @Contract(pure = true)
    public static <T> List<T> createMaybeSingletonList(@Nullable T element) {
        return element == null ? Collections.emptyList() : Collections.singletonList(element);
    }

    
    @Contract(pure = true)
    public static <T> Set<T> createMaybeSingletonSet(@Nullable T element) {
        return element == null ? Collections.emptySet() : Collections.singleton(element);
    }

    
    public static <T, V> V getOrCreate(Map<T, V> result, T key, V defaultValue) {
        V value = result.get(key);
        if (value == null) {
            result.put(key, value = defaultValue);
        }
        return value;
    }

    public static <T, V> V getOrCreate(Map<T, V> result, T key, Supplier<V> factory) {
        V value = result.get(key);
        if (value == null) {
            result.put(key, value = factory.get());
        }
        return value;
    }

    
    @Contract(pure = true)
    public static <T, V> V getOrElse(Map<T, V> result, T key, V defValue) {
        V value = result.get(key);
        return value == null ? defValue : value;
    }

    @Contract(pure = true)
    public static <T> boolean and(T[] iterable, Predicate<? super T> condition) {
        return and(Arrays.asList(iterable), condition);
    }

    @Contract(pure = true)
    public static <T> boolean and(Iterable<? extends T> iterable, Predicate<? super T> condition) {
        for (T t : iterable) {
            if (!condition.test(t)) {
                return false;
            }
        }
        return true;
    }

    @Contract(pure = true)
    public static <T> boolean exists(T[] iterable, Predicate<? super T> condition) {
        return or(Arrays.asList(iterable), condition);
    }

    @Contract(pure = true)
    public static <T> boolean exists(Iterable<? extends T> iterable, Predicate<? super T> condition) {
        return or(iterable, condition);
    }

    @Contract(pure = true)
    public static <T> boolean or(T[] iterable, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.or(iterable, condition);
    }

    @Contract(pure = true)
    public static <T> boolean or(Iterable<? extends T> iterable, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.or(iterable, condition);
    }

    @Contract(pure = true)
    public static <T> int count(Iterable<T> iterable, Predicate<? super T> condition) {
        int count = 0;
        for (T t : iterable) {
            if (condition.test(t)) {
                count++;
            }
        }
        return count;
    }

    
    @Contract(pure = true)
    public static <T> List<T> unfold(@Nullable T t, Function<T, T> next) {
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

    
    @Contract(pure = true)
    public static <T> List<T> dropTail(List<T> items) {
        return items.subList(0, items.size() - 1);
    }

    
    public static <T> List<T> dropWhile(Iterable<T> target, Predicate<T> predicate) {
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

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }

    public static <T> void quickSort(List<T> list, Comparator<? super T> comparator) {
        Lists.quickSort(list, comparator);
    }

    /**
     * Merge sorted points, which are sorted by x and with equal x by y.
     * Result is put to x1 y1.
     */
    public static void mergeSortedArrays(
        TIntArrayList x1,
        TIntArrayList y1,
        TIntArrayList x2,
        TIntArrayList y2
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
    
    @Contract(pure = true)
    public static <E> List<E> flatten(Collection<E>[] collections) {
        return flatten(Arrays.asList(collections));
    }

    /**
     * Processes the list, remove all duplicates and return the list with unique elements.
     *
     * @param list must be sorted (according to the comparator), all elements must be not-null
     */
    
    public static <T> List<T> removeDuplicatesFromSorted(List<T> list, Comparator<? super T> comparator) {
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
    
    @Contract(pure = true)
    public static <E> List<E> flatten(Iterable<? extends Collection<? extends E>> collections) {
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
    
    @Contract(pure = true)
    public static <E> List<E> flattenIterables(Iterable<? extends Iterable<E>> collections) {
        List<E> result = new ArrayList<>();
        for (Iterable<E> list : collections) {
            for (E e : list) {
                result.add(e);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    
    public static <K, V> V[] convert(K[] from, V[] to, Function<K, V> fun) {
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
    public static <T> boolean containsIdentity(Iterable<T> list, T element) {
        for (T t : list) {
            if (t == element) {
                return true;
            }
        }
        return false;
    }

    @Contract(pure = true)
    public static <T> int indexOfIdentity(List<? extends T> list, T element) {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            if (list.get(i) == element) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> boolean equalsIdentity(List<T> list1, List<T> list2) {
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
    public static <T> int indexOf(List<? extends T> list, Predicate<? super T> condition) {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            T t = list.get(i);
            if (condition.test(t)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T> int lastIndexOf(List<T> list, Predicate<? super T> condition) {
        for (int i = list.size() - 1; i >= 0; i--) {
            T t = list.get(i);
            if (condition.test(t)) {
                return i;
            }
        }
        return -1;
    }

    @Contract(pure = true)
    public static <T, U extends T> @Nullable U findLastInstance(List<T> list, Class<U> clazz) {
        int i = lastIndexOf(list, clazz::isInstance);
        //noinspection unchecked
        return i < 0 ? null : (U)list.get(i);
    }

    @Contract(pure = true)
    public static <T, U extends T> int lastIndexOfInstance(List<T> list, Class<U> clazz) {
        return lastIndexOf(list, clazz::isInstance);
    }

    @Contract(pure = true)
    public static <T> int indexOf(List<? extends T> list, T object) {
        return indexOf(list, t -> t.equals(object));
    }

    
    @Contract(pure = true)
    public static <A, B> Map<B, A> reverseMap(Map<A, B> map) {
        Map<B, A> result = newHashMap();
        for (Map.Entry<A, B> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    @Contract(pure = true)
    public static <T> boolean processRecursively(T root, BiPredicate<T, List<T>> processor) {
        LinkedList<T> list = new LinkedList<>();
        list.add(root);
        while (!list.isEmpty()) {
            T o = list.removeFirst();
            if (!processor.test(o, list)) {
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

    
    @Contract(pure = true)
    public static <T> Stack<T> newStack() {
        return ContainerUtilRt.newStack();
    }

    
    @Contract(pure = true)
    public static <T> Stack<T> newStack(Collection<T> initial) {
        return ContainerUtilRt.newStack(initial);
    }

    
    @Contract(pure = true)
    @SafeVarargs
    public static <T> Stack<T> newStack(T... initial) {
        return ContainerUtilRt.newStack(initial);
    }

    
    @Contract(pure = true)
    public static <T> List<T> emptyList() {
        return ContainerUtilRt.emptyList();
    }

    
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
    
    @Contract(pure = true)
    public static <T> List<T> createLockFreeCopyOnWriteList() {
        return Lists.newLockFreeCopyOnWriteList();
    }

    
    @Contract(pure = true)
    public static <T> List<T> createLockFreeCopyOnWriteList(Collection<? extends T> c) {
        return Lists.newLockFreeCopyOnWriteList(c);
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectMap() {
        return IntMaps.newConcurrentIntObjectHashMap();
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        return IntMaps.newConcurrentIntObjectHashMap(initialCapacity, loadFactor, concurrencyLevel);
    }

    
    @Contract(pure = true)
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectSoftValueMap() {
        return IntMaps.newConcurrentIntObjectSoftValueHashMap();
    }

    
    @Contract(pure = true)
    public static <V> ConcurrentLongObjectMap<V> createConcurrentLongObjectMap() {
        return LongMaps.newConcurrentLongObjectHashMap();
    }

    
    @Contract(pure = true)
    public static <V> ConcurrentLongObjectMap<V> createConcurrentLongObjectMap(int initialCapacity) {
        return LongMaps.newConcurrentLongObjectHashMap(initialCapacity);
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakValueMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentWeakValueMap();
    }

    
    @Contract(pure = true)
    public static <V> ConcurrentIntObjectMap<V> createConcurrentIntObjectWeakValueMap() {
        return IntMaps.newConcurrentIntObjectWeakValueHashMap();
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeySoftValueMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<K> hashingStrategy
    ) {
        //noinspection deprecation
        return consulo.util.collection.ContainerUtil.createConcurrentWeakKeySoftValueMap(
            initialCapacity,
            loadFactor,
            concurrencyLevel,
            hashingStrategy
        );
    }

    
    @Contract(value = " -> new", pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftKeySoftValueMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentSoftKeySoftValueMap();
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftKeySoftValueMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<K> hashingStrategy
    ) {
        return consulo.util.collection.ContainerUtil.createConcurrentSoftKeySoftValueMap(
            initialCapacity,
            loadFactor,
            concurrencyLevel,
            hashingStrategy
        );
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeySoftValueMap() {
        return createConcurrentWeakKeySoftValueMap(
            100,
            0.75f,
            Runtime.getRuntime().availableProcessors(),
            ContainerUtil.<K>canonicalStrategy()
        );
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeyWeakValueMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentWeakKeyWeakValueMap();
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakKeyWeakValueMap(HashingStrategy<K> strategy) {
        return consulo.util.collection.ContainerUtil.createConcurrentWeakKeyWeakValueMap(strategy);
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftValueMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentSoftValueMap();
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentSoftMap();
    }

    
    @Contract(value = " -> new", pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap() {
        return consulo.util.collection.ContainerUtil.createConcurrentWeakMap();
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentSoftMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<K> hashingStrategy
    ) {
        //noinspection deprecation
        return consulo.util.collection.ContainerUtil.createConcurrentSoftMap(
            initialCapacity,
            loadFactor,
            concurrencyLevel,
            hashingStrategy
        );
    }

    
    @Contract(pure = true)
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap(
        int initialCapacity,
        float loadFactor,
        int concurrencyLevel,
        HashingStrategy<K> hashingStrategy
    ) {
        //noinspection deprecation
        return consulo.util.collection.ContainerUtil.createConcurrentWeakMap(
            initialCapacity,
            loadFactor,
            concurrencyLevel,
            hashingStrategy
        );
    }

    
    @Contract(pure = true)
    @Deprecated
    public static <K, V> ConcurrentMap<K, V> createConcurrentWeakMap(HashingStrategy<K> hashingStrategy) {
        //noinspection deprecation
        return Maps.newConcurrentWeakHashMap(hashingStrategy);
    }

    /**
     * @see #createLockFreeCopyOnWriteList()
     */
    
    @Contract(pure = true)
    public static <T> consulo.util.collection.ConcurrentList<T> createConcurrentList() {
        return Lists.newLockFreeCopyOnWriteList();
    }

    
    @Contract(pure = true)
    public static <T> consulo.util.collection.ConcurrentList<T> createConcurrentList(Collection<? extends T> collection) {
        return Lists.newLockFreeCopyOnWriteList(collection);
    }

    /**
     * @see #addIfNotNull(Collection, Object)
     */
    @Deprecated
    public static <T> void addIfNotNull(@Nullable T element, Collection<T> result) {
        consulo.util.collection.ContainerUtil.addIfNotNull(result, element);
    }

    public static <T> void addIfNotNull(Collection<T> result, @Nullable T element) {
        consulo.util.collection.ContainerUtil.addIfNotNull(result, element);
    }

    
    @Contract(pure = true)
    public static <T, V> List<V> map2List(T[] array, Function<T, V> mapper) {
        return ContainerUtilRt.map2List(array, mapper);
    }

    
    @Contract(pure = true)
    public static <T, V> List<V> map2List(Collection<? extends T> collection, Function<T, V> mapper) {
        return ContainerUtilRt.map2List(collection, mapper);
    }

    
    @Contract(pure = true)
    public static <T, V> Set<V> map2Set(T[] collection, Function<T, V> mapper) {
        return ContainerUtilRt.map2Set(collection, mapper);
    }

    
    @Contract(pure = true)
    public static <T, V> Set<V> map2Set(Collection<? extends T> collection, Function<T, V> mapper) {
        return ContainerUtilRt.map2Set(collection, mapper);
    }

    
    @Contract(pure = true)
    public static <T, V> Set<V> map2LinkedSet(Collection<? extends T> collection, Function<T, V> mapper) {
        if (collection.isEmpty()) {
            return Collections.emptySet();
        }
        Set<V> set = new LinkedHashSet<>(collection.size());
        for (T t : collection) {
            set.add(mapper.apply(t));
        }
        return set;
    }

    
    @Contract(pure = true)
    public static <T, V> Set<V> map2SetNotNull(Collection<? extends T> collection, Function<T, V> mapper) {
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

    
    @Contract(pure = true)
    public static <T> T[] toArray(List<T> collection, T[] array) {
        return ContainerUtilRt.toArray(collection, array);
    }

    
    @Contract(pure = true)
    public static <T> T[] toArray(Collection<T> c, T[] sample) {
        return ContainerUtilRt.toArray(c, sample);
    }

    
    public static <T> T[] copyAndClear(Collection<T> collection, ArrayFactory<T> factory, boolean clear) {
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

    
    @Contract(pure = true)
    public static <T> Collection<T> toCollection(Iterable<T> iterable) {
        return iterable instanceof Collection ? (Collection<T>)iterable : newArrayList(iterable);
    }

    
    public static <T> List<T> toList(Enumeration<T> enumeration) {
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

    
    @Contract(pure = true)
    public static <T> List<T> notNullize(@Nullable List<T> list) {
        return Lists.notNullize(list);
    }

    
    @Contract(pure = true)
    public static <K, V> Map<K, V> notNullize(@Nullable Map<K, V> map) {
        return Maps.notNullize(map);
    }

    
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

    
    @Contract(pure = true)
    public static <T> Set<T> notNullize(@Nullable Set<T> set) {
        return Sets.notNullize(set);
    }

    @Contract(pure = true)
    public static <T, C extends Collection<T>> @Nullable C nullize(@Nullable C collection) {
        return isEmpty(collection) ? null : collection;
    }

    @Contract(pure = true)
    public static <T extends Comparable<T>> int compareLexicographically(List<T> o1, List<T> o2) {
        for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
            int result = Comparing.compare(o1.get(i), o2.get(i));
            if (result != 0) {
                return result;
            }
        }
        return o1.size() < o2.size() ? -1 : o1.size() == o2.size() ? 0 : 1;
    }

    @Contract(pure = true)
    public static <T> int compareLexicographically(List<T> o1, List<T> o2, Comparator<T> comparator) {
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
    
    @Contract(pure = true)
    public static String toString(Map<?, ?> map) {
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

        public KeyOrderedMultiMap(MultiMap<? extends K, ? extends V> toCopy) {
            super(toCopy);
        }

        
        public NavigableSet<K> navigableKeySet() {
            //noinspection unchecked
            return ((TreeMap)myMap).navigableKeySet();
        }
    }

    
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
    
    public static <K, V> Map<K, V> createWeakValueMap() {
        return consulo.util.collection.ContainerUtil.createWeakValueMap();
    }

    /**
     * Soft keys hard values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    
    @Deprecated
    public static <K, V> Map<K, V> createSoftMap() {
        return consulo.util.collection.ContainerUtil.createSoftMap();
    }

    @Contract(value = "_ -> new", pure = true)
    
    @Deprecated
    public static <K, V> Map<K, V> createSoftMap(HashingStrategy<? super K> strategy) {
        return consulo.util.collection.ContainerUtil.createSoftMap(strategy);
    }

    /**
     * Hard keys soft values hash map.
     * Null keys are NOT allowed
     * Null values are allowed
     */
    @Contract(value = " -> new", pure = true)
    
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
    
    public static <K, V> Map<K, V> createWeakMap() {
        return Maps.newWeakHashMap();
    }

    @Contract(value = "_ -> new", pure = true)
    
    public static <K, V> Map<K, V> createWeakMap(int initialCapacity) {
        return Maps.newWeakHashMap(initialCapacity);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    
    @Deprecated
    public static <K, V> Map<K, V> createWeakMap(int initialCapacity, float loadFactor, HashingStrategy<? super K> strategy) {
        return Maps.newWeakHashMap(initialCapacity, loadFactor, strategy);
    }

    @Contract(value = " -> new", pure = true)
    
    @Deprecated
    public static <K, V> Map<K, V> createWeakKeyWeakValueMap() {
        return consulo.util.collection.ContainerUtil.createWeakKeyWeakValueMap();
    }

    public static <T> boolean all(T[] collection, Predicate<? super T> condition) {
        return consulo.util.collection.ContainerUtil.all(collection, condition);
    }

    public static <T> boolean all(Collection<? extends T> collection, Predicate<? super T> condition) {
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
