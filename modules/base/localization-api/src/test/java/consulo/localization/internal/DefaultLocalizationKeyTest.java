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
public class DefaultLocalizationKeyTest {
    private static final String LOC_ID = "FooLocalization";
    private static final String LOC_KEY = "foo.bar";

    LocalizationManager myManager = mock(LocalizationManager.class);

    @Test
    void testKey() {
        DefaultLocalizationKey key = new DefaultLocalizationKey(myManager, LOC_ID, LOC_KEY);

        assertThat(key.getLocalizationId()).isEqualTo(LOC_ID);
        assertThat(key.getKey()).isEqualTo(LOC_KEY);
        assertThat(key.toString()).isEqualTo(LOC_ID + '@' + LOC_KEY);
    }

    @Test
    void testValueCaching() {
        DefaultLocalizationKey key = new DefaultLocalizationKey(myManager, LOC_ID, LOC_KEY);
        LocalizedValue value = key.getValue();

        assertThat(value.getKey().get()).isSameAs(key);

        assertThat(key.getValue()).isSameAs(value);
    }

    @Test
    void testParametrizedValue() {
        DefaultLocalizationKey key = new DefaultLocalizationKey(myManager, LOC_ID, LOC_KEY);
        LocalizedValue value = key.getValue("Foo");

        assertThat(value.getKey().get()).isSameAs(key);
    }
}
