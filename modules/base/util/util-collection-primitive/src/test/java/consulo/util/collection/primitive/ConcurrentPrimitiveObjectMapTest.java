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
package consulo.util.collection.primitive;

import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.longs.ConcurrentLongObjectMap;
import consulo.util.collection.primitive.longs.LongMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the fastutil-backed concurrent primitive-object maps.
 *
 * @author VISTALL
 */
public class ConcurrentPrimitiveObjectMapTest {
    @Test
    public void testIntPutGetRemove() {
        ConcurrentIntObjectMap<String> map = IntMaps.newConcurrentIntObjectHashMap();
        assertTrue(map.isEmpty());
        assertNull(map.put(1, "a"));
        assertEquals("a", map.put(1, "b"));
        assertEquals("b", map.get(1));
        assertTrue(map.containsKey(1));
        assertFalse(map.containsKey(2));
        assertEquals(1, map.size());
        assertEquals("b", map.remove(1));
        assertNull(map.get(1));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testIntCacheOrGet() {
        ConcurrentIntObjectMap<String> map = IntMaps.newConcurrentIntObjectHashMap();
        assertEquals("first", map.cacheOrGet(1, "first"));
        assertEquals("first", map.cacheOrGet(1, "second"));
        assertEquals("first", map.get(1));
    }

    @Test
    public void testIntAtomicRemoveAndReplace() {
        ConcurrentIntObjectMap<String> map = IntMaps.newConcurrentIntObjectHashMap();
        map.put(1, "a");
        assertFalse(map.remove(1, "x"));
        assertTrue(map.containsKey(1));
        assertTrue(map.remove(1, "a"));
        assertFalse(map.containsKey(1));

        map.put(2, "a");
        assertFalse(map.replace(2, "x", "b"));
        assertEquals("a", map.get(2));
        assertTrue(map.replace(2, "a", "b"));
        assertEquals("b", map.get(2));
    }

    @Test
    public void testIntEntrySetAndKeys() {
        ConcurrentIntObjectMap<String> map = IntMaps.newConcurrentIntObjectHashMap();
        map.put(10, "x");
        map.put(20, "y");
        map.put(30, "z");

        Set<Integer> keysFromEntries = new HashSet<>();
        for (Int2ObjectMap.Entry<String> entry : map.int2ObjectEntrySet()) {
            keysFromEntries.add(entry.getIntKey());
            assertEquals(map.get(entry.getIntKey()), entry.getValue());
        }
        assertEquals(Set.of(10, 20, 30), keysFromEntries);
        assertEquals(3, map.keySet().size());
        assertTrue(map.values().contains("y"));
    }

    @Test
    public void testIntConcurrentStress() throws Exception {
        ConcurrentIntObjectMap<Integer> map = IntMaps.newConcurrentIntObjectHashMap();
        int threads = 8;
        int perThread = 5000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger errors = new AtomicInteger();
        for (int t = 0; t < threads; t++) {
            int base = t * perThread;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        int key = base + i;
                        map.put(key, Integer.valueOf(key));
                        Integer got = map.get(key);
                        if (got == null || got.intValue() != key) {
                            errors.incrementAndGet();
                        }
                    }
                }
                catch (Throwable e) {
                    errors.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        assertEquals(0, errors.get());
        assertEquals(threads * perThread, map.size());
    }

    @Test
    public void testIntWeakValueMapBasic() {
        ConcurrentIntObjectMap<Object> map = IntMaps.newConcurrentIntObjectWeakValueHashMap();
        Object value = new Object();
        map.put(1, value);
        assertEquals(value, map.get(1));
        assertEquals(value, map.remove(1));
        assertNull(map.get(1));
    }

    @Test
    public void testLongPutGetRemove() {
        ConcurrentLongObjectMap<String> map = LongMaps.newConcurrentLongObjectHashMap();
        assertNull(map.put(1L, "a"));
        assertEquals("a", map.get(1L));
        assertEquals("a", map.cacheOrGet(1L, "b"));
        assertTrue(map.containsKey(1L));
        assertEquals("a", map.remove(1L));
        assertNull(map.get(1L));
    }

    @Test
    public void testLongEntrySetAndReplace() {
        ConcurrentLongObjectMap<String> map = LongMaps.newConcurrentLongObjectHashMap();
        map.put(100L, "x");
        map.put(200L, "y");

        Set<Long> keys = new HashSet<>();
        for (Long2ObjectMap.Entry<String> entry : map.long2ObjectEntrySet()) {
            keys.add(entry.getLongKey());
        }
        assertEquals(Set.of(100L, 200L), keys);

        assertTrue(map.replace(100L, "x", "z"));
        assertEquals("z", map.get(100L));
        assertTrue(map.remove(200L, "y"));
        assertFalse(map.containsKey(200L));
    }
}
