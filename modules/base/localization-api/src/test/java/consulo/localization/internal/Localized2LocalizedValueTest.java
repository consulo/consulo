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

import consulo.localization.LocalizationManager;
import consulo.localization.Localized;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2026-09-25
 */
public class Localized2LocalizedValueTest {
    LocalizationManager myManager = Mockito.mock(LocalizationManager.class);
    Localized myLocalized = Mockito.mock(Localized.class);
    Localized2LocalizedValue myValue = new Localized2LocalizedValue(myManager, myLocalized);

    @Test
    void testId() {
        Mockito.when(myLocalized.getId()).thenReturn("foobar");
        assertThat(myValue.getId()).isEqualTo("foobar→LocalizedValue");
    }

    @Test
    void testValue() {
        String text = "foobar";
        Mockito.when(myLocalized.toString()).thenReturn(text);
        assertThat(myValue.toString()).isEqualTo(text);
    }

    @Test
    void testEqualsAndHashCode() {
        assertThat(new Localized2LocalizedValue(myManager, myLocalized))
            .isEqualTo(myValue)
            .hasSameHashCodeAs(myValue);
    }
}
