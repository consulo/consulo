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
package consulo.util.lang;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2026-03-01
 */
public class StringEscapeUtilTest {
    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testEscape() {
        assertThat(StringEscapeUtil.escape("\b\f\n\r\t\u007F\uFEFF\"'\\foo", '"'))
            .isEqualTo(StringEscapeUtil.escape("\b\f\n\r\t\u007F\uFEFF\"'\\foo", '"', sb()).toString())
            .isEqualTo("\\b\\f\\n\\r\\t\\u007F\\uFEFF\\\"'\\\\foo");

        assertThat(StringEscapeUtil.escape("\b\f\n\r\t\u007F\uFEFF\"'\\foo", '\''))
            .isEqualTo(StringEscapeUtil.escape("\b\f\n\r\t\u007F\uFEFF\"'\\foo", '\'', sb()).toString())
            .isEqualTo("\\b\\f\\n\\r\\t\\u007F\\uFEFF\"\\'\\\\foo");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testQuote() {
        assertThat(StringEscapeUtil.quote("\b\f\n\r\t\u007F\uFEFF\"'\\foo", '"'))
            .isEqualTo(StringEscapeUtil.quote("\b\f\n\r\t\u007F\uFEFF\"'\\foo", '"', sb()).toString())
            .isEqualTo("\"\\b\\f\\n\\r\\t\\u007F\\uFEFF\\\"'\\\\foo\"");

        assertThat(StringEscapeUtil.quote("\b\f\n\r\t\u007F\uFEFF\"'\\foo", '\''))
            .isEqualTo(StringEscapeUtil.quote("\b\f\n\r\t\u007F\uFEFF\"'\\foo", '\'', sb()).toString())
            .isEqualTo("'\\b\\f\\n\\r\\t\\u007F\\uFEFF\"\\'\\\\foo'");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testUnescape() {
        assertThat(StringEscapeUtil.unescape("\\0\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo"))
            .isEqualTo(StringEscapeUtil.unescape("\\0\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo", sb()).toString())
            .isEqualTo("\0\b\f\n\r\t\u007F\uFEFF\"'\\foo");

        assertThat(StringEscapeUtil.unescape("\\uXXXX")).isEqualTo("\\uXXXX");
        assertThat(StringEscapeUtil.unescape("\\uXXX")).isEqualTo("\\uXXX");
        assertThat(StringEscapeUtil.unescape("\\z\\")).isEqualTo("\\z\\");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testUnquote() {
        assertThat(StringEscapeUtil.unquote("\"\\0\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo\"", '"'))
            .isEqualTo(StringEscapeUtil.unquote("\"\\0\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo\"", '"', sb()).toString())
            .isEqualTo(StringEscapeUtil.unquote("'\\0\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo'", '\''))
            .isEqualTo(StringEscapeUtil.unquote("\\0\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo", '"'))
            .isEqualTo(StringEscapeUtil.unquote("\\0\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo", '"', sb()).toString())
            .isEqualTo("\0\b\f\n\r\t\u007F\uFEFF\"'\\foo");
    }

    @Nonnull
    private static StringBuilder sb() {
        return new StringBuilder();
    }
}
