// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.lang;

import consulo.util.lang.function.TripleFunction;
import consulo.util.lang.internal.NaturalComparator;
import consulo.util.lang.internal.Verifier;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Eugene Zhuravlev
 * @author UNV
 */
public class StringUtilTest {
    @Test
    public void testTrimLeadingChar() {
        doTestTrimLeading("", "");
        doTestTrimLeading("", " ");
        doTestTrimLeading("", "    ");
        doTestTrimLeading("a  ", "a  ");
        doTestTrimLeading("a  ", "  a  ");
    }

    @Test
    public void testTrimTrailingChar() {
        doTestTrimTrailing("", "");
        doTestTrimTrailing("", " ");
        doTestTrimTrailing("", "    ");
        doTestTrimTrailing("  a", "  a");
        doTestTrimTrailing("  a", "  a  ");
    }

    private static void doTestTrimLeading(@Nonnull String expected, @Nonnull String string) {
        assertThat(StringUtil.trimLeading(string)).isEqualTo(expected);
        assertThat(StringUtil.trimLeading(string, ' ')).isEqualTo(expected);
//        assertThat(StringUtil.trimLeading(new StringBuilder(string), ' ').toString()).isEqualTo(expected);
    }

    private static void doTestTrimTrailing(@Nonnull String expected, @Nonnull String string) {
        assertThat(StringUtil.trimTrailing(string)).isEqualTo(expected);
        assertThat(StringUtil.trimTrailing(string, ' ')).isEqualTo(expected);
        assertThat(StringUtil.trimTrailing(new StringBuilder(string), ' ').toString()).isEqualTo(expected);
    }

//    @Test
//    public void doTestTrimCharSequence() {
//        assertThat(StringUtil.trim((CharSequence) "").toString()).isEqualTo("");
//        assertThat(StringUtil.trim((CharSequence) " ").toString()).isEqualTo("");
//        assertThat(StringUtil.trim((CharSequence) " \n\t\r").toString()).isEqualTo("");
//        assertThat(StringUtil.trim((CharSequence) "a").toString()).isEqualTo("a");
//        assertThat(StringUtil.trim((CharSequence) " a").toString()).isEqualTo("a");
//        assertThat(StringUtil.trim((CharSequence) "bc ").toString()).isEqualTo("bc");
//        assertThat(StringUtil.trim((CharSequence) " b a c   ").toString()).isEqualTo("b a c");
//    }

    @Test
    public void testToUpperCase() {
        assertThat(StringUtil.toUpperCase('/')).isEqualTo('/');
        assertThat(StringUtil.toUpperCase(':')).isEqualTo(':');
        assertThat(StringUtil.toUpperCase('a')).isEqualTo('A');
        assertThat(StringUtil.toUpperCase('A')).isEqualTo('A');
        assertThat(StringUtil.toUpperCase('k')).isEqualTo('K');
        assertThat(StringUtil.toUpperCase('K')).isEqualTo('K');

        assertThat(StringUtil.toUpperCase(Character.toLowerCase('\u2567'))).isEqualTo('\u2567');
    }

    @Test
    public void testToUpperCaseGeneric() {
        for (char ch = 0; ch < Character.MAX_VALUE; ch++) {
            char upperCaseCh = Character.toUpperCase(ch);
            assertThat(StringUtil.toUpperCase(ch))
                .withFailMessage("Optimized StringUtil.toUpperCase(" + ch + ") must be == Character.toUpperCase(ch)[=" + upperCaseCh + "]")
                .isEqualTo(upperCaseCh);
        }
    }

    @Test
    public void testToLowerCase() {
        assertThat(StringUtil.toLowerCase('/')).isEqualTo('/');
        assertThat(StringUtil.toLowerCase(':')).isEqualTo(':');
        assertThat(StringUtil.toLowerCase('a')).isEqualTo('a');
        assertThat(StringUtil.toLowerCase('A')).isEqualTo('a');
        assertThat(StringUtil.toLowerCase('k')).isEqualTo('k');
        assertThat(StringUtil.toLowerCase('K')).isEqualTo('k');

        assertThat(StringUtil.toUpperCase(Character.toLowerCase('\u2567'))).isEqualTo('\u2567');
    }

