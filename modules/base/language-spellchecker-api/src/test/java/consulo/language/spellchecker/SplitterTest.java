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
package consulo.language.spellchecker;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.component.ProcessCanceledException;
import consulo.document.util.TextRange;
import consulo.language.spellcheker.tokenizer.splitter.*;
import consulo.util.io.StreamUtil;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SplitterTest {
    @Test
    public void testSplitSimpleCamelCase() {
        String text = "simpleCamelCase";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "simple", "Camel", "Case");
    }

    @Test
    public void testSplitCamelCaseWithUpperCasedWord() {
        String text = "camelCaseJSP";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "camel", "Case");
    }

    @Test
    public void testArrays() {
        String text = "Token[]";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "Token");
    }

    @Test
    public void testIdentifierInSingleQuotes() {
        String text = "'fill'";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "fill");
    }

    @Test
    public void testWordsInSingleQuotesWithSep() {
        String text = "'test-something'";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "test", "something");
    }

    @Test
    public void testComplexWordsInQuotes() {
        String text = "\"test-customer's'\"";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "test", "customer's");
    }

    @Test
    public void testCapitalizedWithShortWords() {
        String text = "IntelliJ";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "Intelli");
    }

    @Test
    public void testWords() {
        String text = "first-last";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "first", "last");
    }

    @Test
    public void testCapitalizedWithShortAndLongWords() {
        String text = "IntelliJTestTest";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "Intelli", "Test", "Test");
    }

    @Test
    public void testWordWithApostrophe1() {
        String text = "don't check";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "don't", "check");
    }

    @Test
    public void testHexInPlainText() {
        String text = "some text 0xacvfgt";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "some", "text");
    }

    @Test
    public void testHexInStringLiteral() {
        String text = "qwerty 0x12acfgt test";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "qwerty", "test");
    }


    @Test
    public void testHex() {
        String text = "0xacvfgt";
        correctListToCheck(WordTokenSplitter.getInstance(), text);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testCheckXmlIgnored() {
        String text = "abcdef" + new String(new char[]{0xDC00}) + "test";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text);
    }

    @Test
    public void testIdentifiersWithNumbers() {
        String text = "result1";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "result");
    }

    @Test
    public void testIdentifiersWithNumbersInside() {
        String text = "result1result";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "result", "result");
    }

    @Test
    public void testWordWithApostrophe2() {
        String text = "customers'";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "customers");
    }

    @Test
    public void testWordWithApostrophe3() {
        String text = "customer's";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "customer's");
    }

    @Test
    public void testWordWithApostrophe4() {
        String text = "we'll";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "we'll");
    }

    @Test
    public void testWordWithApostrophe5() {
        String text = "I'm you're we'll";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "you're", "we'll");
    }

    @Test
    public void testConstantName() {
        String text = "TEST_CONSTANT";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "TEST", "CONSTANT");
    }

    @Test
    public void testLongConstantName() {
        String text = "TEST_VERY_VERY_LONG_AND_COMPLEX_CONSTANT";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "TEST", "VERY", "VERY", "LONG", "COMPLEX", "CONSTANT");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testJavaComments() {
        String text = "/*special symbols*/";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "special", "symbols");

        text = "// comment line which spell check works: misttake";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "comment", "line", "which", "spell", "check", "works", "misttake");

        text = "// comment line which spell check not works: misttake";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "comment", "line", "which", "spell", "check", "works", "misttake");
    }

    @Test
    public void testXmlComments() {
        String text = "<!--special symbols-->";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "special", "symbols");
    }

    @Test
    public void testCamelCaseInXmlComments() {
        String text = "<!--specialCase symbols-->";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "special", "Case", "symbols");
    }

    @Test
    public void testWordsWithNumbers() {
        String text = "testCamelCase123";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "test", "Camel", "Case");
    }

    @Test
    public void testCommentsWithWordsWithNumbers() {
        String text = "<!--specialCase456 symbols-->";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "special", "Case", "symbols");
    }

    @Test
    public void testCommentsWithAbr() {
        String text = "<!--JSPTestClass-->";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "Test", "Class");
    }

    @Test
    public void testStringLiterals() {
        String text = "test\ntest\n";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "test", "test");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testCommentWithHtml() {
        String text = "<!--<li>something go here</li> <li>next content</li> foooo barrrr <p> text -->";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "something", "here", "next", "content", "foooo", "barrrr", "text");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testCommentWithHtmlTagsAndAtr() {
        String text = "<!-- <li style='color:red;'>something go here</li> foooo <li style='color:red;'>barrrr</li> <p> text text -->";
        correctListToCheck(CommentTokenSplitter.getInstance(), text, "something", "here", "foooo", "barrrr", "text", "text");
    }

    @Test
    public void testSpecial() {
        String text = "test &nbsp; test";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "test", "test");
    }

    @Test
    public void testColorUC() {
        String text = "#AABBFF";
        correctListToCheck(WordTokenSplitter.getInstance(), text);
    }

    @Test
    public void testColorUCSC() {
        String text = "#AABBFF;";
        correctListToCheck(WordTokenSplitter.getInstance(), text);
    }

    @Test
    public void testColorUCSurrounded() {
        String text = "\"#AABBFF\"";
        correctListToCheck(WordTokenSplitter.getInstance(), text);
    }

    @Test
    public void testColorLC() {
        String text = "#fff";
        correctListToCheck(TextTokenSplitter.getInstance(), text);
    }

    @Test
    public void testTooShort() {
        String text = "bgColor carLight";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "Color", "Light");

    }

    @Test
    public void testPhpVariableCorrectSimple() {
        String text = "$this";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "this");
    }

    @Test
    public void testPhpVariableCorrect() {
        String text = "$this_this$this";
        correctListToCheck(IdentifierTokenSplitter.getInstance(), text, "this", "this", "this");
    }

    @Test
    public void testEmail() {
        String text = "some text with email (shkate.test@gmail.com) inside";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "some", "text", "with", "email", "inside");
    }

    @Test
    public void testEmailOnly() {
        String text = "shkate123-\u00DC.test@gmail.com";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text);
    }

    @Test
    public void testUrl() {
        String text = "http://www.jetbrains.com/idea";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testUrlThenSpaces() {
        String text = "http://www.jetbrains.com/idea asdasdasd sdfsdf";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "asdasdasd", "sdfsdf");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordBeforeDelimiter() {
        String text = "badd,";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "badd");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordAfterDelimiter() {
        String text = ",badd";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "badd");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordInCapsBeforeDelimiter() {
        String text = "BADD,";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "BADD");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordInCapsAfterDelimiter() {
        String text = ",BADD";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "BADD");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordInCapsAfterDelimiter2() {
        String text = "BADD;";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "BADD");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordInCapsAfterDelimiter3() {
        String text = ";BADD;";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "BADD");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordWithUmlauts() {
        String text = "rechtsb\u00FCndig";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, text);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordUpperCasedWithUmlauts() {
        String text = "RECHTSB\u00DCNDIG";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, text);
    }

    @Test
    public void testCommaSeparatedList() {
        String text = "properties,test,properties";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "properties", "test", "properties");
    }

    @Test
    public void testSemicolonSeparatedList() {
        String text = "properties;test;properties";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, "properties", "test", "properties");
    }

    @Test
    public void testProperties1() {
        String text = "properties.test.properties";
        correctListToCheck(PropertiesTokenSplitter.getInstance(), text, "properties", "test", "properties");
    }

    @Test
    public void testPropertiesWithCamelCase() {
        String text = "upgrade.testCommit.propertiesSomeNews";
        correctListToCheck(PropertiesTokenSplitter.getInstance(), text, "upgrade", "test", "Commit", "properties", "Some", "News");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testWordUpperCasedWithUmlautsInTheBeginning() {
        String text = "\u00DCNDIG";
        correctListToCheck(PlainTextTokenSplitter.getInstance(), text, text);
    }

    @Test
    public void testTCData() throws IOException {
        InputStream stream = SplitterTest.class.getResourceAsStream("contents.txt");
        String text = StreamUtil.readText(stream, StandardCharsets.UTF_8);
        List<String> words = wordsToCheck(PlainTextTokenSplitter.getInstance(), text);
        assertEquals(0, words.size());
    }

    private static List<String> wordsToCheck(TokenSplitter splitter, String text) {
        List<String> words = new ArrayList<>();
        SplitContext context = new BaseSplitContext(
            text,
            textRange -> words.add(textRange.substring(text)),
            new ProgressIndicatorProvider() {
                @Override
                public ProgressIndicator getProgressIndicator() {
                    return null;
                }

                @Override
                protected void doCheckCanceled() throws ProcessCanceledException {
                }
            }
        );
        splitter.split(context, TextRange.allOf(text));
        return words;
    }

    private static void correctListToCheck(TokenSplitter splitter, String text, @Nonnull String... expected) {
        assertThat(wordsToCheck(splitter, text))
            .isEqualTo(Arrays.asList(expected));
    }
}
