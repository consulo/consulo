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
public class SeparatorJoinedLocalizedValue2Test {
    LocalizationManager myManager = mock(LocalizationManager.class);

    @Test
    void testValue() {
        LocalizedValue[] subValues = {LocalizedValue.of("Foo"), LocalizedValue.empty(), LocalizedValue.of("Bar")};
        JoinedLocalizedValue value = new SeparatorJoinedLocalizedValue2(myManager, LocalizedValue.of(", "), subValues);
        assertThat(value.getValue())
            .isEqualTo("Foo, Bar")
            .isEqualTo(value.get())
            .isEqualTo(value.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalizedValue separator1 = LocalizedValue.of(", ");
        LocalizedValue separator2 = LocalizedValue.of("; ");
        LocalizedValue[] subValues0 = {LocalizedValue.of("Foo")};
        LocalizedValue[] subValues1 = {LocalizedValue.of("Foo"), LocalizedValue.of("Bar")};
        LocalizedValue[] subValues2 = {LocalizedValue.of("Foo"), LocalizedValue.of("Bar")};
        LocalizedValue value10 = new SeparatorJoinedLocalizedValue2(myManager, separator1, subValues0);
        LocalizedValue value11 = new SeparatorJoinedLocalizedValue2(myManager, separator1, subValues1);
        LocalizedValue value12 = new SeparatorJoinedLocalizedValue2(myManager, separator1, subValues2);
        LocalizedValue value22 = new SeparatorJoinedLocalizedValue2(myManager, separator2, subValues2);

        assertThat(value11.hashCode())
            .isEqualTo(value12.hashCode())
            .isNotEqualTo(value10.hashCode())
            .isNotEqualTo(value22.hashCode());

        assertThat(value11)
            .isEqualTo(value12)
            .isNotEqualTo(value10)
            .isNotEqualTo(value22);
    }
}
