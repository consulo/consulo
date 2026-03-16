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

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.map.base.ConcurrentRefMapTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author UNV
 * @since 2026-03-15
 */
public class ConcurrentSoftHashMapTest extends ConcurrentRefMapTestBase {
    /**
     * Returns a new empty map.
     */
    @Override
    protected <K, V> ConcurrentSoftHashMap<K, V> emptyMap() {
        return new ConcurrentSoftHashMap<>();
    }

    @DisplayName("Constructor (initialCapacity, loadFactor, concurrencyLevel) throws IllegalArgumentException if any argument is negative")
    @Test
    public void testConstructor3() {
        assertThatThrownBy(() -> new ConcurrentSoftHashMap<Object, String>(-1, .75f, 1, HashingStrategy.canonical()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConcurrentSoftHashMap<Object, String>(16, -1, 1, HashingStrategy.canonical()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConcurrentSoftHashMap<Object, String>(16, .75f, -1, HashingStrategy.canonical()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
