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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author UNV
 * @since 2026-03-15
 */
public abstract class ConcurrentRefHashMapTestBase extends ConcurrentMapTestBase {
    @DisplayName("containsValue is not supported in ref maps")
    @Override
    @Test
    public void testContainsValue() {
        assertThatThrownBy(() -> emptyMap().containsValue(ABSENT_KEY)).isInstanceOf(UnsupportedOperationException.class);
    }

    @DisplayName("containsValue is not supported in ref maps")
    @Override
    @SuppressWarnings("NullAway")
    @Test
    public void testContainsValueNPE() {
        assertThatThrownBy(() -> emptyMap().containsValue(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @DisplayName("hashCode() equals sum of each key.hashCode ^ value.hashCode")
    @Test
    public void testHashCode() {
        ConcurrentMap<Object, String> map = map5();
        int sum = 0;

        for (Map.Entry<Object, String> e : map.entrySet()) {
            sum += HashingStrategy.canonical().hashCode(e.getKey()) ^ e.getValue().hashCode();
        }
        assertThat(map.hashCode()).isEqualTo(sum);
    }
}
