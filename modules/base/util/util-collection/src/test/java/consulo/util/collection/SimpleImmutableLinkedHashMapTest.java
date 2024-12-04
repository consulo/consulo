/*
 * Copyright 2013-2024 consulo.io
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

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author UNV
 * @since 2024-12-04
 */
public class SimpleImmutableLinkedHashMapTest {
    @Test
    public void testEmpty() {
        SimpleImmutableLinkedHashMap<Object, Object> empty = SimpleImmutableLinkedHashMap.empty();
        assertThat(empty)
            .doesNotContainKey("foo")
            .hasSize(0)
            .isEmpty();

        assertThat(empty)
            .extractingByKey("foo").isNull();

        Map<String, String> map = Map.of("k", "v");
        assertThat(empty.withAll(map))
            .isEqualTo(map);

        assertThat(SimpleImmutableLinkedHashMap.empty(HashingStrategy.identity()))
            .isEmpty();
    }

    @Test
    public void testNull() {
        SimpleImmutableLinkedHashMap<Object, Object> empty = SimpleImmutableLinkedHashMap.empty();

        assertThat(empty.containsKey(null))
            .isFalse();

        assertThatThrownBy(() -> empty.withAll(Collections.singletonMap(null, null)))
            .isInstanceOf(IllegalArgumentException.class);

        Map<Integer, String> nullMap = new HashMap<>();
        nullMap.put(0, "0");
        nullMap.put(null, null);

        for (int size : new int[]{1, 2, 5, 10}) {
            SimpleImmutableLinkedHashMap<Integer, String> map = create(size);
            assertThatThrownBy(() -> map.withAll(nullMap))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPut() {
        assertThatThrownBy(() -> SimpleImmutableLinkedHashMap.empty().put("foo", "bar"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testRemove() {
        assertThatThrownBy(() -> SimpleImmutableLinkedHashMap.empty().remove("foo"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPutAll() {
        //noinspection RedundantCollectionOperation
        assertThatThrownBy(() -> SimpleImmutableLinkedHashMap.empty().putAll(Collections.singletonMap("foo", "bar")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testClear() {
        assertThatThrownBy(() -> SimpleImmutableLinkedHashMap.empty().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testWith() {
        SimpleImmutableLinkedHashMap<Integer, String> map = SimpleImmutableLinkedHashMap.empty();
        for (int i = 0; i < 5; i++) {
            String value = String.valueOf(i);
            map = map.with(i, value);
            assertThat(map)
                .hasSize(i + 1)
                .containsKey(i)
                .containsValue(value)
                .doesNotContainValue(String.valueOf(i + 1))
                .extractingByKey(i).isEqualTo(value);
            assertThat(map)
                .extractingByKey(i + 1).isNull();

            SimpleImmutableLinkedHashMap<Integer, String> map1 = map.with(i, value);
            assertThat(map)
                .isSameAs(map1);

            SimpleImmutableLinkedHashMap<Integer, String> map2 = map.with(i, String.valueOf(i + 1));
            assertThat(map)
                .isNotSameAs(map2)
                .hasSameSizeAs(map)
                .containsOnlyKeys(map.keySet());
            assertThat(map2)
                .containsValue(String.valueOf(i + 1))
                .doesNotContainValue(value);
        }
    }

    @Test
    public void testWithNonMain() {
        SimpleImmutableLinkedHashMap<Integer, String> map = create(5);
        SimpleImmutableLinkedHashMap<Integer, String> superMap = map.with(5, "5");
        SimpleImmutableLinkedHashMap<Integer, String> subMap = superMap.without(5);

        assertThat(subMap.with(4, map.get(4)))
            .isSameAs(subMap);

        assertThat(subMap.with(3, "3"))
            .isNotSameAs(subMap)
            .isEqualTo(subMap)
            .hasToString("{0=0, 1=1, 2=2, 4=4, 3=3}");

        assertThat(subMap.with(4, "4"))
            .isNotSameAs(subMap)
            .isEqualTo(subMap);

        assertThat(subMap.with(5, "5"))
            .isNotSameAs(superMap)
            .isEqualTo(superMap);

        assertThat(subMap.with(6, "6"))
            .isNotEqualTo(superMap);
    }

    @Test
    public void testWithAll() {
        for (int size : new int[]{0, 1, 2, 5, 10}) {
            SimpleImmutableLinkedHashMap<Integer, String> map = create(size);
            assertThat(map.get(null)).isNull();

            SimpleImmutableLinkedHashMap<Integer, String> map2 = map.withAll(Map.of());
            assertThat(map2)
                .isSameAs(map);

            int k1 = size + 1;
            String v1 = String.valueOf(k1);
            map2 = map.withAll(Collections.singletonMap(k1, v1));
            assertThat(map2)
                .hasSize(size + 1)
                .containsAllEntriesOf(map2)
                .extractingByKey(k1).isEqualTo(v1);

            int k2 = size + 2;
            String v2 = String.valueOf(k2);
            map2 = map.withAll(Map.of(k1, v1, k2, v2));
            assertThat(map2)
                .hasSize(size + 2)
                .containsAllEntriesOf(map2)
                .extractingByKey(k1).isEqualTo(v1);
            assertThat(map2)
                .extractingByKey(k2).isEqualTo(v2);
        }
    }

    @Test
    public void testWithAllDuplicated() {
        for (int size : new int[]{1, 2, 5, 10}) {
            SimpleImmutableLinkedHashMap<Integer, String> map = create(size);

            int k0 = 0;
            String v0 = map.get(0);

            SimpleImmutableLinkedHashMap<Integer, String> map2 = map.withAll(Map.of(k0, v0));
            assertThat(map2)
                .isSameAs(map);

            int k1 = size + 1;
            String v1 = String.valueOf(k1);
            map2 = map.withAll(Map.of(k0, v0, k1, v1));
            assertThat(map2)
                .hasSize(size + 1)
                .isEqualTo(map2)
                .extractingByKey(k0).isEqualTo(v0);
            assertThat(map2)
                .extractingByKey(k1).isEqualTo(v1);
        }
    }

    @Test
    public void testWithAllDifferentValue() {
        SimpleImmutableLinkedHashMap<Integer, String> map = create(5);

        assertThat(map.withAll(Map.of(0, map.get(0), 1, map.get(1))))
            .isNotSameAs(map);

        LinkedHashMap<Integer, String> dupKeyMap = new LinkedHashMap<>();
        dupKeyMap.put(0, map.get(0));
        dupKeyMap.put(1, "2");

        assertThat(map.withAll(dupKeyMap))
            .hasSameSizeAs(map)
            .isNotSameAs(map)
            .hasToString("{2=2, 3=3, 4=4, 0=0, 1=2}");
    }

    @Test
    public void testWithAllNullable() {
        SimpleImmutableLinkedHashMap<Integer, String> map = create(5);

        LinkedHashMap<Integer, String> mapWithNull = new LinkedHashMap<>();
        mapWithNull.put(5, "5");
        mapWithNull.put(null, null);

        assertThatThrownBy(() -> map.withAll(mapWithNull))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Null keys are not supported");
    }

    @Test
    public void testWithout() {
        SimpleImmutableLinkedHashMap<Integer, String> empty = SimpleImmutableLinkedHashMap.empty();
        assertThat(empty.without(1))
            .isSameAs(empty);

        int size = 11;
        SimpleImmutableLinkedHashMap<Integer, String> map = create(size);
        for (int i = 0; i < size; i++) {
            map = map.without(i);
            assertThat(map)
                .hasSize(size - 1 - i)
                .doesNotContainKey(i);
            assertThat(i == size - 1 || map.containsKey(i + 1)).isTrue();
        }

        map = create(size);
        for (int k = size >> 1, i = k; i < size; i++) {
            map = map.without(i);
            assertThat(map)
                .hasSize(size - 1 - i + k)
                .doesNotContainKey(i);
            assertThat(i == size - 1 || map.containsKey(i + 1)).isTrue();
        }

        map = create(size);
        for (int i = size; i >= 0; i--) {
            map = map.without(i);
            assertThat(map)
                .hasSize(i)
                .doesNotContainKey(i);
            assertThat(i == 0 || map.containsKey(i - 1)).isTrue();
        }
    }

    @Test
    public void testAddCollisions() {
        SimpleImmutableLinkedHashMap<Long, String> map = SimpleImmutableLinkedHashMap.empty();
        for (int i = 0; i < 50; i++) {
            long key = ((long)i << 32) | i ^ 135;
            map = map.with(key, String.valueOf(key));
            assertThat(map)
                .hasSize(i + 1)
                .containsKey(key)
                .containsValue(String.valueOf(key))
                .doesNotContainValue(String.valueOf(key + 1))
                .extractingByKey(key).isEqualTo(String.valueOf(key));
            assertThat(map)
                .extractingByKey(key + 1).isNull();
        }
    }

    @Test
    public void testGet() {
        SimpleImmutableLinkedHashMap<Integer, String> map = create(10);
        assertThat(map.get(null))
            .isNull();
        assertThat(map.get(0))
            .isEqualTo("0");

        map = map.without(0);
        assertThat(map.get(0))
            .isNull();
        assertThat(map.get(1))
            .isEqualTo("1");
    }

    @Test
    public void testIterate() {
        for (int size : new int[]{0, 1, 2, 5, 10}) {
            SimpleImmutableLinkedHashMap<Integer, String> map = create(size);

            assertThat(map.keySet())
                .isEqualTo(IntStream.range(0, size).boxed().collect(Collectors.toSet()));

            assertThat(new HashSet<>(map.values()))
                .isEqualTo(IntStream.range(0, size).mapToObj(String::valueOf).collect(Collectors.toSet()));

            assertThat(
                map.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()))
                    .collect(Collectors.toSet())
            ).isEqualTo(
                IntStream.range(0, size)
                    .mapToObj(i -> new AbstractMap.SimpleImmutableEntry<>(i, String.valueOf(i)))
                    .collect(Collectors.toSet())
            );
        }
    }

    @Test
    public void testEntry() {
        Map.Entry<Integer, String> entry = create(1).entrySet().iterator().next();

        assertThat(entry)
            .hasToString("0=0");

        assertThatThrownBy(() -> entry.setValue(""))
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(entry::hashCode)
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> entry.equals(entry))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testIterateEmpty() {
        SimpleImmutableLinkedHashMap<Object, Object> empty = SimpleImmutableLinkedHashMap.empty();
        assertThatThrownBy(() -> empty.keySet().iterator().next())
            .isInstanceOf(NoSuchElementException.class);

        assertThatThrownBy(() -> empty.values().iterator().next())
            .isInstanceOf(NoSuchElementException.class);

        assertThatThrownBy(() -> empty.entrySet().iterator().next())
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testValues() {
        SimpleImmutableLinkedHashMap<Integer, String> map = create(10);
        assertThat(map.values().contains("9"))
            .isTrue();
        assertThat(map.values().contains("11"))
            .isFalse();
    }

    @Test
    public void testForEach() {
        for (int size : new int[]{0, 1, 2, 5, 10}) {
            SimpleImmutableLinkedHashMap<Integer, String> map = create(size);
            Set<Integer> keys1 = new HashSet<>();
            Set<String> values1 = new HashSet<>();
            map.keySet().forEach(keys1::add);
            map.values().forEach(values1::add);

            Set<Integer> keys2 = new HashSet<>();
            Set<String> values2 = new HashSet<>();
            map.forEach((k, v) -> {
                keys2.add(k);
                values2.add(v);
            });

            assertThat(keys1)
                .isEqualTo(IntStream.range(0, size).boxed().collect(Collectors.toSet()))
                .containsExactlyElementsOf(keys2);

            assertThat(values1)
                .isEqualTo(IntStream.range(0, size).mapToObj(String::valueOf).collect(Collectors.toSet()))
                .containsExactlyElementsOf(values2);
        }
    }

    @Test
    public void testToString() {
        for (int size : new int[]{0, 1, 2, 5, 10}) {
            SimpleImmutableLinkedHashMap<Integer, String> map = create(size);
            String actual = map.toString();
            assertThat(actual)
                .startsWith("{")
                .endsWith("}");
            String content = actual.substring(1, actual.length() - 1);
            if (size == 0) {
                assertThat(content).isEmpty();
                continue;
            }

            Set<String> parts = Set.of(content.split(", ", -1));
            assertThat(parts)
                .hasSize(size)
                .isEqualTo(IntStream.range(0, size).mapToObj(i -> i + "=" + i).collect(Collectors.toSet()));
        }
    }

    @Test
    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    public void testEquals() {
        for (int size : new int[]{0, 1, 2, 5, 10}) {
            SimpleImmutableLinkedHashMap<Integer, String> map = create(size);
            SimpleImmutableLinkedHashMap<Integer, String> map1 = map.with(size - 1, String.valueOf(size));
            HashMap<Integer, String> hashMap = new HashMap<>(map);

            assertThat(map)
                .isEqualTo(map)
                .isNotEqualTo(map1)
                .isEqualTo(hashMap)
                .isNotEqualTo(new Object());

            assertThat(hashMap)
                .isEqualTo(map)
                .isNotEqualTo(map1);

            assertThat(map1)
                .isNotEqualTo(map)
                .isNotEqualTo(hashMap);
        }
    }

    @Test
    public void testHashCode() {
        for (int size : new int[]{0, 1, 2, 5, 10}) {
            SimpleImmutableLinkedHashMap<Integer, String> map = create(size);
            HashMap<Integer, String> hashMap = new HashMap<>(map);
            assertThat(hashMap.hashCode()).isEqualTo(map.hashCode());
        }
    }

    @Test
    public void testFromMap() {
        SimpleImmutableLinkedHashMap<Integer, String> map = create(10);
        assertThat(map)
            .isSameAs(SimpleImmutableLinkedHashMap.fromMap(map));

        assertThat(SimpleImmutableLinkedHashMap.fromMap(Map.of()))
            .isSameAs(SimpleImmutableLinkedHashMap.empty());
    }

    @Test
    public void testReversed() {
        SimpleImmutableLinkedHashMap<Integer, String> map = create(3);

        assertThat(map)
            .hasToString("{0=0, 1=1, 2=2}");

        assertThat(map.reversed())
            .hasSameSizeAs(map)
            .isNotSameAs(map)
            .hasToString("{2=2, 1=1, 0=0}");

        assertThat(map.without(2).reversed())
            .hasToString("{1=1, 0=0}");

        assertThat(map.sequencedKeySet().reversed())
            .hasToString("[2, 1, 0]");

        assertThat(map.sequencedValues().reversed())
            .hasToString("[2, 1, 0]");

        assertThat(map.sequencedEntrySet().reversed())
            .hasToString("[2=2, 1=1, 0=0]");
    }

    private static SimpleImmutableLinkedHashMap<Integer, String> create(int size) {
        Map<Integer, String> srcMap = IntStream.range(0, size)
            .mapToObj(i -> new AbstractMap.SimpleImmutableEntry<>(i, String.valueOf(i)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        SimpleImmutableLinkedHashMap<Integer, String> map = SimpleImmutableLinkedHashMap.fromMap(srcMap);
        return map;
    }
}
