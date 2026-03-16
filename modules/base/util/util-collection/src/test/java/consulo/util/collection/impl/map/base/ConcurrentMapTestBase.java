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
package consulo.util.collection.impl.map.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author UNV
 * @since 2026-03-15
 */
public abstract class ConcurrentMapTestBase {
    protected static final Object ABSENT_KEY = new Object();
    protected static final Object[] KEYS = new Object[]{new Object(), new Object(), new Object(), new Object(), new Object()};
    protected static final String[] VALUES = new String[]{"A", "B", "C", "D", "E"};
    protected static final Map<Object, String> MAP5 = Map.of(
        KEYS[0], VALUES[0],
        KEYS[1], VALUES[1],
        KEYS[2], VALUES[2],
        KEYS[3], VALUES[3],
        KEYS[4], VALUES[4]
    );

    protected boolean nullKeysProhibited() {
        return true;
    }

    protected boolean nullValuesProhibited() {
        return true;
    }

    protected boolean containsKeySupported() {
        return true;
    }

    protected boolean containsValueSupported() {
        return true;
    }

    protected boolean keySetSupported() {
        return true;
    }

    protected boolean entrySetSupported() {
        return true;
    }

    protected boolean equalsAndHashCodeSupported() {
        return true;
    }

    /**
     * Returns a new empty map.
     */
    protected abstract <K, V> ConcurrentMap<K, V> emptyMap();

    /**
     * Returns a new map from KEYS[0..4] to Strings "A".."E".
     */
    protected ConcurrentMap<Object, String> map5() {
        ConcurrentMap<Object, String> map = emptyMap();
        assertThat(map).isEmpty();
        map.putAll(MAP5);
        assertThat(map)
            .isNotEmpty()
            .hasSize(5);
        return map;
    }

    // classes for testing Comparable fallbacks
    static class BI implements Comparable<BI> {
        private final int value;

