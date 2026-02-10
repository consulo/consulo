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
package consulo.localization.internal;

import consulo.localization.LocalizedValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2026-01-13
 */
public class EmptyLocalizedValueTest {
    LocalizedValue value = EmptyLocalizedValue.INSTANCE;

    @Test
    void testId() {
        assertThat(value.getId())
            .isEqualTo("empty");
    }

    @Test
    void testValue() {
        assertThat(value.getValue())
            .isEqualTo("")
            .isEqualTo(value.get())
            .isEqualTo(value.toString());
        assertThat(value.getNullIfEmpty()).isNull();
    }

    @Test
    void testIsEmpty() {
        assertThat(value.isEmpty()).isTrue();
        assertThat(value.isNotEmpty()).isFalse();
    }

    @Test
    void testOrIfEmpty() {
        LocalizedValue otherValue = LocalizedValue.dot();
        assertThat(value.orIfEmpty(otherValue)).isSameAs(otherValue);
    }

    @Test
    void testKey() {
        assertThat(value.getKey()).isNotPresent();
    }

    @Test
    void testMap() {
        assertThat(value.map(string -> string + "foobar")).isSameAs(value);
        assertThat(value.map((manager, string) -> string + "foobar")).isSameAs(value);
    }

    @Test
    void testModificationCount() {
        assertThat(value.getModificationCount()).isEqualTo((byte) 0);
    }

    @Test
    void testEquals() {
        assertThat(value)
            .isEqualTo(value)
            .isNotEqualTo(LocalizedValue.dot());
    }

    @Test
    void testCompareTo() {
        LocalizedValue value1 = new ConstantLocalizedValue("Foo");

        assertThat(value.compareTo(value)).isEqualTo(0);
        assertThat(value.compareTo(value1)).isLessThan(0);
        assertThat(value1.compareTo(value)).isGreaterThan(0);
    }
}
