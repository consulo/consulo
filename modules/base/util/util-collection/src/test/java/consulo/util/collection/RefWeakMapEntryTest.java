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
package consulo.util.collection;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RefWeakMapEntryTest {
    @Test
    public void entriesSurviveClear() {
        Map<Object, String> map = Maps.newWeakHashMap();
        Object k1 = new Object();
        Object k2 = new Object();
        map.put(k1, "a");
        map.put(k2, "b");

        List<Map.Entry<Object, String>> copy = new ArrayList<>(map.entrySet());
        map.clear();

        Map<Object, String> byKey = new HashMap<>();
        for (Map.Entry<Object, String> entry : copy) {
            byKey.put(entry.getKey(), entry.getValue());
        }

        assertEquals(2, byKey.size());
        assertEquals("a", byKey.get(k1));
        assertEquals("b", byKey.get(k2));
    }

    @Test
    public void weakKeysAreStillCollected() throws InterruptedException {
        Map<Object, String> map = Maps.newWeakHashMap();
        WeakReference<Object> keyRef = putTransientKey(map);
        assertEquals(1, map.size());

        for (int i = 0; i < 200 && keyRef.get() != null; i++) {
            allocatePressure();
            System.gc();
            map.size();
            Thread.sleep(5);
        }

        assertNull(keyRef.get(), "weak key must be collectible after its strong references are gone");
        assertTrue(map.isEmpty(), "entry whose key was collected must be removed");
    }

    private static WeakReference<Object> putTransientKey(Map<Object, String> map) {
        Object key = new Object();
        map.put(key, "value");
        return new WeakReference<>(key);
    }

    private static void allocatePressure() {
        byte[][] junk = new byte[64][];
        for (int i = 0; i < junk.length; i++) {
            junk[i] = new byte[64 * 1024];
        }
    }
}
