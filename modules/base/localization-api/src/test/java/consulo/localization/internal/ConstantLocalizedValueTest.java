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
    @Test
    void testValue() {
        LocalizedValue value = new ConstantLocalizedValue("Foo");
        assertThat(value.getValue())
            .isEqualTo("Foo")
            .isEqualTo(value.get())
            .isEqualTo(value.toString());
    }

    @Test
    void testKey() {
        LocalizedValue value = new ConstantLocalizedValue("Foo");
        assertThat(value.getKey()).isNotPresent();
    }

    @Test
    void testModificationCount() {
        LocalizedValue value = new ConstantLocalizedValue("Foo");
        assertThat(value.getModificationCount()).isEqualTo((byte) 0);
    }

    @Test
    @SuppressWarnings("RedundantStringConstructorCall")
    void testEqualsAndHashCode() {
        LocalizedValue value0 = new ConstantLocalizedValue("Foo");
        LocalizedValue value1 = new ConstantLocalizedValue("Bar");
        LocalizedValue value2 = new ConstantLocalizedValue(new String("Bar"));

        assertThat(value1)
            .isEqualTo(value2)
            .isNotEqualTo(value0);

        assertThat(value1.hashCode())
            .isEqualTo(value2.hashCode())
            .isNotEqualTo(value0.hashCode());
    }

    @Test
    void testCompareTo() {
        LocalizedValue value1 = new ConstantLocalizedValue("Foo");
        LocalizedValue value2 = new ConstantLocalizedValue("Foo");
        LocalizedValue value3 = new ConstantLocalizedValue("foo");
        LocalizedValue value4 = new ConstantLocalizedValue("fo");
        LocalizedValue value5 = new ConstantLocalizedValue("Bar");

        assertThat(value1.compareTo(value2)).isEqualTo(0);
        assertThat(value1.compareTo(value3)).isLessThan(0);
        assertThat(value1.compareTo(value4)).isGreaterThan(0);
        assertThat(value1.compareTo(value5)).isGreaterThan(0);
    }
}
