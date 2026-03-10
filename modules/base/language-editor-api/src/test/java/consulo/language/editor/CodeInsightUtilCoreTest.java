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
import jakarta.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2026-03-02
 */
public class CodeInsightUtilCoreTest {
    private static final String NULL = null;

    Function<String, ParseStringCharsTester> CHAR_SEQ = ParseToCharSequence::new;
    Function<String, ParseStringCharsTester> DEF_BLOCK =
        chars -> new GroupParseTester(new ParseToCharSequence(chars), new ParseToStringBuilder(chars));
    BiFunction<String, Boolean, ParseStringCharsTester> OPT_BLOCK = ParseToStringBuilder::new;

    @Test
    void testParseStringCharacters() {
        DEF_BLOCK.apply("\\0\\12\\345\\456\\b\\f\\n\\r\\s\\t\\u007F\\uuuFEff\\\"\\'\\\\foo")
            .expectResult("\0\12\345%6\b\f\n\r \t\u007F\uFEFF\"'\\foo")
            .expectOffsets(0, 2, 5, 9, 12, 13, 15, 17, 19, 21, 23, 25, 31, 39, 41, 43, 45, 46, 47, 48);
        DEF_BLOCK.apply("\\u005C\\u005C").expectResult("\\").expectOffsets(0, 12);
        DEF_BLOCK.apply("\\u005C\\u006E").expectResult("\n").expectOffsets(0, 12);
    }

    @Test
    void testParseNoEscape() {
        String noEscapeChars = "foobar";
        CHAR_SEQ.apply(noEscapeChars)
            .expectResult(a -> a.isSameAs(noEscapeChars))
            .expectOffsets(0, 1, 2, 3, 4, 5, 6);
        OPT_BLOCK.apply(noEscapeChars, true)
            .expectResult(noEscapeChars)
            .expectOffsets(0, 1, 2, 3, 4, 5, 6);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testParseEscapedNewLine() {
        DEF_BLOCK.apply("\\\n").expectResult("").expectOffsets(2, 1); // TODO: should be prohibited by default in normal literal
        OPT_BLOCK.apply("\\\n", true).expectResult("").expectOffsets(2, 1); // Allowed in block literal
        OPT_BLOCK.apply("\\\n", false).expectResult(NULL).expectOffsets(0, 1);

        DEF_BLOCK.apply("\\u000A").expectResult("\n").expectOffsets(0, 6); // TODO: should be prohibited by default in normal literal
        OPT_BLOCK.apply("\\u000A", true).expectResult("\n").expectOffsets(0, 6); // Allowed in block literal
        OPT_BLOCK.apply("\\u000A", false).expectResult(NULL).expectOffsets(0, 1);

        DEF_BLOCK.apply("\\u000D").expectResult("\r").expectOffsets(0, 6); // TODO: should be prohibited by default in normal literal
        OPT_BLOCK.apply("\\u000D", true).expectResult("\r").expectOffsets(0, 6); // Allowed in block literal
        OPT_BLOCK.apply("\\u000D", false).expectResult(NULL).expectOffsets(0, 1);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testParseErrors() {
        DEF_BLOCK.apply("\\").expectResult(NULL).expectOffsets(0, 1);
        DEF_BLOCK.apply("\\uXXXX").expectResult(NULL).expectOffsets(0, 1);
        DEF_BLOCK.apply("\\u123").expectResult(NULL).expectOffsets(0, 1);
        DEF_BLOCK.apply("\\u005Cu005C").expectResult(NULL).expectOffsets(0, 1);
        DEF_BLOCK.apply("\\u005C\\u0000").expectResult(NULL).expectOffsets(0, 1);
        DEF_BLOCK.apply("\\u005Cx").expectResult(NULL).expectOffsets(0, 1);
    }

    private interface ParseStringCharsTester {
        default ParseStringCharsTester expectResult(@Nullable String expected) {
            return expectResult(expected == null ? AbstractAssert::isNull : a -> a.hasToString(expected));
        }
        ParseStringCharsTester expectResult(@Nonnull Consumer<AbstractCharSequenceAssert> check);
        ParseStringCharsTester expectOffsets(@Nonnull int... expectedOffsets);
    }

    private abstract class AbstractParseStringCharsTester implements ParseStringCharsTester {
        @Nullable
        protected CharSequence myNoOffsets, myWithOffsets;
        @Nonnull
        protected final int[] mySourceOffsets;

        protected AbstractParseStringCharsTester(@Nonnull String chars) {
            mySourceOffsets = new int[chars.length() + 1];
        }

        @Override
        public ParseStringCharsTester expectResult(@Nonnull Consumer<AbstractCharSequenceAssert> check) {
            check.accept(assertThat(myNoOffsets));
            check.accept(assertThat(myWithOffsets));
            return this;
        }

        @Override
        public ParseStringCharsTester expectOffsets(@Nonnull int... expectedOffsets) {
            assertThat(mySourceOffsets).containsExactly(padExpectedOffsets(expectedOffsets));
            return this;
        }

        private int[] padExpectedOffsets(int[] expectedOffsets) {
            if (expectedOffsets.length < mySourceOffsets.length) {
                int[] newExpectedOffsets = new int[mySourceOffsets.length];
                System.arraycopy(expectedOffsets, 0, newExpectedOffsets, 0, expectedOffsets.length);
                return newExpectedOffsets;
            }
            return expectedOffsets;
        }
    }

    private class ParseToCharSequence extends AbstractParseStringCharsTester {
        private ParseToCharSequence(@Nonnull String chars) {
            super(chars);
            myNoOffsets = CodeInsightUtilCore.parseStringCharacters(chars, null);
            myWithOffsets = CodeInsightUtilCore.parseStringCharacters(chars, mySourceOffsets);
        }
    }

    private class ParseToStringBuilder extends AbstractParseStringCharsTester {
        private ParseToStringBuilder(@Nonnull String chars) {
            super(chars);
            StringBuilder out = new StringBuilder();
            myNoOffsets = CodeInsightUtilCore.parseStringCharacters(chars, out, null) ? out : null;
            out = new StringBuilder();
            myWithOffsets = CodeInsightUtilCore.parseStringCharacters(chars, out, mySourceOffsets) ? out : null;
        }

        private ParseToStringBuilder(@Nonnull String chars, boolean textBlock) {
            super(chars);
            StringBuilder out = new StringBuilder();
            myNoOffsets = CodeInsightUtilCore.parseStringCharacters(chars, out, null, textBlock) ? out : null;
            out = new StringBuilder();
            myWithOffsets = CodeInsightUtilCore.parseStringCharacters(chars, out, mySourceOffsets, textBlock) ? out : null;
        }
    }

    private class GroupParseTester implements ParseStringCharsTester {
        @Nonnull
        private final ParseStringCharsTester[] testers;

        private GroupParseTester(@Nonnull ParseStringCharsTester... testers) {
            this.testers = testers;
        }

        @Override
        public ParseStringCharsTester expectResult(@Nonnull Consumer<AbstractCharSequenceAssert> check) {
            for (ParseStringCharsTester tester : testers) {
                tester.expectResult(check);
            }
            return this;
        }

        @Override
        public ParseStringCharsTester expectOffsets(@Nonnull int[] expectedOffsets) {
            for (ParseStringCharsTester tester : testers) {
                tester.expectOffsets(expectedOffsets);
            }
            return this;
        }
    }
}
