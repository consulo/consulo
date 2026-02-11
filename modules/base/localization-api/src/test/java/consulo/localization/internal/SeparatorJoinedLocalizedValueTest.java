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

import consulo.localization.LocalizationManager;
import consulo.localization.LocalizedValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author UNV
 * @since 2025-11-19
 */
public class SeparatorJoinedLocalizedValueTest {
    LocalizationManager myManager = mock(LocalizationManager.class);

    LocalizedValue[] subValues = {LocalizedValue.of("Foo"), LocalizedValue.empty(), LocalizedValue.of("Bar")};
    LocalizedValue value = new SeparatorJoinedLocalizedValue(myManager, ", ", subValues);

    @Test
    void testId() {
        assertThat(value.getId())
            .isEqualTo("[\"Foo\",empty,\"Bar\"]->join(\", \")");
    }

    @Test
    void testValue() {
        assertThat(value.getValue())
            .isEqualTo("Foo, Bar")
            .isEqualTo(value.get())
            .isEqualTo(value.toString());
    }

    @Test
    void testKey() {
        assertThat(value.getKey()).isNotPresent();
    }

    @Test
    void testEqualsAndHashCode() {
        LocalizedValue[] subValues0 = {LocalizedValue.of("Foo")};
        LocalizedValue[] subValues1 = {LocalizedValue.of("Foo"), LocalizedValue.of("Bar")};
        LocalizedValue[] subValues2 = {LocalizedValue.of("Foo"), LocalizedValue.of("Bar")};
        LocalizedValue value0 = new SeparatorJoinedLocalizedValue(myManager, ", ", subValues0);
        LocalizedValue value1 = new SeparatorJoinedLocalizedValue(myManager, ", ", subValues1);
        LocalizedValue value2 = new SeparatorJoinedLocalizedValue(myManager, ", ", subValues2);

        assertThat(value1.hashCode())
            .isEqualTo(value2.hashCode())
            .isNotEqualTo(value0.hashCode());

        assertThat(value1)
            .isEqualTo(value2)
            .isNotEqualTo(value0);
    }
}
