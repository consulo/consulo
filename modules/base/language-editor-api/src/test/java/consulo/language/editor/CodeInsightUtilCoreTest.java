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
package consulo.language.editor;

import jakarta.annotation.Nonnull;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2026-03-02
 */
public class CodeInsightUtilCoreTest {
    @Test
    void testParseStringCharacters() {
        assertParseStringCharacters(
            "\\0\\12\\345\\456\\b\\f\\n\\r\\t\\u007F\\uuuFEff\\\"\\'\\\\foo",
            "\0\12\345%6\b\f\n\r\t\u007F\uFEFF\"'\\foo",
            0, 2, 5, 9, 12, 13, 15, 17, 19, 21, 23, 29, 37, 39, 41, 43, 44, 45, 46
        );
        assertParseStringCharacters("\\u005C\\u005C", "\\", 0, 12);
        assertParseStringCharacters("\\u005C\\u006E", "\n", 0, 12);
    }

    @Test
    void testParseNoEscape() {
        String noEscapeChars = "foobar";
        assertParseStringCharacters(noEscapeChars, a -> a.isSameAs(noEscapeChars), 0, 1, 2, 3, 4, 5, 6);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testParseErrors() {
        assertParseStringCharacters("\\uXXXX", AbstractAssert::isNull, 0, 1);
        assertParseStringCharacters("\\u123", AbstractAssert::isNull, 0, 1);
        assertParseStringCharacters("\\u000A", AbstractAssert::isNull, 0, 1);
        assertParseStringCharacters("\\u000D", AbstractAssert::isNull, 0, 1);
        assertParseStringCharacters("\\u005Cu005C", AbstractAssert::isNull, 0, 1);
        assertParseStringCharacters("\\u005C\\u0000", AbstractAssert::isNull, 0, 1);
        assertParseStringCharacters("\\u005Cx", AbstractAssert::isNull, 0, 1);
    }

    private static void assertParseStringCharacters(@Nonnull String chars, @Nonnull String expected, @Nonnull int... expectedOffsets) {
        assertParseStringCharacters(chars, a -> a.hasToString(expected), expectedOffsets);
    }

    private static void assertParseStringCharacters(
        @Nonnull String chars,
        @Nonnull Consumer<AbstractCharSequenceAssert> check,
        @Nonnull int... expectedOffsets
    ) {
        int[] sourceOffsets = new int[chars.length() + 1];
        check.accept(assertThat(CodeInsightUtilCore.parseStringCharacters(chars, null)));
        check.accept(assertThat(CodeInsightUtilCore.parseStringCharacters(chars, sourceOffsets)));
        assertThat(sourceOffsets).containsExactly(padExpectedOffsets(sourceOffsets, expectedOffsets));
    }

    private static int[] padExpectedOffsets(int[] sourceOffsets, int[] expectedOffsets) {
        if (expectedOffsets.length < sourceOffsets.length) {
            int[] newExpectedOffsets = new int[sourceOffsets.length];
            System.arraycopy(expectedOffsets, 0, newExpectedOffsets, 0, expectedOffsets.length);
            return newExpectedOffsets;
        }
        return expectedOffsets;
    }
}
