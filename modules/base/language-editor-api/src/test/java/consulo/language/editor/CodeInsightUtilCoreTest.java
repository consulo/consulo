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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2026-03-02
 */
public class CodeInsightUtilCoreTest {
    @Test
    void testParseStringCharacters() {
        String chars = "\\0\\12\\345\\456\\b\\f\\n\\r\\t\\u007F\\uuuFEff\\\"\\'\\\\foo";
        String parsed = "\0\12\345%6\b\f\n\r\t\u007F\uFEFF\"'\\foo";
        int[] sourceOffsets = new int[chars.length() + 1];

        assertThat(CodeInsightUtilCore.parseStringCharacters(chars, null)).hasToString(parsed);
        assertThat(CodeInsightUtilCore.parseStringCharacters(chars, sourceOffsets)).hasToString(parsed);
        assertThat(sourceOffsets).containsExactly(
            0, 2, 5, 9, 12, 13, 15, 17, 19, 21, 23, 29, 37, 39, 41, 43, 44, 45, 46,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
    }

    @Test
    void testParseNoEscape() {
        String noEscapeChars = "foobar";
        int[] noEscapeOffsets = new int[noEscapeChars.length() + 1];

        assertThat(CodeInsightUtilCore.parseStringCharacters(noEscapeChars, null)).isSameAs(noEscapeChars);
        assertThat(CodeInsightUtilCore.parseStringCharacters(noEscapeChars, noEscapeOffsets)).isSameAs(noEscapeChars);
        assertThat(noEscapeOffsets).containsExactly(0, 1, 2, 3, 4, 5, 6);
    }

    @Test
    void testParseErrors() {
        String chars = "\\u000A";
        int[] sourceOffsets = new int[chars.length() + 1];

        assertThat(CodeInsightUtilCore.parseStringCharacters(chars, null)).isNull();
        assertThat(CodeInsightUtilCore.parseStringCharacters(chars, sourceOffsets)).isNull();
        assertThat(sourceOffsets).containsExactly(0, 1, 0, 0, 0, 0, 0);
    }
}
