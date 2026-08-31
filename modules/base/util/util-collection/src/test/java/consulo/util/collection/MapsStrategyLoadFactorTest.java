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

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests: the legacy strategy-map API defaults to a load factor of {@code 1}, which the trove
 * backend accepted but fastutil rejects ({@code 0 < loadFactor < 1}). Construction must not throw.
 *
 * @author VISTALL
 */
public class MapsStrategyLoadFactorTest {
    @Test
    public void newHashMapWithCapacityAndStrategy() {
        Map<String, String> map = Maps.newHashMap(16, HashingStrategy.canonical());
        map.put("a", "1");
        assertEquals("1", map.get("a"));
    }

    @Test
    public void newHashMapWithExplicitLoadFactorOne() {
        Map<String, String> map = Maps.newHashMap(16, 1f, HashingStrategy.canonical());
        map.put("a", "1");
        assertEquals("1", map.get("a"));
    }

    @Test
    public void newHashMapFromInnerMap() {
        Map<String, String> map = Maps.newHashMap(Map.of("k", "v"), HashingStrategy.canonical());
        assertEquals("v", map.get("k"));
    }

    @Test
    public void newWeakHashMapWithLoadFactorOne() {
        Map<String, String> map = Maps.newWeakHashMap(16, 1f, HashingStrategy.canonical());
        map.put("a", "1");
        assertEquals("1", map.get("a"));
    }

    @Test
    public void newHashSetWithCapacityAndStrategy() {
        Set<String> set = Sets.newHashSet(16, HashingStrategy.canonical());
        set.add("x");
        assertTrue(set.contains("x"));
    }
}