        BI(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(BI other) {
            return Integer.compare(value, other.value);
        }

        @Override
        public boolean equals(Object x) {
            return x instanceof BI bi && bi.value == value;
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    static class CI extends BI {
        CI(int value) {
            super(value);
        }
    }

    static class DI extends BI {
        DI(int value) {
            super(value);
        }
    }

    static class BS implements Comparable<BS> {
        private final String value;

        BS(String value) {
            this.value = value;
        }

        @Override
        public int compareTo(BS other) {
            return value.compareTo(other.value);
        }

        @Override
        public boolean equals(Object x) {
            return x instanceof BS bs && value.equals(bs.value);
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    static class LexicographicList<E extends Comparable<E>> extends ArrayList<E> implements Comparable<LexicographicList<E>> {
        LexicographicList(E e) {
            super(Collections.singleton(e));
        }

        @Override
        public int compareTo(LexicographicList<E> other) {
            int common = Math.min(size(), other.size());
            int r = 0;
            for (int i = 0; i < common; i++) {
                r = get(i).compareTo(other.get(i));
                if (r != 0) {
                    break;
                }
            }
            if (r == 0) {
                r = Integer.compare(size(), other.size());
            }
            return r;
        }

        private static final long serialVersionUID = 0;
    }

    static class CollidingObject {
        final String value;

        CollidingObject(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return this.value.hashCode() & 1;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof CollidingObject) && ((CollidingObject) obj).value.equals(value);
        }
    }

    static class ComparableCollidingObject extends CollidingObject implements Comparable<ComparableCollidingObject> {
        ComparableCollidingObject(String value) {
            super(value);
        }

        @Override
        public int compareTo(ComparableCollidingObject o) {
            return value.compareTo(o.value);
        }
    }

    /**
     * Inserted elements that are subclasses of the same Comparable
     * class are found.
     */
    @Test
    public void testComparableFamily() {
        int size = 10;
        ConcurrentMap<BI, Boolean> m = emptyMap();
        for (int i = 0; i < size; i++) {
            assertThat(m.put(new CI(i), true)).isNull();
        }
        for (int i = 0; i < size; i++) {
            assertThat(m.get(new CI(i))).isTrue();
            assertThat(m.get(new DI(i))).isTrue();
        }
    }

    /**
     * Elements of classes with erased generic type parameters based
     * on Comparable can be inserted and found.
     */
    @Test
    public void testGenericComparable() {
        int size = 10;
        ConcurrentMap<Object, Boolean> m = emptyMap();
        for (int i = 0; i < size; i++) {
            BI bi = new BI(i);
            BS bs = new BS(String.valueOf(i));
            LexicographicList<BI> bis = new LexicographicList<>(bi);
            LexicographicList<BS> bss = new LexicographicList<>(bs);
            assertThat(m.putIfAbsent(bis, true)).isNull();
            assertThat(m.get(bis)).isNotNull();
            if (m.putIfAbsent(bss, true) == null) {
                assertThat(m.get(bss)).isNotNull();
            }
            assertThat(m.get(bis)).isNotNull();
        }
        for (int i = 0; i < size; i++) {
            assertThat(m.get(Collections.singletonList(new BI(i)))).isNotNull();
        }
    }

    /**
     * Elements of non-comparable classes equal to those of classes
     * with erased generic type parameters based on Comparable can be
     * inserted and found.
     */
    //@Test
    public void testGenericComparable2() {
        int size = 10;
        ConcurrentMap<Object, Boolean> m = emptyMap();
        for (int i = 0; i < size; i++) {
            m.put(Collections.singletonList(new BI(i)), true);
        }

        for (int i = 0; i < size; i++) {
            assertThat(m.get(new LexicographicList<>(new BI(i)))).isNotNull();
        }
    }

    /**
     * Mixtures of instances of comparable and non-comparable classes
     * can be inserted and found.
     */
    @Test
    public void testMixedComparable() {
        int size = 10;
        ConcurrentMap<Object, Object> map = emptyMap();
        Random rng = new Random();
        List<Object> testData = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            testData.add(switch (rng.nextInt(4)) {
                case 0 -> new Object();
                case 1 -> new CollidingObject(Integer.toString(i));
                default -> new ComparableCollidingObject(Integer.toString(i));
            });
        }
        for (Object x : testData) {
            assertThat(map.put(x, x)).isNull();
        }
        for (Object k : testData) {
            assertThat(map.get(k)).isSameAs(k);
        }
        assertThat(map).hasSize(size);
        for (Object k : testData) {
            assertThat(map.put(k, k)).isSameAs(k);
        }
    }

    @DisplayName("clear removes all pairs")
    @Test
    public void testClear() {
        ConcurrentMap<Object, String> map = map5();
        map.clear();
        assertThat(map).isEmpty();
    }

    @DisplayName("Maps with same contents are equal")
    @Test
    public void testEquals() {
        ConcurrentMap<Object, String> map = map5();
        assertThat(map).isEqualTo(map);
        if (equalsAndHashCodeSupported()) {
            assertThat(map).isEqualTo(MAP5);
            assertThat(MAP5).isEqualTo(map);
            map.clear();
            assertThat(map).isNotEqualTo(MAP5);
            assertThat(MAP5).isNotEqualTo(map);
        }
        else {
            assertThat(map).isNotEqualTo(MAP5);
        }
    }

    @DisplayName("containsKey returns true for contained key")
    @Test
    public void testContainsKey() {
        if (containsKeySupported()) {
            assertThat(map5())
                .containsKeys(KEYS)
                .doesNotContainKey(ABSENT_KEY);
        }
        else {
            assertThatThrownBy(() -> emptyMap().containsKey("foo")).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @DisplayName("containsValue returns true for held values")
    @Test
    public void testContainsValue() {
        if (containsValueSupported()) {
            assertThat(map5())
                .containsValues(VALUES)
                .doesNotContainValue("Z");
        }
        else {
            assertThatThrownBy(() -> emptyMap().containsValue(ABSENT_KEY)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @DisplayName("get returns the correct element at the given key, or null if not present")
    @SuppressWarnings({"CollectionIncompatibleType", "MismatchedQueryAndUpdateOfCollection"})
    @Test
    public void testGet() {
        ConcurrentMap<Object, String> map = map5();
        assertThat(map.get(KEYS[0])).isEqualTo(VALUES[0]);
        ConcurrentMap<Object, String> empty = emptyMap();
        assertThat(map.get("anything")).isNull();
        assertThat(empty.get("anything")).isNull();
    }

    @DisplayName("isEmpty is true of empty map and false for non-empty")
    @Test
    public void testIsEmpty() {
        assertThat(emptyMap()).isEmpty();
        assertThat(map5()).isNotEmpty();
    }

    @DisplayName("keySet returns a Set containing all the keys")
    @Test
    public void testKeySet() {
        if (keySetSupported()) {
            assertThat(map5().keySet())
                .hasSize(5)
                .contains(KEYS);
        }
        else {
            assertThatThrownBy(() -> emptyMap().keySet()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @DisplayName("Test keySet().removeAll on empty map")
    @Test
    public void testKeySetEmptyRemoveAll() {
        if (keySetSupported()) {
            ConcurrentMap<Object, String> map = emptyMap();
            Set<Object> set = map.keySet();
            set.removeAll(Collections.emptyList());
            assertThat(map).isEmpty();
            assertThat(set).isEmpty();
            // following is test for JDK-8163353
            set.removeAll(Collections.emptySet());
            assertThat(map).isEmpty();
            assertThat(set).isEmpty();
        }
    }

    @DisplayName("keySet.toArray contains all keys")
    @Test
    public void testKeySetToArray() {
        if (keySetSupported()) {
            ConcurrentMap<Object, String> map = map5();
            Set<Object> s = map.keySet();
            Object[] ar = s.toArray();
            assertThat(ar).hasSize(5);
            assertThat(s.containsAll(Arrays.asList(ar))).isTrue();
            ar[0] = ABSENT_KEY;
            assertThat(s.containsAll(Arrays.asList(ar))).isFalse();
        }
    }

    @DisplayName("values.toArray contains all values")
    @Test
    public void testValuesToArray() {
        ConcurrentMap<Object, String> map = map5();
        Collection<String> v = map.values();
        String[] ar = v.toArray(new String[0]);
        assertThat(ar).hasSize(5);
        assertThat(new ArrayList<>(Arrays.asList(ar)))
            .containsExactlyInAnyOrder(VALUES);
    }

    @DisplayName("entrySet.toArray contains all entries")
    @SuppressWarnings("unchecked")
    @Test
    public void testEntrySetToArray() {
        if (entrySetSupported()) {
            ConcurrentMap<Object, String> map = map5();
            Object[] ar = map.entrySet().toArray();
            assertThat(ar).hasSize(5);
            for (int i = 0; i < 5; ++i) {
                Map.Entry<Object, String> entry = (Map.Entry<Object, String>) ar[i];
                assertThat(KEYS).contains(entry.getKey());
                assertThat(VALUES).contains(entry.getValue());
            }
        }
    }

    @DisplayName("values collection contains all values")
    @Test
    public void testValues() {
        assertThat(map5().values())
            .hasSize(5)
            .containsExactlyInAnyOrder(VALUES);
    }

    @DisplayName("entrySet contains all pairs")
    @SuppressWarnings("WhileLoopReplaceableByForEach")
    @Test
    public void testEntrySet() {
        if (entrySetSupported()) {
            Set<Map.Entry<Object, String>> s = map5().entrySet();
            assertThat(s).hasSize(5);
            Iterator<Map.Entry<Object, String>> it = s.iterator();
            while (it.hasNext()) {
                Map.Entry<Object, String> e = it.next();
                assertTrue(
                    (e.getKey().equals(KEYS[0]) && e.getValue().equals(VALUES[0]))
                        || (e.getKey().equals(KEYS[1]) && e.getValue().equals(VALUES[1]))
                        || (e.getKey().equals(KEYS[2]) && e.getValue().equals(VALUES[2]))
                        || (e.getKey().equals(KEYS[3]) && e.getValue().equals(VALUES[3]))
                        || (e.getKey().equals(KEYS[4]) && e.getValue().equals(VALUES[4]))
                );
            }
        }
        else {
            assertThatThrownBy(() -> emptyMap().entrySet()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @DisplayName("putAll adds all key-value pairs from the given map")
    @Test
    public void testPutAll() {
        ConcurrentMap<Object, String> p = emptyMap();
        p.putAll(MAP5);
        assertThat(p).hasSize(5);
        assertThat(p.get(KEYS[0])).isSameAs(VALUES[0]);
        assertThat(p.get(KEYS[1])).isSameAs(VALUES[1]);
        assertThat(p.get(KEYS[2])).isSameAs(VALUES[2]);
        assertThat(p.get(KEYS[3])).isSameAs(VALUES[3]);
        assertThat(p.get(KEYS[4])).isSameAs(VALUES[4]);
    }

    @DisplayName("putIfAbsent works when the given key is not present")
    @Test
    public void testPutIfAbsent() {
        ConcurrentMap<Object, String> map = map5();
        map.putIfAbsent(ABSENT_KEY, "Z");
        assertThat(map.get(ABSENT_KEY)).isNotNull();
    }

    @DisplayName("putIfAbsent does not add the pair if the key is already present")
    @Test
    public void testPutIfAbsent2() {
        ConcurrentMap<Object, String> map = map5();
        assertThat(map.putIfAbsent(KEYS[0], "Z")).isEqualTo("A");
    }

    @DisplayName("replace fails when the given key is not present")
    @Test
    public void testReplace() {
        ConcurrentMap<Object, String> map = map5();
        assertThat(map.replace(ABSENT_KEY, "Z")).isNull();
        assertThat(map.get(ABSENT_KEY)).isNull();
    }

    @DisplayName("replace succeeds if the key is already present")
    @Test
    public void testReplace2() {
        ConcurrentMap<Object, String> map = map5();
        assertThat(map.replace(KEYS[0], "Z")).isEqualTo("A");
        assertThat(map.get(KEYS[0])).isEqualTo("Z");
    }

    @DisplayName("replace value fails when the given key not mapped to expected value")
    @Test
    public void testReplaceValue() {
        ConcurrentMap<Object, String> map = map5();
        assertThat(map.get(KEYS[0])).isEqualTo("A");
        assertThat(map.replace(KEYS[0], "Z", "Z")).isFalse();
        assertThat(map.get(KEYS[0])).isEqualTo("A");
    }

    @DisplayName("replace value succeeds when the given key mapped to expected value")
    @Test
    public void testReplaceValue2() {
        ConcurrentMap<Object, String> map = map5();
        assertThat(map.get(KEYS[0])).isEqualTo("A");
        assertThat(map.replace(KEYS[0], "A", "Z")).isTrue();
        assertThat(map.get(KEYS[0])).isEqualTo("Z");
    }

    @DisplayName("remove removes the correct key-value pair from the map")
    @Test
    public void testRemove() {
        ConcurrentMap<Object, String> map = map5();
        map.remove(KEYS[4]);
        assertThat(map).hasSize(4);
        assertThat(map.get(KEYS[4])).isNull();
    }

    @DisplayName("remove(key, value) removes only if pair present")
    @Test
    public void testRemove2() {
        ConcurrentMap<Object, String> map = map5();
        map.remove(KEYS[4], "E");
        assertThat(map).hasSize(4);
        assertThat(map.get(KEYS[4])).isNull();
        map.remove(KEYS[3], "A");
        assertThat(map).hasSize(4);
        assertThat(map.get(KEYS[3])).isSameAs("D");
    }

    @Test
    @DisplayName("size returns the correct values")
    public void testSize() {
        assertThat(map5()).hasSize(5);
        assertThat(emptyMap()).hasSize(0);
    }

    @DisplayName("toString contains toString of elements")
    @Test
    public void testToString() {
        ConcurrentMap<Object, String> map = map5();
        String s = map.toString();
        for (String value : VALUES) {
            assertThat(s).contains(value);
        }
    }

    @DisplayName("get(null) throws NPE")
    @SuppressWarnings({"ConstantConditions", "NullAway"})
    @Test
    public void testGetNPE() {
        if (nullKeysProhibited()) {
            assertThatThrownBy(() -> map5().get(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("containsKey(null) throws NPE")
    @SuppressWarnings("NullAway")
    @Test
    public void testContainsKeyNPE() {
        if (nullKeysProhibited() && containsKeySupported()) {
            assertThatThrownBy(() -> map5().containsKey(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("containsValue(null) throws NPE")
    @SuppressWarnings("NullAway")
    @Test
    public void testContainsValueNPE() {
        if (nullValuesProhibited() && containsValueSupported()) {
            assertThatThrownBy(() -> emptyMap().containsValue(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("put(null, x) throws NPE")
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "ConstantConditions", "NullAway"})
    @Test
    public void testPut1NPE() {
        if (nullKeysProhibited()) {
            assertThatThrownBy(() -> emptyMap().put(null, "whatever")).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("put(x, null) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testPut2NPE() {
        if (nullValuesProhibited()) {
            assertThatThrownBy(() -> emptyMap().put(ABSENT_KEY, null)).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("putIfAbsent(null, x) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testPutIfAbsent1NPE() {
        if (nullKeysProhibited()) {
            assertThatThrownBy(() -> emptyMap().putIfAbsent(null, "whatever")).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("replace(null, x) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testReplaceNPE() {
        if (nullKeysProhibited()) {
            assertThatThrownBy(() -> emptyMap().replace(null, "whatever")).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("replace(null, x, y) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testReplaceValueNPE() {
        if (nullKeysProhibited()) {
            assertThatThrownBy(() -> emptyMap().replace(null, "A", "B")).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("putIfAbsent(x, null) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testPutIfAbsent2NPE() {
        if (nullValuesProhibited()) {
            assertThatThrownBy(() -> emptyMap().putIfAbsent(ABSENT_KEY, null)).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("replace(x, null) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testReplace2NPE() {
        if (nullValuesProhibited()) {
            assertThatThrownBy(() -> emptyMap().replace(KEYS[0], null)).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("replace(x, null, y) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testReplaceValue2NPE() {
        if (nullValuesProhibited()) {
            assertThatThrownBy(() -> emptyMap().replace(KEYS[0], null, "A")).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("replace(x, y, null) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testReplaceValue3NPE() {
        if (nullValuesProhibited()) {
            assertThatThrownBy(() -> emptyMap().replace(ABSENT_KEY, "A", null)).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("remove(null) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testRemove1NPE() {
        if (nullKeysProhibited()) {
            ConcurrentMap<Object, String> c = map5();
            assertThatThrownBy(() -> c.remove(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("remove(null, x) throws NPE")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testRemove2NPE() {
        if (nullKeysProhibited()) {
            ConcurrentMap<Object, String> c = map5();
            assertThatThrownBy(() -> c.remove(null, "whatever")).isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("remove(x, null) returns false")
    @SuppressWarnings({"ConstantConditions", "MismatchedQueryAndUpdateOfCollection", "NullAway"})
    @Test
    public void testRemove3NPE() {
        if (nullValuesProhibited()) {
            ConcurrentMap<Object, String> c = map5();
            assertThatThrownBy(() -> c.remove(KEYS[0], null)).isInstanceOf(NullPointerException.class);
        }
    }
}
