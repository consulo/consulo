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

/**
 * {@link SoftHashMap} whose backing sub map is a fastutil open custom hash map.
 *
 * @author VISTALL
 */
public class FastUtilSoftHashMap<K, V> extends SoftHashMap<K, V> {
    public FastUtilSoftHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public FastUtilSoftHashMap(HashingStrategy<? super K> hashingStrategy) {
        super(hashingStrategy);
    }

    @Override
    protected SubMap<K, V> createSubMap(int initialCapacity, float loadFactor, HashingStrategy<? super K> strategy) {
        return new FastUtilRefSubHashMap<>(initialCapacity, loadFactor, this, strategy);
    }
}
