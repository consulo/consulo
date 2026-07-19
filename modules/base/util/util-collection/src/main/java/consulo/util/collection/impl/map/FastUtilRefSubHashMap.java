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
import consulo.util.collection.impl.FastUtilHashingStrategies;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * {@link RefHashMap.SubMap} backed by a fastutil {@link Object2ObjectOpenCustomHashMap} keyed by
 * reference keys with a custom strategy that compares the referents.
 *
 * @author VISTALL
 */
class FastUtilRefSubHashMap<K, V> implements RefHashMap.SubMap<K, V> {
    private final Object2ObjectOpenCustomHashMap<RefHashMap.Key<K>, V> myDelegate;

    FastUtilRefSubHashMap(int initialCapacity, float loadFactor, RefHashMap<K, V> map, HashingStrategy<? super K> hashingStrategy) {
        float lf = FastUtilHashingStrategies.loadFactor(loadFactor);
        myDelegate = new Object2ObjectOpenCustomHashMap<>(initialCapacity, lf, new Hash.Strategy<>() {
            @Override
            public int hashCode(RefHashMap.@Nullable Key<K> key) {
                return key == null ? 0 : key.hashCode(); // use stored hashCode
            }

            @Override
            public boolean equals(RefHashMap.@Nullable Key<K> o1, RefHashMap.@Nullable Key<K> o2) {
                return o1 == o2 || o1 != null && o2 != null && RefHashMap.keyEqual(o1.get(), o2.get(), hashingStrategy);
            }
        });
    }

    @Override
    public void compactIfNecessary() {
        // fastutil open hash maps do not track dead slots and do not auto-shrink; garbage collected
        // entries are already dropped via remove(), so avoid an O(n) trim() on every queue processing
    }

    @Override
    public int size() {
        return myDelegate.size();
    }

    @Override
    public boolean isEmpty() {
        return myDelegate.isEmpty();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return myDelegate.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return myDelegate.containsValue(value);
    }

    @Override
    public @Nullable V get(@Nullable Object key) {
        return myDelegate.get(key);
    }

    @Override
    public @Nullable V put(RefHashMap.Key<K> key, V value) {
        return myDelegate.put(key, value);
    }

    @Override
    public @Nullable V remove(@Nullable Object key) {
        return myDelegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends RefHashMap.Key<K>, ? extends V> m) {
        myDelegate.putAll(m);
    }

    @Override
    public void clear() {
        myDelegate.clear();
    }

    @Override
    public Set<RefHashMap.Key<K>> keySet() {
        return myDelegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return myDelegate.values();
    }

    @Override
    public Set<Map.Entry<RefHashMap.Key<K>, V>> entrySet() {
        return myDelegate.entrySet();
    }
}
