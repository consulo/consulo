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

import consulo.localization.LocalizationKey;
import consulo.localization.LocalizationManager;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author UNV
 * @since 2025-11-19
 */
public class DefaultLocalizedValueTest {
    LocalizationKey myKey = mock(LocalizeKey.class);
    LocalizationManager myManager = mock(LocalizeManager.class);

    @Test
    void testValue() {
        when(myManager.getUnformattedText(any())).thenReturn(Map.entry(Locale.ROOT, "-"));
        when(myManager.getUnformattedText(myKey)).thenReturn(Map.entry(Locale.ROOT, "Foo"));

        DefaultLocalizedValue value = new DefaultLocalizedValue(myManager, myKey);

        assertThat(value.getValue())
            .isEqualTo("Foo")
            .isSameAs(value.get())
            .isSameAs(value.toString());
    }

    @Test
    void testParametrizedValue() {
        String unformattedText = "Foo{0}", param = "bar";
        when(myManager.getUnformattedText(any())).thenReturn(Map.entry(Locale.ROOT, "-"));
        when(myManager.getUnformattedText(myKey)).thenReturn(Map.entry(Locale.ROOT, unformattedText));
        when(myManager.formatText(any(), any(), any())).thenReturn("-");
        when(myManager.formatText(unformattedText, Locale.ROOT, param)).thenReturn("Foobar");

        DefaultLocalizedValue value = new DefaultLocalizedValue(myManager, myKey, param);

        assertThat(value.getValue())
            .isEqualTo("Foobar")
            .isSameAs(value.get())
            .isSameAs(value.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        DefaultLocalizedValue value1 = new DefaultLocalizedValue(myManager, myKey);
        DefaultLocalizedValue value2 = new DefaultLocalizedValue(myManager, myKey);

        assertThat(value1).isEqualTo(value1);
        assertThat(value1).isEqualTo(value2);
        assertThat(value1.hashCode()).isEqualTo(value2.hashCode());
    }
}
