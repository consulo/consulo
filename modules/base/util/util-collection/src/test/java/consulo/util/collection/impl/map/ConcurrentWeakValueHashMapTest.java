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

import consulo.util.collection.impl.map.base.ConcurrentRefValueMapTestBase;

/**
 * @author UNV
 * @since 2026-03-15
 */
public class ConcurrentWeakValueHashMapTest extends ConcurrentRefValueMapTestBase {
    /**
     * Returns a new empty map.
     */
    @Override
    protected <K, V> ConcurrentWeakValueHashMap<K, V> emptyMap() {
        return new ConcurrentWeakValueHashMap<>();
    }
}