    @Test
    public void testToLowerCaseGeneric() {
        for (char ch = 0; ch < Character.MAX_VALUE; ch++) {
            char lowerCaseCh = Character.toLowerCase(ch);
            assertThat(StringUtil.toLowerCase(ch))
                .withFailMessage("Optimized StringUtil.toLowerCase(" + ch + ") must be == Character.toLowerCase(ch)[=" + lowerCaseCh + "]")
                .isEqualTo(lowerCaseCh);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testIsEmptyOrSpaces() {
        assertTrue(StringUtil.isEmptyOrSpaces(null));
        assertTrue(StringUtil.isEmptyOrSpaces(""));
        assertTrue(StringUtil.isEmptyOrSpaces("                   "));

        assertFalse(StringUtil.isEmptyOrSpaces("1"));
        assertFalse(StringUtil.isEmptyOrSpaces("         12345          "));
        assertFalse(StringUtil.isEmptyOrSpaces("test"));
    }

    @Test
    public void testSplitWithQuotes() {
        // Merge separators
        assertThat(StringUtil.splitHonorQuotes("aaa bbb   ccc ", ' '))
            .containsExactly("aaa", "bbb", "ccc");
        // Support different quotes
        assertThat(StringUtil.splitHonorQuotes("'aaa' \"bbb\"", ' '))
            .containsExactly("'aaa'", "\"bbb\"");
        // Ignore separators inside quotes
        assertThat(StringUtil.splitHonorQuotes("'a aa' \"bb b\"", ' '))
            .containsExactly("'a aa'", "\"bb b\"");
        // Ignore other quotes inside quotes
        assertThat(StringUtil.splitHonorQuotes("'a\" aa' \"bb 'b\"", ' '))
            .containsExactly("'a\" aa'", "\"bb 'b\"");
        // Escape quotes
        assertThat(StringUtil.splitHonorQuotes("'a \\'aa' \"bb\\\" b\"", ' '))
            .containsExactly("'a \\'aa'", "\"bb\\\" b\"");
        // Unescape escaped quotes
        assertThat(StringUtil.splitHonorQuotes("'a aa\\\\' \"bb b\\\\\"", ' '))
            .containsExactly("'a aa\\\\'", "\"bb b\\\\\"");
        // Escape unescaped quotes
        assertThat(StringUtil.splitHonorQuotes("'a \\\\\\'aa' \"bb \\\\\\\"b\"", ' '))
            .containsExactly("'a \\\\\\'aa'", "\"bb \\\\\\\"b\"");
    }

//    @Test
//    @SuppressWarnings("SpellCheckingInspection")
//    public void testUnPluralize() {
//        // synthetic
//        assertThat(StringUtil.unpluralize("pluralses")).isEqualTo("plurals");
//        assertThat(StringUtil.unpluralize("Inheritses")).isEqualTo("Inherits");
//        assertThat(StringUtil.unpluralize("ss")).isEqualTo("s");
//        assertThat(StringUtil.unpluralize("Is")).isEqualTo("I");
//        assertThat(StringUtil.unpluralize("s")).isNull();
//        assertThat(StringUtil.unpluralize("zs")).isEqualTo("z");
//        // normal
//        assertThat(StringUtil.unpluralize("cases")).isEqualTo("case");
//        assertThat(StringUtil.unpluralize("Indices")).isEqualTo("Index");
//        assertThat(StringUtil.unpluralize("fixes")).isEqualTo("fix");
//        assertThat(StringUtil.unpluralize("men")).isEqualTo("man");
//        assertThat(StringUtil.unpluralize("leaves")).isEqualTo("leaf");
//        assertThat(StringUtil.unpluralize("cookies")).isEqualTo("cookie");
//        assertThat(StringUtil.unpluralize("searches")).isEqualTo("search");
//        assertThat(StringUtil.unpluralize("process")).isEqualTo("process");
//        assertThat(StringUtil.unpluralize("PROPERTIES")).isEqualTo("PROPERTY");
//        assertThat(StringUtil.unpluralize("THESE")).isEqualTo("THIS");
//        assertThat(StringUtil.unpluralize("databases")).isEqualTo("database");
//        assertThat(StringUtil.unpluralize("bases")).isEqualTo("basis");
//    }

//    @Test
//    public void testPluralize() {
//        assertThat(StringUtil.pluralize("value")).isEqualTo("values");
//        assertThat(StringUtil.pluralize("values")).isEqualTo("values");
//        assertThat(StringUtil.pluralize("index")).isEqualTo("indices");
//        assertThat(StringUtil.pluralize("matrix")).isEqualTo("matrices");
//        assertThat(StringUtil.pluralize("fix")).isEqualTo("fixes");
//        assertThat(StringUtil.pluralize("man")).isEqualTo("men");
//        assertThat(StringUtil.pluralize("medium")).isEqualTo("media");
//        assertThat(StringUtil.pluralize("stash")).isEqualTo("stashes");
//        assertThat(StringUtil.pluralize("child")).isEqualTo("children");
//        assertThat(StringUtil.pluralize("leaf")).isEqualTo("leaves");
//        assertThat(StringUtil.pluralize("This")).isEqualTo("These");
//        assertThat(StringUtil.pluralize("cookie")).isEqualTo("cookies");
//        assertThat(StringUtil.pluralize("VaLuE")).isEqualTo("VaLuES");
//        assertThat(StringUtil.pluralize("PLAN")).isEqualTo("PLANS");
//        assertThat(StringUtil.pluralize("stackTraceLineEx")).isEqualTo("stackTraceLineExes");
//        assertThat(StringUtil.pluralize("schema")).isEqualTo("schemas"); // anglicized version
//        assertThat(StringUtil.pluralize("PROPERTY")).isEqualTo("PROPERTIES");
//        assertThat(StringUtil.pluralize("THIS")).isEqualTo("THESE");
//        assertThat(StringUtil.pluralize("database")).isEqualTo("databases");
//        assertThat(StringUtil.pluralize("base")).isEqualTo("bases");
//        assertThat(StringUtil.pluralize("basis")).isEqualTo("bases");
//    }

    @Test
    public void testStartsWithConcatenation() {
        assertTrue(StringUtil.startsWithConcatenation("something.with.dot", "something", "."));
        assertTrue(StringUtil.startsWithConcatenation("something.with.dot", "", "something."));
        assertTrue(StringUtil.startsWithConcatenation("something.", "something", "."));
        assertTrue(StringUtil.startsWithConcatenation("something", "something", "", "", ""));
        assertFalse(StringUtil.startsWithConcatenation("something", "something", "", "", "."));
        assertFalse(StringUtil.startsWithConcatenation("some", "something", ""));
    }

    @Test
    public void testNaturalCompareTransitivity() {
        String s1 = "#";
        String s2 = "0b";
        String s3 = " 0b";
        assertThat(StringUtil.naturalCompare(s1, s2)).isLessThan(0);
        assertThat(StringUtil.naturalCompare(s2, s3)).isLessThan(0);
        assertThat(StringUtil.naturalCompare(s1, s3))
            .withFailMessage("non-transitive")
            .isLessThan(0);
    }

//    @Test
//    public void testNaturalCompareTransitivityProperty() {
//        PropertyChecker.forAll(Surrogate.Generator.listsOf(Surrogate.Generator.stringsOf("ab01()_# ")), l -> {
//            List<String> sorted = ContainerUtil.sorted(l, StringUtil::naturalCompare);
//            for (int i = 0; i < sorted.size(); i++) {
//                for (int j = i + 1; j < sorted.size(); j++) {
//                    if (StringUtil.naturalCompare(sorted.get(i), sorted.get(j)) > 0) return false;
//                    if (StringUtil.naturalCompare(sorted.get(j), sorted.get(i)) < 0) return false;
//                }
//            }
//            return true;
//        });
//    }

    @Test
    public void testNaturalCompareStability() {
        assertThat(StringUtil.naturalCompare("01a1", "1a01")).isNotSameAs(StringUtil.naturalCompare("1a01", "01a1"));
        assertThat(StringUtil.naturalCompare("#01A", "# 1A")).isNotSameAs(StringUtil.naturalCompare("# 1A", "#01A"));
        assertThat(StringUtil.naturalCompare("aA", "aa")).isNotSameAs(StringUtil.naturalCompare("aa", "aA"));
    }

    @Test
    public void testNaturalCompare() {
        var numbers = Arrays.asList("1a000001", "000001a1", "001a0001", "0001A001", "00001a01", "01a00001");
        numbers.sort(NaturalComparator.INSTANCE);
        assertThat(numbers).containsExactly("1a000001", "01a00001", "001a0001", "0001A001", "00001a01", "000001a1");

        var test = Arrays.asList("test011", "test10", "test10a", "test010");
        test.sort(NaturalComparator.INSTANCE);
        assertThat(test).containsExactly("test10", "test10a", "test010", "test011");

        var strings = Arrays.asList(
            "Test99", "tes0", "test0", "testing", "test", "test99", "test011", "test1", "test 3", "test2",
            "test10a", "test10", "1.2.10.5", "1.2.9.1"
        );
        strings.sort(NaturalComparator.INSTANCE);
        assertThat(strings).containsExactly(
            "1.2.9.1", "1.2.10.5", "tes0", "test", "test0", "test1", "test2", "test 3", "test10", "test10a",
            "test011", "Test99", "test99", "testing"
        );

        var strings2 = Arrays.asList("t1", "t001", "T2", "T002", "T1", "t2");
        strings2.sort(NaturalComparator.INSTANCE);
        assertThat(strings2).containsExactly("T1", "t1", "t001", "T2", "t2", "T002");

        assertThat(StringUtil.naturalCompare("7403515080361171695", "07403515080361171694")).isPositive();
        assertThat(StringUtil.naturalCompare("_firstField", "myField1")).isNegative();

        var strings3 = Arrays.asList("C148A_InsomniaCure", "C148B_Escape", "C148C_TersePrincess", "C148D_BagOfMice", "C148E_Porcelain");
        strings3.sort(NaturalComparator.INSTANCE);
        assertThat(strings3).containsExactly(
            "C148A_InsomniaCure",
            "C148B_Escape",
            "C148C_TersePrincess",
            "C148D_BagOfMice",
            "C148E_Porcelain"
        );

        var l = Arrays.asList("a0002", "a0 2", "a001");
        l.sort(NaturalComparator.INSTANCE);
        assertThat(l).containsExactly("a0 2", "a001", "a0002");
    }

//    @Test
//    public void testFormatLinks() {
//        assertEquals("<a href=\"http://a-b+c\">http://a-b+c</a>", StringUtil.formatLinks("http://a-b+c"));
//    }

    @Test
    public void testCopyHeapCharBuffer() {
        String s = "abc.d";
        CharBuffer buffer = CharBuffer.allocate(s.length());
        buffer.append(s);
        buffer.rewind();

        assertThat(CharArrayUtil.fromSequenceWithoutCopying(buffer)).isNotNull();
        assertThat(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(0, 5))).isNotNull();
        assertThat(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(1, 5))).isNull();
        assertThat(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(1, 2))).isNull();
    }

//    @Test
//    public void testTitleCase() {
//        assertThat(StringUtil.wordsToBeginFromUpperCase("Couldn't connect to debugger")).isEqualTo("Couldn't Connect to Debugger");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("Let's make abbreviations like I18n, SQL and CSS"))
//            .isEqualTo("Let's Make Abbreviations Like I18n, SQL and CSS");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("1s_t _how a&re mn_emonics &handled, or aren't they"))
//            .isEqualTo("1s_t _How A&re Mn_emonics &Handled, or Aren't They");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("a good steak should not be this hard to come by"))
//            .isEqualTo("A Good Steak Should Not Be This Hard to Come By");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("twenty-one quick-fixes")).isEqualTo("Twenty-One Quick-Fixes");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("it's not a question of if, but when")).isEqualTo(
//            "It's Not a Question of If, but When");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("scroll to the end. a good steak should not be this hard to come by."))
//            .isEqualTo("Scroll to the End. A Good Steak Should Not Be This Hard to Come By.");
//    }

//    @Test
//    public void testSentenceCapitalization() {
//        assertThat(StringUtil.wordsToBeginFromLowerCase("Couldn't Connect To Debugger")).isEqualTo("couldn't connect to debugger");
//        assertThat(StringUtil.wordsToBeginFromLowerCase("Let's Make Abbreviations Like I18n, SQL and CSS S SQ Sq"))
//            .isEqualTo("let's make abbreviations like I18n, SQL and CSS s SQ sq");
//    }

