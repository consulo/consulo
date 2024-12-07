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

import java.util.Map;
import java.util.Objects;

/**
 * @author UNV
 * @since 2024-12-03
 */
public abstract class AbstractImmutableMap<K, V> implements ImmutableMap<K, V> {
    @Override
    @SuppressWarnings("unchecked")
    public int hashCode() {
        HashingStrategy<K> strategy = getStrategy();
        int[] h = new int[]{0};
        forEach((key, value) -> h[0] += strategy.hashCode(key) ^ Objects.hashCode(value));
        return h[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        return obj instanceof Map map
            && size() == map.size()
            && entrySet().stream().allMatch(entry -> Objects.equals(map.get(entry.getKey()), entry.getValue()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append('{');
        forEach((k, v) -> {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(k).append('=').append(v);
        });
        return sb.append('}').toString();
    }
}
