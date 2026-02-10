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
import org.mockito.Mockito;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2025-11-18
 */
public class MappedLocalizedValueTest {
    LocalizationManager myManager = Mockito.mock(LocalizationManager.class);

    LocalizedValue subValue = LocalizedValue.of("Foo");
    LocalizedValue value = new MappedLocalizedValue(myManager, subValue, string -> string + "bar");

    @Test
    void testId() {
        assertThat(value.getId())
            .matches("\"Foo\"->consulo\\.localization\\.internal\\.MappedLocalizedValueTest\\$\\$Lambda/.*");
    }

    @Test
    void testValue() {
        assertThat(value.getValue())
            .isEqualTo("Foobar")
            .isEqualTo(value.get())
            .isEqualTo(value.toString());
    }

    @Test
    void testKey() {
        assertThat(value.getKey()).isNotPresent();
    }

    @Test
    void testEqualsAndHashCode() {
        LocalizedValue subValue0 = LocalizedValue.of("Foo");
        LocalizedValue subValue1 = LocalizedValue.of("Bar");
        LocalizedValue subValue2 = LocalizedValue.of("Bar");
        Function<String, String> mapper1 = string -> string + "bar";
        Function<String, String> mapper2 = string -> string + "baz";

        LocalizedValue value01 = new MappedLocalizedValue(myManager, subValue0, mapper1);
        LocalizedValue value02 = new MappedLocalizedValue(myManager, subValue0, mapper2);
        LocalizedValue value11 = new MappedLocalizedValue(myManager, subValue1, mapper1);
        LocalizedValue value12 = new MappedLocalizedValue(myManager, subValue1, mapper2);
        LocalizedValue value21 = new MappedLocalizedValue(myManager, subValue2, mapper1);
        LocalizedValue value22 = new MappedLocalizedValue(myManager, subValue2, mapper2);

        assertThat(value11.hashCode())
            .isEqualTo(value21.hashCode())
            .isNotEqualTo(value01.hashCode())
            .isNotEqualTo(value02.hashCode())
            .isNotEqualTo(value12.hashCode())
            .isNotEqualTo(value22.hashCode());

        assertThat(value11)
            .isEqualTo(value21)
            .isNotEqualTo(value01)
            .isNotEqualTo(value02)
            .isNotEqualTo(value12)
            .isNotEqualTo(value22);

        assertThat(value12.hashCode())
            .isEqualTo(value22.hashCode())
            .isNotEqualTo(value01.hashCode())
            .isNotEqualTo(value02.hashCode())
            .isNotEqualTo(value11.hashCode())
            .isNotEqualTo(value21.hashCode());

        assertThat(value12)
            .isEqualTo(value22)
            .isNotEqualTo(value01)
            .isNotEqualTo(value02)
            .isNotEqualTo(value11)
            .isNotEqualTo(value21);
    }
}
