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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2026-09-22
 */
@SuppressWarnings("RedundantStringConstructorCall")
public class HashingStrategyTest {
    @Test
    void canonical() {
        String foo1 = "foo";
        String foo2 = new String(foo1);
        HashingStrategy<Object> canonical = HashingStrategy.canonical();
        assertThat(canonical.hashCode(null)).isEqualTo(0);
        assertThat(canonical.hashCode(foo1)).isEqualTo(canonical.hashCode(foo1));
        assertThat(canonical.hashCode(foo1)).isEqualTo(canonical.hashCode(foo2));
        assertThat(canonical.equals(null, null)).isTrue();
        assertThat(canonical.equals(null, foo1)).isFalse();
        assertThat(canonical.equals(foo1, foo1)).isTrue();
        assertThat(canonical.equals(foo1, foo2)).isTrue();
        assertThat(canonical.equals(foo1, "Foo")).isFalse();
    }

    @Test
    void identity() {
        String foo1 = "foo";
        String foo2 = new String(foo1);
        HashingStrategy<Object> identity = HashingStrategy.identity();
        assertThat(identity.hashCode(foo1)).isEqualTo(identity.hashCode(foo1));
        assertThat(identity.equals(null, null)).isTrue();
        assertThat(identity.equals(foo1, foo1)).isTrue();
        assertThat(identity.equals(foo1, foo2)).isFalse();
    }

    @Test
    void caseInsensitive() {
        String foo1 = "foo";
        String foo2 = new String(foo1);
        String foo3 = "Foo";
        HashingStrategy<String> caseInsensitive = HashingStrategy.caseInsensitive();
        assertThat(caseInsensitive.hashCode(foo1)).isEqualTo(caseInsensitive.hashCode(foo1));
        assertThat(caseInsensitive.hashCode(foo1)).isEqualTo(caseInsensitive.hashCode(foo2));
        assertThat(caseInsensitive.hashCode(foo1)).isEqualTo(caseInsensitive.hashCode(foo3));
        assertThat(caseInsensitive.equals(null, null)).isTrue();
        assertThat(caseInsensitive.equals(null, foo1)).isFalse();
        assertThat(caseInsensitive.equals(foo1, foo1)).isTrue();
        assertThat(caseInsensitive.equals(foo1, foo2)).isTrue();
        assertThat(caseInsensitive.equals(foo1, foo3)).isTrue();
        assertThat(caseInsensitive.equals(foo1, "bar")).isFalse();
    }
}