//    @Test
//    public void testCapitalizeWords() {
//        assertThat(StringUtil.capitalizeWords("AspectJ (syntax highlighting only)", true)).isEqualTo("AspectJ (Syntax Highlighting Only)");
//    }

    @Test
    public void testEscapeStringCharacters() {
        assertThat(StringUtil.escapeStringCharacters(3, "\\\"\n", "\"", false, new StringBuilder()).toString()).isEqualTo("\\\"\\n");
        assertThat(StringUtil.escapeStringCharacters(2, "\"\n", "\"", false, new StringBuilder()).toString()).isEqualTo("\\\"\\n");
        assertThat(StringUtil.escapeStringCharacters(3, "\\\"\n", "\"", true, new StringBuilder()).toString()).isEqualTo("\\\\\\\"\\n");
    }

    @Test
    public void testEscapeSlashes() {
        assertThat(StringUtil.escapeSlashes("/")).isEqualTo("\\/");
        assertThat(StringUtil.escapeSlashes("foo/bar\\foo/")).isEqualTo("foo\\/bar\\foo\\/");

        assertThat(StringUtil.escapeBackSlashes("\\\\server\\share\\extension.crx")).isEqualTo("\\\\\\\\server\\\\share\\\\extension.crx");
    }

    @Test
    public void testEscapeQuotes() {
        assertThat(StringUtil.escapeQuotes("\"")).isEqualTo("\\\"");
        assertThat(StringUtil.escapeQuotes("foo\"bar'\"")).isEqualTo("foo\\\"bar'\\\"");
    }

    @Test
    public void testUnquote() {
        assertThat(StringUtil.unquoteString("")).isEqualTo("");
        assertThat(StringUtil.unquoteString("\"")).isEqualTo("\"");
        assertThat(StringUtil.unquoteString("\"\"")).isEqualTo("");
        assertThat(StringUtil.unquoteString("\"\"\"")).isEqualTo("\"");
        assertThat(StringUtil.unquoteString("\"foo\"")).isEqualTo("foo");
        assertThat(StringUtil.unquoteString("\"foo")).isEqualTo("\"foo");
        assertThat(StringUtil.unquoteString("foo\"")).isEqualTo("foo\"");
        assertThat(StringUtil.unquoteString("")).isEqualTo("");
        assertThat(StringUtil.unquoteString("'")).isEqualTo("'");
        assertThat(StringUtil.unquoteString("''")).isEqualTo("");
        assertThat(StringUtil.unquoteString("'''")).isEqualTo("'");
        assertThat(StringUtil.unquoteString("'foo'")).isEqualTo("foo");
        assertThat(StringUtil.unquoteString("'foo")).isEqualTo("'foo");
        assertThat(StringUtil.unquoteString("foo'")).isEqualTo("foo'");

        assertThat(StringUtil.unquoteString("'\"")).isEqualTo("'\"");
        assertThat(StringUtil.unquoteString("\"'")).isEqualTo("\"'");
        assertThat(StringUtil.unquoteString("\"foo'")).isEqualTo("\"foo'");
    }

    @SuppressWarnings("SSBasedInspection")
    @Test
    public void testStripQuotesAroundValue() {
        assertThat(StringUtil.stripQuotesAroundValue("")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("'")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("\"")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("''")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("\"\"")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("'\"")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("'foo'")).isEqualTo("foo");
        assertThat(StringUtil.stripQuotesAroundValue("'foo")).isEqualTo("foo");
        assertThat(StringUtil.stripQuotesAroundValue("foo'")).isEqualTo("foo");
        assertThat(StringUtil.stripQuotesAroundValue("'f'o'o'")).isEqualTo("f'o'o");
        assertThat(StringUtil.stripQuotesAroundValue("\"f\"o'o'")).isEqualTo("f\"o'o");
        assertThat(StringUtil.stripQuotesAroundValue("f\"o'o")).isEqualTo("f\"o'o");
        assertThat(StringUtil.stripQuotesAroundValue("\"\"'f\"o'o\"\"")).isEqualTo("\"'f\"o'o\"");
        assertThat(StringUtil.stripQuotesAroundValue("'''f\"o'o'''")).isEqualTo("''f\"o'o''");
        assertThat(StringUtil.stripQuotesAroundValue("foo' 'bar")).isEqualTo("foo' 'bar");
    }

    @Test
    public void testUnquoteWithQuotationChar() {
        assertThat(StringUtil.unquoteString("", '|')).isEqualTo("");
        assertThat(StringUtil.unquoteString("|", '|')).isEqualTo("|");
        assertThat(StringUtil.unquoteString("||", '|')).isEqualTo("");
        assertThat(StringUtil.unquoteString("|||", '|')).isEqualTo("|");
        assertThat(StringUtil.unquoteString("|foo|", '|')).isEqualTo("foo");
        assertThat(StringUtil.unquoteString("|foo", '|')).isEqualTo("|foo");
        assertThat(StringUtil.unquoteString("foo|", '|')).isEqualTo("foo|");
    }

    @Test
    public void testIsQuotedString() {
        assertFalse(StringUtil.isQuotedString(""));
        assertFalse(StringUtil.isQuotedString("'"));
        assertFalse(StringUtil.isQuotedString("\""));
        assertTrue(StringUtil.isQuotedString("\"\""));
        assertTrue(StringUtil.isQuotedString("''"));
        assertTrue(StringUtil.isQuotedString("'ab'"));
        assertTrue(StringUtil.isQuotedString("\"foo\""));
    }

    @Test
    public void testJoin() {
        assertThat(StringUtil.join(List.of(), ",")).isEqualTo("");
        assertThat(StringUtil.join(List.of("qqq"), ",")).isEqualTo("qqq");
        assertThat(StringUtil.join(Collections.singletonList(null), ",")).isEqualTo("");
        assertThat(StringUtil.join(List.of("a", "b"), ",")).isEqualTo("a,b");
        assertThat(StringUtil.join(List.of("foo", "", "bar"), ",")).isEqualTo("foo,,bar");
        assertThat(StringUtil.join(new String[]{"foo", "", "bar"}, ",")).isEqualTo("foo,,bar");
    }

