/*
 * Copyright 2013-2026 consulo.io
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
package consulo.util.collection.impl.map;

import consulo.util.collection.impl.map.base.ConcurrentMapTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author UNV
 * @since 2026-03-14
 */
public class ConcurrentHashMapTest extends ConcurrentMapTestBase {
    /**
     * Returns a new empty map.
     */
    @Override
    protected <K, V> ConcurrentHashMap<K, V> emptyMap() {
        return new ConcurrentHashMap<>(5);
    }

    /**
     * Returns a new map from KEYS[0..4] to Strings "A".."E".
     */
    @Override
    protected ConcurrentHashMap<Object, String> map5() {
        return (ConcurrentHashMap<Object, String>) super.map5();
    }

    @DisplayName("hashCode() equals sum of each key.hashCode ^ value.hashCode")
    @Test
    public void testHashCode() {
        ConcurrentHashMap<Object, String> map = map5();
        int sum = 0;

        for (Map.Entry<Object, String> e : map.entrySet()) {
            sum += ConcurrentHashMap.spread(map.hashCode(e.getKey())) ^ e.getValue().hashCode();
        }
        assertThat(map.hashCode()).isEqualTo(sum);
    }

    @DisplayName("contains returns true for contained value")
    @Test
    public void testContains() {
        ConcurrentHashMap<Object, String> map = map5();
        assertThat(map.contains("A")).isTrue();
        assertThat(map.contains("Z")).isFalse();
    }

    @DisplayName("enumeration returns an enumeration containing the correct elements")
    @Test
    public void testEnumeration() {
        ConcurrentHashMap<Object, String> map = map5();
        Enumeration<String> e = map.elements();
        int count = 0;
        while (e.hasMoreElements()) {
            count++;
            e.nextElement();
        }
        assertThat(count).isEqualTo(5);
    }

    @DisplayName("keys returns an enumeration containing all the keys from the map")
    @Test
    public void testKeys() {
        ConcurrentHashMap<Object, String> map = map5();
        Enumeration<Object> e = map.keys();
        int count = 0;
        while (e.hasMoreElements()) {
            count++;
            e.nextElement();
        }
        assertThat(count).isEqualTo(5);
    }

    @DisplayName("Cannot create with only negative capacity")
    @Test
    public void testConstructor1() {
        assertThatThrownBy(() -> new ConcurrentHashMap<Object, String>(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Constructor (initialCapacity, loadFactor) throws IllegalArgumentException if either argument is negative")
    @Test
    public void testConstructor2() {
        assertThatThrownBy(() -> new ConcurrentHashMap<Object, String>(-1, .75f)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConcurrentHashMap<Object, String>(16, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Constructor (initialCapacity, loadFactor, concurrencyLevel) throws IllegalArgumentException if any argument is negative")
    @Test
    public void testConstructor3() {
        assertThatThrownBy(() -> new ConcurrentHashMap<Object, String>(-1, .75f, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConcurrentHashMap<Object, String>(16, -1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConcurrentHashMap<Object, String>(16, .75f, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("ConcurrentHashMap(map) throws NullPointerException if the given map is null")
    @SuppressWarnings({"unchecked", "ConstantConditions", "NullAway"})
    @Test
    public void testConstructor4() {
        assertThatThrownBy(() -> new ConcurrentHashMap<Object, String>((Map) null)).isInstanceOf(NullPointerException.class);
    }

    @DisplayName("ConcurrentHashMap(map) creates a new map with the same mappings as the given map")
    @Test
    public void testConstructor5() {
        ConcurrentHashMap<Object, String> map1 = map5();
        ConcurrentHashMap<Object, String> map2 = new ConcurrentHashMap<>((Map<Object, String>) map1);
        assertTrue(map2.equals(map1));
        map2.put(KEYS[0], "F");
        assertFalse(map2.equals(map1));
    }

    @DisplayName("contains(null) throws NPE")
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "ConstantConditions", "NullAway"})
    @Test
    public void testContainsNPE() {
        assertThatThrownBy(() -> emptyMap().contains(null)).isInstanceOf(NullPointerException.class);
    }

//    @DisplayName("SetValue of an EntrySet entry sets value in the map.")
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testSetValueWriteThrough() {
//        // Adapted from a bug report by Eric Zoerner
//        ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<>(2, 5.0f, 1);
//        assertTrue(map.isEmpty());
//        for (int i = 0; i < 20; i++)
//            map.put(itemFor(i), itemFor(i));
//        assertFalse(map.isEmpty());
//        Object key = itemFor(16);
//        Map.Entry<Object, Object> entry1 = map.entrySet().iterator().next();
//        // Unless it happens to be first (in which case remainder of
//        // test is skipped), remove a possibly-colliding key from map
//        // which, under some implementations, may cause entry1 to be
//        // cloned in map
//        if (!entry1.getKey().equals(key)) {
//            map.remove(key);
//            entry1.setValue("XYZ");
//            assertTrue(map.containsValue("XYZ")); // fails if write-through broken
//        }
//    }

//    /**
//     * Tests performance of removeAll when the other collection is much smaller.
//     * ant -Djsr166.tckTestClass=ConcurrentHashMapTest -Djsr166.methodFilter=testRemoveAll_performance -Djsr166.expensiveTests=true tck
//     */
//    @Test
//    public void testRemoveAll_performance() {
//        final int mapSize = expensiveTests ? 1_000_000 : 100;
//        final int iterations = expensiveTests ? 500 : 2;
//        final ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<>();
//        for (int i = 0; i < mapSize; i++) {
//            Object I = itemFor(i);
//            map.put(I, I);
//        }
//        Set<Object> keySet = map.keySet();
//        Collection<Object> removeMe = Arrays.asList(new Object[]{minusOne, minusTwo});
//        for (int i = 0; i < iterations; i++)
//            assertFalse(keySet.removeAll(removeMe));
//        mustEqual(mapSize, map.size());
//    }

//    @Test
//    public void testReentrantComputeIfAbsent() {
//        ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<>(16);
//        try {
//            for (int i = 0; i < 100; i++) { // force a resize
//                map.computeIfAbsent(new Object(i), key -> new Object(findValue(map, key)));
//            }
//            fail("recursive computeIfAbsent should throw IllegalStateException");
//        }
//        catch (IllegalStateException success) {
//        }
//    }

//    @Test
//    public void testMapEqualsIfClassCastExceptionOccurs() {
//        class MonotypeKeyMap extends HashMap<Byte, Object> {
//            @Nullable
//            @Override
//            public Object get(Object key) {
//                return super.get((Byte) key); // Force cast, allowed by spec
//            }
//        }
//
//        var mkm = new MonotypeKeyMap();
//        mkm.put((byte) 1, "value1");
//        var similar = new ConcurrentHashMap<Byte, Object>();
//        similar.put((byte) 1, "value1");
//        var different = new ConcurrentHashMap<String, String>();
//        different.put("test1", "value1");
//
//        assertThat(similar.equals(mkm)).isTrue();
//        assertThat(different.equals(mkm)).isFalse();
//    }
}
