/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class BidirectionalMap<K, V> implements Map<K, V> {
    private final Map<K, V> myKeyToValueMap = new HashMap<>();
    private final Map<V, List<K>> myValueToKeysMap = new HashMap<>();

    @Override
    public V put(K key, V value) {
        V oldValue = myKeyToValueMap.put(key, value);
        if (oldValue != null) {
            if (oldValue.equals(value)) {
                return oldValue;
            }
            List<K> array = myValueToKeysMap.get(oldValue);
            array.remove(key);
        }

        List<K> array = myValueToKeysMap.get(value);
        if (array == null) {
            array = new ArrayList<>();
            myValueToKeysMap.put(value, array);
        }
        array.add(key);
        return oldValue;
    }

    @Override
    public void clear() {
        myKeyToValueMap.clear();
        myValueToKeysMap.clear();
    }

    @Nullable
    public List<K> getKeysByValue(V value) {
        return myValueToKeysMap.get(value);
    }

    @Nonnull
    @Override
    public Set<K> keySet() {
        return myKeyToValueMap.keySet();
    }

    @Override
    public int size() {
        return myKeyToValueMap.size();
    }

    @Override
    public boolean isEmpty() {
        return myKeyToValueMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return myKeyToValueMap.containsKey(key);
    }

    @Override
    @SuppressWarnings({"SuspiciousMethodCalls"})
    public boolean containsValue(Object value) {
        return myValueToKeysMap.containsKey(value);
    }

    @Override
    public V get(Object key) {
        return myKeyToValueMap.get(key);
    }

    public void removeValue(V v) {
        List<K> ks = myValueToKeysMap.remove(v);
        if (ks != null) {
            for (K k : ks) {
                myKeyToValueMap.remove(k);
            }
        }
    }

    @Override
    @SuppressWarnings({"SuspiciousMethodCalls"})
    public V remove(Object key) {
        final V value = myKeyToValueMap.remove(key);
        final List<K> ks = myValueToKeysMap.get(value);
        if (ks != null) {
            if (ks.size() > 1) {
                ks.remove(key);
            }
            else {
                myValueToKeysMap.remove(value);
            }
        }
        return value;
    }

    @Override
    public void putAll(@Nonnull Map<? extends K, ? extends V> t) {
        for (final K k1 : t.keySet()) {
            put(k1, t.get(k1));
        }
    }

    @Nonnull
    @Override
    public Collection<V> values() {
        return myValueToKeysMap.keySet();
    }

    @Nonnull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return myKeyToValueMap.entrySet();
    }

    @Override
    public String toString() {
        return myKeyToValueMap.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BidirectionalMap<?, ?> that = (BidirectionalMap<?, ?>) o;
        return myKeyToValueMap.equals(that.myKeyToValueMap);
    }

    @Override
    public int hashCode() {
        return myKeyToValueMap.hashCode();
    }
}
