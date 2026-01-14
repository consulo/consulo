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
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author UNV
 * @since 2025-11-19
 */
public class CachingLocalizedValueTest {
    class MyLocalizedValue implements LocalizedValue {
        public String myValue;
        public int myHashCode;

        @Nonnull
        @Override
        public String getValue() {
            return myValue;
        }

        @Override
        public byte getModificationCount() {
            return 0;
        }

        @Override
        public int hashCode() {
            return myHashCode;
        }
    }

    MyLocalizedValue mySubValue = new MyLocalizedValue();
    LocalizationManager myManager = mock(LocalizationManager.class);

    @Test
    void testCalcCacheValue() {
        LocalizedValue value = new JoinedLocalizedValue(myManager, new LocalizedValue[]{mySubValue});

        mySubValue.myValue = "Foo";
        mySubValue.myHashCode = 111;
        when(myManager.getModificationCount()).thenReturn((byte) 1);

        assertThat(value.getValue()).isEqualTo("Foo");
        assertThat(value.getModificationCount()).isEqualTo((byte) 1);
        int hashCode = value.hashCode();

        mySubValue.myValue = "Bar";
        mySubValue.myHashCode = 222;

        assertThat(value.getValue()).isEqualTo("Foo");
        assertThat(value.getModificationCount()).isEqualTo((byte) 1);
        assertThat(value.hashCode()).isEqualTo(hashCode);

        when(myManager.getModificationCount()).thenReturn((byte) 2);

        assertThat(value.getValue()).isEqualTo("Bar");
        assertThat(value.getModificationCount()).isEqualTo((byte) 2);
        assertThat(value.hashCode()).isEqualTo(hashCode);
    }
}