//    @Test
//    public void testSplitByLineKeepingSeparators() {
//        assertThat(StringUtil.splitByLinesKeepSeparators("")).containsExactly("");
//        assertThat(StringUtil.splitByLinesKeepSeparators("aa")).containsExactly("aa");
//        assertThat(StringUtil.splitByLinesKeepSeparators("\n\naa\n\nbb\ncc\n\n"))
//            .containsExactly("\n", "\n", "aa\n", "\n", "bb\n", "cc\n", "\n");
//
//        assertThat(StringUtil.splitByLinesKeepSeparators("\r\r\n\r")).containsExactly("\r", "\r\n", "\r");
//        assertThat(StringUtil.splitByLinesKeepSeparators("\r\n\r\r\n")).containsExactly("\r\n", "\r", "\r\n");
//
//        assertThat(StringUtil.splitByLinesKeepSeparators("\n\r\n\n\r\n\r\raa\rbb\r\ncc\n\rdd\n\n\r\n\r"))
//            .containsExactly("\n", "\r\n", "\n", "\r\n", "\r", "\r", "aa\r", "bb\r\n", "cc\n", "\r", "dd\n", "\n", "\r\n", "\r");
//    }

    @Test
    @SuppressWarnings("StringToUpperCaseOrToLowerCaseWithoutLocale")
    public void testReplaceReturnReplacementIfTextEqualsToReplacedText() {
        String str = "/tmp";
        assertThat(StringUtil.replace(
            "$PROJECT_FILE$",
            "$PROJECT_FILE$".toLowerCase().toUpperCase() /* ensure new String instance */,
            str
        )).isSameAs(str);
    }

    @Test
    public void testReplace() {
        assertThat(StringUtil.replace("$PROJECT_FILE$/filename", "$PROJECT_FILE$", "/tmp")).isEqualTo("/tmp/filename");
    }

    @Test
    public void testReplaceListOfChars() {
        assertThat(StringUtil.replace("$PROJECT_FILE$/filename", List.of("$PROJECT_FILE$"), List.of("/tmp")))
            .isEqualTo("/tmp/filename");
        assertThat(StringUtil.replace("/someTextBefore/$PROJECT_FILE$/filename", List.of("$PROJECT_FILE$"), List.of("tmp")))
            .isEqualTo("/someTextBefore/tmp/filename");
    }

    @Test
    public void testReplaceReturnTheSameStringIfNothingToReplace() {
        String str = "/tmp/filename";
        assertThat(StringUtil.replace(str, "$PROJECT_FILE$/filename", "$PROJECT_FILE$")).isSameAs(str);
    }

    @Test
    public void testEqualsIgnoreWhitespaces() {
        assertTrue(StringUtil.equalsIgnoreWhitespaces(null, null));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("", null));

        assertTrue(StringUtil.equalsIgnoreWhitespaces("", ""));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("\n\t ", ""));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("", "\t\n \n\t"));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("\t", "\n"));

        assertTrue(StringUtil.equalsIgnoreWhitespaces("x", " x"));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("x", "x "));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("x\n", "x"));

        assertTrue(StringUtil.equalsIgnoreWhitespaces("abc", "a\nb\nc\n"));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("x y x", "x y x"));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("xyx", "x y x"));

        assertFalse(StringUtil.equalsIgnoreWhitespaces("x", "\t\n "));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("", " x "));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("", "x "));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("", " x"));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("xyx", "xxx"));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("xyx", "xYx"));
    }

    @Test
    public void testStringHashCodeIgnoreWhitespaces() {
        assertTrue(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces(""), StringUtil.stringHashCodeIgnoreWhitespaces("")));
        assertTrue(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("\n\t "), StringUtil.stringHashCodeIgnoreWhitespaces("")));
        assertTrue(Comparing.equal(
            StringUtil.stringHashCodeIgnoreWhitespaces(""),
            StringUtil.stringHashCodeIgnoreWhitespaces("\t\n \n\t")
        ));
        assertTrue(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("\t"), StringUtil.stringHashCodeIgnoreWhitespaces("\n")));

        assertTrue(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("x"), StringUtil.stringHashCodeIgnoreWhitespaces(" x")));
        assertTrue(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("x"), StringUtil.stringHashCodeIgnoreWhitespaces("x ")));
        assertTrue(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("x\n"), StringUtil.stringHashCodeIgnoreWhitespaces("x")));

        assertTrue(Comparing.equal(
            StringUtil.stringHashCodeIgnoreWhitespaces("abc"),
            StringUtil.stringHashCodeIgnoreWhitespaces("a\nb\nc\n")
        ));
        assertTrue(Comparing.equal(
            StringUtil.stringHashCodeIgnoreWhitespaces("x y x"),
            StringUtil.stringHashCodeIgnoreWhitespaces("x y x")
        ));
        assertTrue(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("xyx"), StringUtil.stringHashCodeIgnoreWhitespaces("x y x")));

        assertFalse(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("x"), StringUtil.stringHashCodeIgnoreWhitespaces("\t\n ")));
        assertFalse(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces(""), StringUtil.stringHashCodeIgnoreWhitespaces(" x ")));
        assertFalse(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces(""), StringUtil.stringHashCodeIgnoreWhitespaces("x ")));
        assertFalse(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces(""), StringUtil.stringHashCodeIgnoreWhitespaces(" x")));
        assertFalse(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("xyx"), StringUtil.stringHashCodeIgnoreWhitespaces("xxx")));
        assertFalse(Comparing.equal(StringUtil.stringHashCodeIgnoreWhitespaces("xyx"), StringUtil.stringHashCodeIgnoreWhitespaces("xYx")));
    }

    @Test
    public void testContains() {
        assertTrue(StringUtil.contains("1", "1"));
        assertFalse(StringUtil.contains("1", "12"));
        assertTrue(StringUtil.contains("12", "1"));
        assertTrue(StringUtil.contains("12", "2"));
    }

    @Test
    public void testCompareCharSequence() {
        TripleFunction<CharSequence, CharSequence, Boolean, Boolean> assertPrecedence =
            (lesser, greater, ignoreCase) -> {
                assertThat(StringUtil.compare(lesser, greater, ignoreCase)).isLessThan(0);
                assertThat(StringUtil.compare(greater, lesser, ignoreCase)).isGreaterThan(0);
                return true;
            };
        TripleFunction<CharSequence, CharSequence, Boolean, Boolean> assertEquality =
            (lesser, greater, ignoreCase) -> {
                assertThat(StringUtil.compare(lesser, greater, ignoreCase)).isEqualTo(0);
                assertThat(StringUtil.compare(greater, lesser, ignoreCase)).isEqualTo(0);
                return true;
            };

        assertPrecedence.fun("A", "b", true);
        assertPrecedence.fun("a", "aa", true);
        assertPrecedence.fun("abb", "abC", true);

        assertPrecedence.fun("A", "a", false);
        assertPrecedence.fun("Aa", "a", false);
        assertPrecedence.fun("a", "aa", false);
        assertPrecedence.fun("-", "A", false);

        assertEquality.fun("a", "A", true);
        assertEquality.fun("aa12b", "Aa12B", true);

        assertEquality.fun("aa12b", "aa12b", false);
    }

//    @Test
//    public void testDetectSeparators() {
//        assertThat(StringUtil.detectSeparators("")).isNull();
//        assertThat(StringUtil.detectSeparators("asd")).isNull();
//        assertThat(StringUtil.detectSeparators("asd\t")).isNull();
//
//        assertEquals(LineSeparator.Unix, StringUtil.detectSeparators("asd\n"));
//        assertEquals(LineSeparator.Unix, StringUtil.detectSeparators("asd\nads\r"));
//        assertEquals(LineSeparator.Unix, StringUtil.detectSeparators("asd\nads\n"));
//
//        assertEquals(LineSeparator.Macintosh, StringUtil.detectSeparators("asd\r"));
//        assertEquals(LineSeparator.Macintosh, StringUtil.detectSeparators("asd\rads\r"));
//        assertEquals(LineSeparator.Macintosh, StringUtil.detectSeparators("asd\rads\n"));
//
//        assertEquals(LineSeparator.Windows, StringUtil.detectSeparators("asd\r\n"));
//        assertEquals(LineSeparator.Windows, StringUtil.detectSeparators("asd\r\nads\r"));
//        assertEquals(LineSeparator.Windows, StringUtil.detectSeparators("asd\r\nads\n"));
//    }

//    @Test
//    public void testFindStartingLineSeparator() {
//        assertNull(StringUtil.getLineSeparatorAt("", -1));
//        assertNull(StringUtil.getLineSeparatorAt("", 0));
//        assertNull(StringUtil.getLineSeparatorAt("", 1));
//        assertNull(StringUtil.getLineSeparatorAt("\nHello", -1));
//        assertNull(StringUtil.getLineSeparatorAt("\nHello", 1));
//        assertNull(StringUtil.getLineSeparatorAt("\nH\rel\nlo", 6));
//
//        assertEquals(LineSeparator.Unix, StringUtil.getLineSeparatorAt("\nHello", 0));
//        assertEquals(LineSeparator.Unix, StringUtil.getLineSeparatorAt("\nH\rel\nlo", 5));
//        assertEquals(LineSeparator.Unix, StringUtil.getLineSeparatorAt("Hello\n", 5));
//
//        assertEquals(LineSeparator.Macintosh, StringUtil.getLineSeparatorAt("\rH\r\nelp", 0));
//        assertEquals(LineSeparator.Macintosh, StringUtil.getLineSeparatorAt("Hello\r", 5));
//        assertEquals(LineSeparator.Macintosh, StringUtil.getLineSeparatorAt("Hello\b\r", 6));
//
//        assertEquals(LineSeparator.Windows, StringUtil.getLineSeparatorAt("\rH\r\nelp", 2));
//        assertEquals(LineSeparator.Windows, StringUtil.getLineSeparatorAt("\r\nH\r\nelp", 0));
//        assertEquals(LineSeparator.Windows, StringUtil.getLineSeparatorAt("\r\nH\r\nelp\r\n", 8));
//    }

    @Test
    public void testFormatFileSize() {
        assertFileSizeFormat(0, "0 B");
        assertFileSizeFormat(1, "1 B");
        assertFileSizeFormat(Integer.MAX_VALUE, "2.15 GB");
        assertFileSizeFormat(Long.MAX_VALUE, "9.22 EB");

        assertFileSizeFormat(60_100, "60.1 kB");

        assertFileSizeFormat(1_234, "1.23 kB");
        assertFileSizeFormat(12_345, "12.35 kB");
        assertFileSizeFormat(123_456, "123.46 kB");
        assertFileSizeFormat(1_234_567, "1.23 MB");
        assertFileSizeFormat(12_345_678, "12.35 MB");
        assertFileSizeFormat(123_456_789, "123.46 MB");
        assertFileSizeFormat(1_234_567_890, "1.23 GB");

        assertFileSizeFormat(999, "999 B");
        assertFileSizeFormat(1000, "1 kB");
        assertFileSizeFormat(999_994, "999.99 kB");
        assertFileSizeFormat(999_995, "1 MB");
        assertFileSizeFormat(999_994_999, "999.99 MB");
        assertFileSizeFormat(999_995_000, "1 GB");
        assertFileSizeFormat(999_994_999_999L, "999.99 GB");
        assertFileSizeFormat(999_995_000_000L, "1 TB");
    }

