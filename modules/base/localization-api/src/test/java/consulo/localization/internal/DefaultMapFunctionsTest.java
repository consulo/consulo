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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author UNV
 * @since 2026-10-05
 */
public class DefaultMapFunctionsTest {
    LocalizationManager myManager = Mockito.mock(LocalizationManager.class);

    @BeforeEach
    void init() {
        Mockito.when(myManager.getLocale()).thenReturn(Locale.ROOT);
    }

    @Test
    void toUpperCase() {
        assertThat(DefaultMapFunctions.TO_UPPER_CASE.apply(myManager, "foo123")).isEqualTo("FOO123");
    }

    @Test
    void toLowerCase() {
        assertThat(DefaultMapFunctions.TO_LOWER_CASE.apply(myManager, "FOO123")).isEqualTo("foo123");
    }

    @Test
    void capitalize() {
        assertThat(DefaultMapFunctions.CAPITALIZE.apply(myManager, "")).isEqualTo("");
        assertThat(DefaultMapFunctions.CAPITALIZE.apply(myManager, "f")).isEqualTo("F");
        assertThat(DefaultMapFunctions.CAPITALIZE.apply(myManager, "Foo")).isEqualTo("Foo");
        assertThat(DefaultMapFunctions.CAPITALIZE.apply(myManager, "foo")).isEqualTo("Foo");
    }

    @Test
    void appendEllipsis() {
        assertThat(DefaultMapFunctions.APPEND_ELLIPSIS.apply(myManager, "foo")).isEqualTo("foo…");
    }

    @Test
    void removeEllipsis() {
        assertThat(DefaultMapFunctions.REMOVE_ELLIPSIS.apply(myManager, "foo")).isEqualTo("foo");
        assertThat(DefaultMapFunctions.REMOVE_ELLIPSIS.apply(myManager, "foo…")).isEqualTo("foo");
        assertThat(DefaultMapFunctions.REMOVE_ELLIPSIS.apply(myManager, "foo...")).isEqualTo("foo");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void truncateWithEllipsis() {
        assertThat(new DefaultMapFunctions.EllipsisTruncator(7).apply(myManager, "foobar")).isEqualTo("foobar");
        assertThat(new DefaultMapFunctions.EllipsisTruncator(6).apply(myManager, "foobar")).isEqualTo("foobar");
        assertThat(new DefaultMapFunctions.EllipsisTruncator(5).apply(myManager, "foobar")).isEqualTo("foob…");
        assertThat(new DefaultMapFunctions.EllipsisTruncator(4).apply(myManager, "foobar")).isEqualTo("foo…");
        assertThat(new DefaultMapFunctions.EllipsisTruncator(3).apply(myManager, "foobar")).isEqualTo("fo…");
        assertThatThrownBy(() -> new DefaultMapFunctions.EllipsisTruncator(0).apply(myManager, "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Expecting maxLength (0) to be at least 3");
    }
}
