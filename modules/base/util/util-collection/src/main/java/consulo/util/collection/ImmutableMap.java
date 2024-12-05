/*
 * Copyright 2013-2024 consulo.io
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

import java.util.Map;

/**
 * @author UNV
 * @since 2024-12-02
 */
public interface ImmutableMap<K, V> extends Map<K, V> {
    /**
     * <p>Returns an {@code ImmutableMap} which contains all the entries as this map except the supplied key.</p>
     *
     * @param key a key to exclude from the result
     * @return an {@code ImmutableMap} which contains all the entries as this map except the supplied key.
     */
    ImmutableMap<K, V> without(@Nonnull K key);

    /**
     * <p>Returns an {@code ImmutableMap} which contains all the entries as this map plus the supplied mapping.</p>
     *
     * <p>May return the same map if given key is already associated with the same value. Note, however, that if value is
     * not the same but equal object, the new map will be created as sometimes it's desired to replace the object with
     * another one which is equal to the old object.</p>
     *
     * @param key   a key to add/replace
     * @param value a value to associate with the key
     * @return an {@code ImmutableMap} which contains all the entries as this map plus the supplied mapping.
     */
    ImmutableMap<K, V> with(@Nonnull K key, @Nullable V value);

    /**
     * <p>Returns an {@code ImmutableMap} which contains all the entries as this map plus all the mappings of the supplied map.</p>
     *
     * <p>May (but not guaranteed) return the same map if the supplied map is empty or all its mappings already exist in this map
     * (assuming values are compared by reference). The equals/hashCode strategy of the resulting map is the same as the strategy
     * of this map.</p>
     *
     * @param map to add entries from
     * @return an {@code ImmutableMap} which contains all the entries as this map plus all the mappings of the supplied map.
     */
    ImmutableMap<K, V> withAll(@Nonnull Map<? extends K, ? extends V> map);

    @Nonnull
    HashingStrategy<K> getStrategy();

    @Override
    default V get(Object key) {
        return getOrDefault(key, null);
    }

    /**
     * Unsupported operation: this map is immutable. Use {@link #with(Object, Object)} to create a new
     * {@code ImmutableMap} with an additional element.
     */
    @Deprecated
    @Override
    default V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation: this map is immutable. Use {@link #without(Object)} to create a new
     * {@code ImmutableMap} without some element.
     */
    @Deprecated
    @Override
    default V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation: this map is immutable. Use {@link #withAll(Map)} to create a new
     * {@code ImmutableMap} with additional elements from the specified Map.
     */
    @Deprecated
    @Override
    default void putAll(@Nonnull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation: this map is immutable. Use static method {@code #empty()} on specific map class
     * to get an empty {@code ImmutableMap}.
     */
    @Deprecated
    @Override
    default void clear() {
        throw new UnsupportedOperationException();
    }
}
