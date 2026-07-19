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
package consulo.util.collection.impl;

import consulo.util.collection.HashingStrategy;
import it.unimi.dsi.fastutil.Hash;
import org.jspecify.annotations.Nullable;

/**
 * Adapts a consulo {@link HashingStrategy} to a fastutil {@link Hash.Strategy}.
 *
 * @author VISTALL
 */
public final class FastUtilHashingStrategies {
    private FastUtilHashingStrategies() {
    }

    /**
     * fastutil requires {@code 0 < loadFactor < 1}, while the legacy trove-based API used (and allowed)
     * a load factor of {@code 1}. Substitute an out-of-range value with fastutil's default.
     */
    public static float loadFactor(float loadFactor) {
        return loadFactor > 0f && loadFactor < 1f ? loadFactor : Hash.DEFAULT_LOAD_FACTOR;
    }

    public static <K> Hash.Strategy<K> of(HashingStrategy<? super K> strategy) {
        return new Hash.Strategy<>() {
            @Override
            public int hashCode(@Nullable K o) {
                return o == null ? 0 : strategy.hashCode(o);
            }

            @Override
            public boolean equals(@Nullable K a, @Nullable K b) {
                return a == b || a != null && b != null && strategy.equals(a, b);
            }
        };
    }
}
