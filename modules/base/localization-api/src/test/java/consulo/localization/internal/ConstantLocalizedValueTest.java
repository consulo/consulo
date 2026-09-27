/*
 * Copyright 2013-2025 consulo.io
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
 * @since 2025-11-19
 */
public class ConstantLocalizedValueTest {
    LocalizedValue value = c("Foo");

    @Test
    void testId() {
        assertThat(value.getId()).isEqualTo("\"Foo\"");
    }

    @Test
    void testValue() {
        assertThat(value.getValue())
            .isEqualTo("Foo")
            .isEqualTo(value.get())
            .isEqualTo(value.toString());
    }

    @Test
    void testKey() {
        assertThat(value.getKey()).isNotPresent();
    }

    @Test
    void testModificationCount() {
        assertThat(value.getModificationCount()).isEqualTo((byte) 0);
    }

    @Test
    @SuppressWarnings("RedundantStringConstructorCall")
    void testEqualsAndHashCode() {
        LocalizedValue value0 = c("Foo");
        LocalizedValue value1 = c("Bar");
        LocalizedValue value2 = c(new String("Bar"));

        assertThat(value1)
            .isEqualTo(value2)
            .isNotEqualTo(value0);

        assertThat(value1.hashCode())
            .isEqualTo(value2.hashCode())
            .isNotEqualTo(value0.hashCode());
    }

    @Test
    void testCompareTo() {
        assertThat(c("Foo")).isEqualByComparingTo(c("Foo"));
        assertLessThan(c("Foo"), c("foo"));
        assertLessThan(c("fo"), c("Foo"));
        assertLessThan(c("Bar"), c("Foo"));
    }

    @Test
    void testNaturalCompareTo() {
        assertLessThan(c("Foo3"), c("Foo4"));
        assertLessThan(c("Foo3"), c("Foo20"));
        assertLessThan(c("Foo20"), c("Foo100"));
        assertLessThan(c("Foo20"), c("foo100"));
    }

    private static <T extends Comparable<T>> void assertLessThan(T a, T b) {
        assertThat(a).isLessThan(b);
        assertThat(b).isGreaterThan(a);
    }

    private static ConstantLocalizedValue c(String value) {
        return new ConstantLocalizedValue(value);
    }
}