//    @Test
//    public void testFormatFileSizeFixedPrecision() {
//        assertThat(StringUtil.formatFileSize(10, " ", -1, true)).isEqualTo("10.00 B");
//        assertThat(StringUtil.formatFileSize(100, " ", -1, true)).isEqualTo("100.00 B");
//        assertThat(StringUtil.formatFileSize(1_000, " ", -1, true)).isEqualTo("1.00 kB");
//        assertThat(StringUtil.formatFileSize(10_000, " ", -1, true)).isEqualTo("10.00 kB");
//        assertThat(StringUtil.formatFileSize(100_000, " ", -1, true)).isEqualTo("100.00 kB");
//        assertThat(StringUtil.formatFileSize(1_000_000, " ", -1, true)).isEqualTo("1.00 MB");
//        assertThat(StringUtil.formatFileSize(10_000_000, " ", -1, true)).isEqualTo("10.00 MB");
//        assertThat(StringUtil.formatFileSize(100_000_000, " ", -1, true)).isEqualTo("100.00 MB");
//        assertThat(StringUtil.formatFileSize(1_000_000_000, " ", -1, true)).isEqualTo("1.00 GB");
//    }

    private void assertFileSizeFormat(long sizeBytes, String expectedFormatted) {
        assertThat(StringUtil.formatFileSize(sizeBytes)).isEqualTo(expectedFormatted);
    }

    @Test
    public void testFormatDuration() {
        assertThat(StringUtil.formatDuration(0)).isEqualTo("0 ms");
        assertThat(StringUtil.formatDuration(1)).isEqualTo("1 ms");
        assertThat(StringUtil.formatDuration(1000)).isEqualTo("1 s");
        assertThat(StringUtil.formatDuration(Integer.MAX_VALUE)).isEqualTo("24 d 20 h 31 m 23 s 647 ms");
        assertThat(StringUtil.formatDuration(Integer.MAX_VALUE + 5000000000L)).isEqualTo("82 d 17 h 24 m 43 s 647 ms");

        assertThat(StringUtil.formatDuration(60100)).isEqualTo("1 m 0 s 100 ms");

        assertThat(StringUtil.formatDuration(1234)).isEqualTo("1 s 234 ms");
        assertThat(StringUtil.formatDuration(12345)).isEqualTo("12 s 345 ms");
        assertThat(StringUtil.formatDuration(123456)).isEqualTo("2 m 3 s 456 ms");
        assertThat(StringUtil.formatDuration(1234567)).isEqualTo("20 m 34 s 567 ms");
        assertThat(StringUtil.formatDuration(12345678)).isEqualTo("3 h 25 m 45 s 678 ms");
        assertThat(StringUtil.formatDuration(123456789)).isEqualTo("1 d 10 h 17 m 36 s 789 ms");
        assertThat(StringUtil.formatDuration(1234567890)).isEqualTo("14 d 6 h 56 m 7 s 890 ms");

        assertThat(StringUtil.formatDuration(3378606101L)).isEqualTo("39 d 2 h 30 m 6 s 101 ms");
    }

    @Test
    public void testXmlWrapInCDATA() {
        assertThat(XmlStringUtil.wrapInCDATA("abc")).isEqualTo("<![CDATA[abc]]>");
        assertThat(XmlStringUtil.wrapInCDATA("abc]]>")).isEqualTo("<![CDATA[abc]]]><![CDATA[]>]]>");
        assertThat(XmlStringUtil.wrapInCDATA("abc]]>def")).isEqualTo("<![CDATA[abc]]]><![CDATA[]>def]]>");
        assertThat(XmlStringUtil.wrapInCDATA("123<![CDATA[wow<&>]]>]]><![CDATA[123"))
            .isEqualTo("<![CDATA[123<![CDATA[wow<&>]]]><![CDATA[]>]]]><![CDATA[]><![CDATA[123]]>");
    }

    @Test
    public void testGetPackageName() {
        assertThat(StringUtil.getPackageName("java.lang.String")).isEqualTo("java.lang");
        assertThat(StringUtil.getPackageName("java.util.Map.Entry")).isEqualTo("java.util.Map");
        assertThat(StringUtil.getPackageName("Map.Entry")).isEqualTo("Map");
        assertThat(StringUtil.getPackageName("Number")).isEqualTo("");
    }

    @Test
    public void testIndexOf_1() {
        char[] chars = new char[]{'a', 'b', 'c', 'd', 'a', 'b', 'c', 'd', 'A', 'B', 'C', 'D'};
        assertThat(StringUtil.indexOf(chars, 'c', 0, 12, false)).isEqualTo(2);
        assertThat(StringUtil.indexOf(chars, 'C', 0, 12, false)).isEqualTo(2);
        assertThat(StringUtil.indexOf(chars, 'C', 0, 12, true)).isEqualTo(10);
        assertThat(StringUtil.indexOf(chars, 'c', -42, 99, false)).isEqualTo(2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testIndexOf_2() {
        assertThat(StringUtil.indexOf("axaxa", 'x', 0, 5)).isEqualTo(1);
        assertThat(StringUtil.indexOf("abcd", 'c', -42, 99)).isEqualTo(2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testIndexOf_3() {
        assertThat(StringUtil.indexOf("axaXa", 'x', 0, 5, false)).isEqualTo(1);
        assertThat(StringUtil.indexOf("axaXa", 'X', 0, 5, true)).isEqualTo(3);
        assertThat(StringUtil.indexOf("abcd", 'c', -42, 99, false)).isEqualTo(2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testIndexOfAny() {
        assertThat(StringUtil.indexOfAny("axa", "x", 0, 5)).isEqualTo(1);
        assertThat(StringUtil.indexOfAny("axa", "zx", 0, 5)).isEqualTo(1);
        assertThat(StringUtil.indexOfAny("abcd", "c", -42, 99)).isEqualTo(2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testLastIndexOf() {
        assertThat(StringUtil.lastIndexOf("axaxa", 'x', 0, 2)).isEqualTo(1);
        assertThat(StringUtil.lastIndexOf("axaxa", 'x', 0, 3)).isEqualTo(1);
        assertThat(StringUtil.lastIndexOf("axaxa", 'x', 0, 5)).isEqualTo(3);
        assertThat(StringUtil.lastIndexOf("abcd", 'c', -42, 99)).isEqualTo(2);  // #IDEA-144968
    }

    @Test
    public void testEscapingIllegalXmlChars() {
        for (String s : new String[]{"ab\n\0\r\tde", "\\abc\1\2\3\uFFFFdef"}) {
            String escapedText = XmlStringUtil.escapeIllegalXmlChars(s);
            assertThat(Verifier.checkCharacterData(escapedText)).isNull();
            assertThat(XmlStringUtil.unescapeIllegalXmlChars(escapedText)).isEqualTo(s);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testCountChars() {
        assertThat(StringUtil.countChars("abcdefgh", 'x')).isEqualTo(0);
        assertThat(StringUtil.countChars("abcdefgh", 'd')).isEqualTo(1);
        assertThat(StringUtil.countChars("abcddddefghd", 'd')).isEqualTo(5);
        assertThat(StringUtil.countChars("abcddddefghd", 'd', 4, false)).isEqualTo(4);
        assertThat(StringUtil.countChars("abcddddefghd", 'd', 4, true)).isEqualTo(3);
        assertThat(StringUtil.countChars("abcddddefghd", 'd', 4, 6, false)).isEqualTo(2);
        assertThat(StringUtil.countChars("aaabcddddefghdaaaa", 'a', -20, 20, true)).isEqualTo(3);
        assertThat(StringUtil.countChars("aaabcddddefghdaaaa", 'a', 20, -20, true)).isEqualTo(4);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testSubstringBeforeLast() {
        assertThat(StringUtil.substringBeforeLast("abc", "b")).isEqualTo("a");
        assertThat(StringUtil.substringBeforeLast("ababbccc", "b")).isEqualTo("abab");
        assertThat(StringUtil.substringBeforeLast("abc", "")).isEqualTo("abc");
        assertThat(StringUtil.substringBeforeLast("abc", "1")).isEqualTo("abc");
        assertThat(StringUtil.substringBeforeLast("", "1")).isEqualTo("");
//        assertThat(StringUtil.substringBeforeLast("abc", "b", false)).isEqualTo("a");
//        assertThat(StringUtil.substringBeforeLast("ababbccc", "b", false)).isEqualTo("abab");
//        assertThat(StringUtil.substringBeforeLast("abc", "", false)).isEqualTo("abc");
//        assertThat(StringUtil.substringBeforeLast("abc", "1", false)).isEqualTo("abc");
//        assertThat(StringUtil.substringBeforeLast("", "1", false)).isEqualTo("");
//        assertThat(StringUtil.substringBeforeLast("abc", "b", true)).isEqualTo("ab");
//        assertThat(StringUtil.substringBeforeLast("ababbccc", "b", true)).isEqualTo("ababb");
//        assertThat(StringUtil.substringBeforeLast("abc", "", true)).isEqualTo("abc");
//        assertThat(StringUtil.substringBeforeLast("abc", "1", true)).isEqualTo("abc");
//        assertThat(StringUtil.substringBeforeLast("", "1", true)).isEqualTo("");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testSubstringAfterLast() {
        assertThat(StringUtil.substringAfterLast("abc", "b")).isEqualTo("c");
        assertThat(StringUtil.substringAfterLast("ababbccc", "b")).isEqualTo("ccc");
        assertThat(StringUtil.substringAfterLast("abc", "")).isEqualTo("");
        assertThat(StringUtil.substringAfterLast("abc", "1")).isNull();
        assertThat(StringUtil.substringAfterLast("", "1")).isNull();
    }

//    @Test
//    public void testGetWordIndicesIn() {
//        assertThat(StringUtil.getWordIndicesIn("first second")).containsExactly(new TextRange(0, 5), new TextRange(6, 12));
//        assertThat(StringUtil.getWordIndicesIn(" first second")).containsExactly(new TextRange(1, 6), new TextRange(7, 13));
//        assertThat(StringUtil.getWordIndicesIn(" first second    ")).containsExactly(new TextRange(1, 6), new TextRange(7, 13));
//        assertThat(StringUtil.getWordIndicesIn("first:second")).containsExactly(new TextRange(0, 5), new TextRange(6, 12));
//        assertThat(StringUtil.getWordIndicesIn("first-second")).containsExactly(new TextRange(0, 5), new TextRange(6, 12));
//        assertThat(StringUtil.getWordIndicesIn("first-second", Set.of(' ', '_', '.'))).containsExactly(new TextRange(0, 12));
//        assertThat(StringUtil.getWordIndicesIn("first-second", Set.of('-'))).containsExactly(new TextRange(0, 5), new TextRange(6, 12));
//    }

//    @Test
//    @SuppressWarnings({"SpellCheckingInspection", "NonAsciiCharacters"})
//    public void testIsLatinAlphanumeric() {
//        assertTrue(StringUtil.isLatinAlphanumeric("1234567890"));
//        assertTrue(StringUtil.isLatinAlphanumeric("123abc593"));
//        assertTrue(StringUtil.isLatinAlphanumeric("gwengioewn"));
//        assertTrue(StringUtil.isLatinAlphanumeric("FiwnFWinfs"));
//        assertTrue(StringUtil.isLatinAlphanumeric("b"));
//        assertTrue(StringUtil.isLatinAlphanumeric("1"));
//
//        assertFalse(StringUtil.isLatinAlphanumeric("йцукен"));
//        assertFalse(StringUtil.isLatinAlphanumeric("ЙцуTYuio"));
//        assertFalse(StringUtil.isLatinAlphanumeric("йцу626кен"));
//        assertFalse(StringUtil.isLatinAlphanumeric("12 12"));
//        assertFalse(StringUtil.isLatinAlphanumeric("."));
//        assertFalse(StringUtil.isLatinAlphanumeric("_"));
//        assertFalse(StringUtil.isLatinAlphanumeric("-"));
//        assertFalse(StringUtil.isLatinAlphanumeric("fhu384 "));
//        assertFalse(StringUtil.isLatinAlphanumeric(""));
//        assertFalse(StringUtil.isLatinAlphanumeric(null));
//        assertFalse(StringUtil.isLatinAlphanumeric("'"));
//    }

//    @Test
//    public void testIsShortNameOf() {
//        assertTrue(StringUtil.isShortNameOf("a.b.c", "c"));
//        assertTrue(StringUtil.isShortNameOf("foo", "foo"));
//        assertFalse(StringUtil.isShortNameOf("foo", ""));
//        assertFalse(StringUtil.isShortNameOf("", "foo"));
//        assertFalse(StringUtil.isShortNameOf("a.b.c", "d"));
//        assertFalse(StringUtil.isShortNameOf("x.y.zzz", "zz"));
//        assertFalse(StringUtil.isShortNameOf("x", "a.b.x"));
//    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void startsWith() {
        assertTrue(StringUtil.startsWith("abcdefgh", 5, "fgh"));
        assertTrue(StringUtil.startsWith("abcdefgh", 2, "cde"));
        assertTrue(StringUtil.startsWith("abcdefgh", 0, "abc"));
        assertTrue(StringUtil.startsWith("abcdefgh", 0, "abcdefgh"));
        assertFalse(StringUtil.startsWith("abcdefgh", 5, "cde"));

        assertTrue(StringUtil.startsWith("abcdefgh", 0, ""));
        assertTrue(StringUtil.startsWith("abcdefgh", 4, ""));
        assertTrue(StringUtil.startsWith("abcdefgh", 7, ""));
        assertTrue(StringUtil.startsWith("abcdefgh", 8, ""));

        assertTrue(StringUtil.startsWith("", 0, ""));

        assertFalse(StringUtil.startsWith("ab", 0, "abcdefgh"));
        assertFalse(StringUtil.startsWith("ab", 1, "abcdefgh"));
        assertFalse(StringUtil.startsWith("ab", 2, "abcdefgh"));
    }

    @Test
    public void startsWithNegativeIndex() {
        assertThatThrownBy(() -> StringUtil.startsWith("whatever", -1, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: -1, length: 8");
    }

    @Test
    public void startsWithIndexGreaterThanLength() {
        assertThatThrownBy(() -> StringUtil.startsWith("whatever", 9, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: 9, length: 8");
    }

    @Test
    public void startsWithEmptyStringNegativeIndex() {
        assertThatThrownBy(() -> StringUtil.startsWith("", -1, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: -1, length: 0");
    }

    @Test
    public void startsWithEmptyStringIndexGreaterThanLength() {
        assertThatThrownBy(() -> StringUtil.startsWith("", 1, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: 1, length: 0");
    }

    @Test
    public void startsWithLongerSuffixNegativeIndex() {
        assertThatThrownBy(() -> StringUtil.startsWith("wh", -1, "whatever"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: -1, length: 2");
    }

    @Test
    public void startsWithLongerSuffixIndexGreaterThanLength() {
        assertThatThrownBy(() -> StringUtil.startsWith("wh", 3, "whatever"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: 3, length: 2");
    }

//    @Test
//    public void offsetToLineNumberCol() {
//        assertEquals(LineColumn.of(0, 0), StringUtil.offsetToLineColumn("abc\nabc", 0));
//        assertEquals(LineColumn.of(0, 1), StringUtil.offsetToLineColumn("abc\nabc", 1));
//        assertEquals(LineColumn.of(0, 2), StringUtil.offsetToLineColumn("abc\nabc", 2));
//        assertEquals(LineColumn.of(0, 3), StringUtil.offsetToLineColumn("abc\nabc", 3));
//        assertEquals(LineColumn.of(1, 0), StringUtil.offsetToLineColumn("abc\nabc", 4));
//        assertEquals(LineColumn.of(1, 1), StringUtil.offsetToLineColumn("abc\nabc", 5));
//        assertEquals(LineColumn.of(1, 3), StringUtil.offsetToLineColumn("abc\nabc", 7));
//        assertNull(StringUtil.offsetToLineColumn("abc\nabc", 8));
//        assertEquals(LineColumn.of(0, 3), StringUtil.offsetToLineColumn("abc\r\nabc", 3));
//        assertEquals(LineColumn.of(1, 0), StringUtil.offsetToLineColumn("abc\r\nabc", 5));
//        assertEquals(LineColumn.of(2, 1), StringUtil.offsetToLineColumn("abc\n\nabc", 6));
//        assertEquals(LineColumn.of(1, 1), StringUtil.offsetToLineColumn("abc\r\nabc", 6));
//    }

//    @Test
//    public void testEnglishOrdinals() {
//        assertEquals("100th", OrdinalFormat.formatEnglish(100));
//        assertEquals("101st", OrdinalFormat.formatEnglish(101));
//        assertEquals("111th", OrdinalFormat.formatEnglish(111));
//        assertEquals("122nd", OrdinalFormat.formatEnglish(122));
//
//        assertEquals("-3rd", OrdinalFormat.formatEnglish(-3));
//        assertEquals("-9223372036854775808th", OrdinalFormat.formatEnglish(Long.MIN_VALUE));
//    }

//    @Test
//    public void testCollapseWhiteSpace() {
//        assertEquals("one two three four five", StringUtil.collapseWhiteSpace("\t one\ttwo     three\nfour five   "));
//        assertEquals("one two three four five", StringUtil.collapseWhiteSpace(" one \ttwo  \t  three\n\tfour five "));
//    }

    @Test
    public void testReplaceUnicodeEscapeSequences() {
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uuu005a")).isEqualTo("Z");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uuu005aZ")).isEqualTo("ZZ");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("Z\\uuu005aZ")).isEqualTo("ZZZ");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("Z\\\\uuu005aZ")).isEqualTo("Z\\\\uuu005aZ");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uuu005\\a\\u1\\u22\\u333")).isEqualTo("\\uuu005\\a\\u1\\u22\\u333");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\u\\u0041\\u1\\u005a")).isEqualTo("\\uA\\u1Z");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\u004")).isEqualTo("\\u004");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\")).isEqualTo("\\");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\u")).isEqualTo("\\u");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uu")).isEqualTo("\\uu");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uu1")).isEqualTo("\\uu1");
    }

    @Test
    public void testStripCharFilter() {
        assertThat(StringUtil.strip("\n   my -string ", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my-string");
        assertThat(StringUtil.strip("my- string", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my-string");
        assertThat(StringUtil.strip("my-string", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my-string");
        assertThat(StringUtil.strip("\n   my string ", CharFilter.WHITESPACE_FILTER)).isEqualTo("\n     ");
        assertThat(StringUtil.strip("", CharFilter.WHITESPACE_FILTER)).isEqualTo("");
        assertThat(StringUtil.strip("\n   my string ", ch -> false)).isEqualTo("");
        assertThat(StringUtil.strip("\n   my string ", ch -> true)).isEqualTo("\n   my string ");
    }

    @Test
    public void testTrimCharFilter() {
        assertThat(StringUtil.trim("\n   my string ", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my string");
        assertThat(StringUtil.trim("my string", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my string");
        assertThat(StringUtil.trim("my string\t", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my string");
        assertThat(StringUtil.trim("\nmy string", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my string");
        assertThat(StringUtil.trim("my-string", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my-string");
        assertThat(StringUtil.trim("my-string ", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("my-string");
        assertThat(StringUtil.trim("\n   my string ", CharFilter.WHITESPACE_FILTER)).isEqualTo("\n   my string ");
        assertThat(StringUtil.trim("", CharFilter.WHITESPACE_FILTER)).isEqualTo("");
        assertThat(StringUtil.trim("\n   my string ", ch -> false)).isEqualTo("");
        assertThat(StringUtil.trim("\n   my string ", ch -> true)).isEqualTo("\n   my string ");
        assertThat(StringUtil.trim("\u00A0   my string ", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("\u00A0   my string");
    }

    @Test
    public void testEscapeToRegexp() {
        assertThat(StringUtil.escapeToRegexp("a\nb")).isEqualTo("a\\nb");
        assertThat(StringUtil.escapeToRegexp("a&%$b")).isEqualTo("a&%\\$b");
        assertThat(StringUtil.escapeToRegexp("\uD83D\uDE80")).isEqualTo("\uD83D\uDE80");
        assertThat(StringUtil.escapeToRegexp(",'%=")).isEqualTo(",'%=");
    }

//    @Test
//    public void testRemoveEllipsisSuffix() {
//        assertEquals("a", removeEllipsisSuffix("a..."));
//        assertEquals("a", removeEllipsisSuffix("a"));
//        assertEquals("a", removeEllipsisSuffix("a" + ELLIPSIS));
//        assertEquals("a...", removeEllipsisSuffix("a..." + ELLIPSIS));
//    }

    @Test
    public void testEndsWith() {
        assertTrue(StringUtil.endsWith("text", 0, 4, "text"));
        assertFalse(StringUtil.endsWith("text", 4, 4, "-->"));
        assertThatThrownBy(() -> StringUtil.endsWith("text", -1, 4, "t"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid offsets: start=-1; end=4; text.length()=4");
        assertFalse(StringUtil.endsWith("text", "-->"));
    }

    @Test
    public void testIsJavaIdentifier() {
        assertFalse(StringUtil.isJavaIdentifier(""));
        assertTrue(StringUtil.isJavaIdentifier("x"));
        assertFalse(StringUtil.isJavaIdentifier("0"));
        assertFalse(StringUtil.isJavaIdentifier("0x"));
        assertTrue(StringUtil.isJavaIdentifier("x0"));
        assertTrue(StringUtil.isJavaIdentifier("\uD835\uDEFCA"));
        assertTrue(StringUtil.isJavaIdentifier("A\uD835\uDEFC"));
        //noinspection UnnecessaryUnicodeEscape
        assertTrue(StringUtil.isJavaIdentifier("\u03B1A"));
    }

//    @SuppressWarnings("UnnecessaryUnicodeEscape")
//    @Test
//    public void testCharSequenceSliceIsJavaIdentifier() {
//        assertFalse(StringUtil.isJavaIdentifier("", 0, 0));
//        assertTrue(StringUtil.isJavaIdentifier("x", 0, 1));
//        assertFalse(StringUtil.isJavaIdentifier("0", 0, 1));
//        assertFalse(StringUtil.isJavaIdentifier("0x", 0, 2));
//        assertTrue(StringUtil.isJavaIdentifier("foo$bar", 0, 7));
//        assertTrue(StringUtil.isJavaIdentifier("x0", 0, 2));
//        assertTrue(StringUtil.isJavaIdentifier("\uD835\uDEFCA", 0, 3));
//        assertTrue(StringUtil.isJavaIdentifier("A\uD835\uDEFC", 0, 3));
//        assertTrue(StringUtil.isJavaIdentifier("\u03B1A", 0, 2));
//        assertTrue(StringUtil.isJavaIdentifier("###\u03B1A", 3, 5));
//        assertTrue(StringUtil.isJavaIdentifier("\u03B1A###", 0, 2));
//        assertTrue(StringUtil.isJavaIdentifier("###\u03B1A###", 3, 5));
//    }

    @Test
    public void testSplit() {
        String spaceSeparator = " ";
        assertThat(StringUtil.split("test", spaceSeparator, false, false)).containsExactly("test");
        CharSequenceSubSequence seq = new CharSequenceSubSequence("test");
        assertThat(StringUtil.split(seq, spaceSeparator, false, false)).containsExactly(seq);

        assertThat(StringUtil.split("", spaceSeparator, false, false)).isEqualTo(Arrays.asList(""));
        assertThat(StringUtil.split("", spaceSeparator, true, true)).isEqualTo(Arrays.asList());

        assertThat(StringUtil.split(" ", spaceSeparator, false, false)).isEqualTo(Arrays.asList(" ", ""));
        assertThat(StringUtil.split(" ", spaceSeparator, true, false)).isEqualTo(Arrays.asList("", ""));
        assertThat(StringUtil.split(" ", spaceSeparator, true, true)).isEqualTo(Arrays.asList());

        assertThat(StringUtil.split("a  b ", spaceSeparator, true, true)).isEqualTo(Arrays.asList("a", "b"));
        assertThat(StringUtil.split("a  b ", spaceSeparator, false, true)).isEqualTo(Arrays.asList("a ", " ", "b "));
        assertThat(StringUtil.split("a  b ", spaceSeparator, false, false)).isEqualTo(Arrays.asList("a ", " ", "b ", ""));

        assertThat(StringUtil.split("a  b", spaceSeparator, true, true)).isEqualTo(Arrays.asList("a", "b"));
        assertThat(StringUtil.split("a  b", spaceSeparator, false, true)).isEqualTo(Arrays.asList("a ", " ", "b"));
        assertThat(StringUtil.split("a  b", spaceSeparator, false, false)).isEqualTo(Arrays.asList("a ", " ", "b"));

        assertThat(StringUtil.split("test", spaceSeparator, true, true)).isEqualTo(Arrays.asList("test"));
        assertThat(StringUtil.split("test", spaceSeparator, false, true)).isEqualTo(Arrays.asList("test"));
        assertThat(StringUtil.split("test", spaceSeparator, true, false)).isEqualTo(Arrays.asList("test"));
        assertThat(StringUtil.split("test", spaceSeparator, false, false)).isEqualTo(Arrays.asList("test"));

        assertThat(StringUtil.split("a  b ", spaceSeparator, true, true)).isEqualTo(Arrays.asList("a", " b"));
        assertThat(StringUtil.split("a \n\tb ", spaceSeparator, true, true)).isEqualTo(Arrays.asList("a", "\n\tb"));

        assertThat(StringUtil.split("a\u00A0b", spaceSeparator, true, true)).isEqualTo(Arrays.asList("a\u00A0b"));

        assertThat(StringUtil.split("a  \n\ta ", "a", true, true)).isEqualTo(Arrays.asList("  \n\t", " "));
    }

//    @Test
//    public void testSplitCharFilter() {
//        CharFilter spaceSeparator = CharFilter.WHITESPACE_FILTER;
//        //noinspection rawtypes
//        List split1 = StringUtil.split("test", spaceSeparator, false, false);
//        List split2 = StringUtil.split(new CharSequenceSubSequence("test"), spaceSeparator, false, false);
//        assertTrue(ContainerUtil.getOnlyItem(split1) instanceof String);
//        assertTrue(ContainerUtil.getOnlyItem(split2) instanceof CharSequenceSubSequence);
//
//        assertEquals(Arrays.asList(""), StringUtil.split("", spaceSeparator, false, false));
//        assertEquals(Arrays.asList(), StringUtil.split("", spaceSeparator, true, true));
//
//        assertEquals(Arrays.asList(" ", ""), StringUtil.split(" ", spaceSeparator, false, false));
//        assertEquals(Arrays.asList("", ""), StringUtil.split(" ", spaceSeparator, true, false));
//        assertEquals(Arrays.asList(), StringUtil.split(" ", spaceSeparator, true, true));
//
//        assertEquals(Arrays.asList("a", "b"), StringUtil.split("a  b ", spaceSeparator, true, true));
//        assertEquals(Arrays.asList("a ", " ", "b "), StringUtil.split("a  b ", spaceSeparator, false, true));
//        assertEquals(Arrays.asList("a ", " ", "b ", ""), StringUtil.split("a  b ", spaceSeparator, false, false));
//
//        assertEquals(Arrays.asList("a", "b"), StringUtil.split("a  b", spaceSeparator, true, true));
//        assertEquals(Arrays.asList("a ", " ", "b"), StringUtil.split("a  b", spaceSeparator, false, true));
//        assertEquals(Arrays.asList("a ", " ", "b"), StringUtil.split("a  b", spaceSeparator, false, false));
//
//        assertEquals(Arrays.asList("test"), StringUtil.split("test", spaceSeparator, true, true));
//        assertEquals(Arrays.asList("test"), StringUtil.split("test", spaceSeparator, false, true));
//        assertEquals(Arrays.asList("test"), StringUtil.split("test", spaceSeparator, true, false));
//        assertEquals(Arrays.asList("test"), StringUtil.split("test", spaceSeparator, false, false));
//
//        assertEquals(Arrays.asList("a", "b"), StringUtil.split("a  b ", spaceSeparator, true, true));
//        assertEquals(Arrays.asList("a", "b"), StringUtil.split("a \n\tb ", spaceSeparator, true, true));
//
//        assertEquals(Arrays.asList("a\u00A0b"), StringUtil.split("a\u00A0b", spaceSeparator, true, true));
//
//        assertEquals(Arrays.asList("  \n\t", " "), StringUtil.split("a  \n\ta ", CharFilter.NOT_WHITESPACE_FILTER, true, true));
//    }

//    @Test
//    @SuppressWarnings({"OctalInteger", "UnnecessaryUnicodeEscape"}) // need to test octal numbers and escapes
//    public void testUnescapeAnsiStringCharacters() {
//        assertEquals("'", StringUtil.unescapeAnsiStringCharacters("\\'"));
//        assertEquals("\"", StringUtil.unescapeAnsiStringCharacters("\\\""));
//        assertEquals("?", StringUtil.unescapeAnsiStringCharacters("\\?"));
//        assertEquals("\\", StringUtil.unescapeAnsiStringCharacters("\\\\"));
//        assertEquals("" + (char)0x07, StringUtil.unescapeAnsiStringCharacters("\\a"));
//        assertEquals("" + (char)0x08, StringUtil.unescapeAnsiStringCharacters("\\b"));
//        assertEquals("" + (char)0x0c, StringUtil.unescapeAnsiStringCharacters("\\f"));
//        assertEquals("\n", StringUtil.unescapeAnsiStringCharacters("\\n"));
//        assertEquals("\r", StringUtil.unescapeAnsiStringCharacters("\\r"));
//        assertEquals("\t", StringUtil.unescapeAnsiStringCharacters("\\t"));
//        assertEquals("" + (char)0x0b, StringUtil.unescapeAnsiStringCharacters("\\v"));
//
//        // octal
//        assertEquals("" + (char)00, StringUtil.unescapeAnsiStringCharacters("\\0"));
//        assertEquals("" + (char)01, StringUtil.unescapeAnsiStringCharacters("\\1"));
//        assertEquals("" + (char)012, StringUtil.unescapeAnsiStringCharacters("\\12"));
//        assertEquals("" + (char)0123, StringUtil.unescapeAnsiStringCharacters("\\123"));
//
//        // hex
//        assertEquals("" + (char)0x0, StringUtil.unescapeAnsiStringCharacters("\\x0"));
//        assertEquals("" + (char)0xf, StringUtil.unescapeAnsiStringCharacters("\\xf"));
//        assertEquals("" + (char)0xff, StringUtil.unescapeAnsiStringCharacters("\\xff"));
//        assertEquals("" + (char)0xfff, StringUtil.unescapeAnsiStringCharacters("\\xfff"));
//        assertEquals("" + (char)0xffff, StringUtil.unescapeAnsiStringCharacters("\\xffff"));
//        assertEquals("" + (char)0xf, StringUtil.unescapeAnsiStringCharacters("\\x0000000000000000f"));
//        assertEquals("\\x110000", StringUtil.unescapeAnsiStringCharacters("\\x110000")); // invalid unicode codepoint
//
//        // 4 digit codepoint
//        assertEquals("\u1234", StringUtil.unescapeAnsiStringCharacters("\\u1234"));
//
//        // 8 digit codepoint
//        assertEquals("\u0061", StringUtil.unescapeAnsiStringCharacters("\\U00000061"));
//        assertEquals("\\U00110000", StringUtil.unescapeAnsiStringCharacters("\\U00110000")); // invalid unicode codepoint
//    }
}
