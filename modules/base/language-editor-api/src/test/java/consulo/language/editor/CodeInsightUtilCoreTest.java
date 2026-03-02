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
        assertThat(CodeInsightUtilCore.parseStringCharacters("\\0\\12\\345\\456\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo", null))
            .hasToString("\0\12\345%6\b\f\n\r\t\u007F\uFEFF\"'\\foo");
    }
}
